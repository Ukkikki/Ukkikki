import face_recognition
import numpy as np
from datetime import datetime
import cv2
import os
import io
from dotenv import load_dotenv
import boto3
import base64
import hashlib
import dlib
import uuid

# GPU 사용 여부 확인
print(dlib.DLIB_USE_CUDA)
print(dlib.cuda.get_num_devices())

# env 파일 로드
load_dotenv()

ACCESS_KEY_ID = os.environ.get('S3_ACCESS_KEY')  # s3 관련 권한을 가진 IAM계정 정보
ACCESS_SECRET_KEY = os.environ.get('S3_SECRET_KEY')
BUCKET_NAME = os.environ.get('S3_BUCKET_NAME')
S3_REGION = os.environ.get('S3_REGION')

ratio = 1.0
# 동일 인물 대한 판단 기준치
similarity_threshold = 0.4

def generate_aes_key(input_data):
    sha256_hash = hashlib.sha256()
    sha256_hash.update(input_data.encode('utf-8'))
    return sha256_hash.digest()[:32]

def generate_key_md5(b64_key):
    key_bytes = base64.b64decode(b64_key)
    md5_hash = hashlib.md5(key_bytes).digest()
    return base64.b64encode(md5_hash).decode('utf-8')

def face_classifier(file, partyId, key, cursor, db, photoId) :
    # todo : file로 들어온 사진 이미지 cv2 형식으로 변환
    in_memory_file = io.BytesIO()
    file.save(in_memory_file)
    data = np.frombuffer(in_memory_file.getvalue(), dtype=np.uint8)
    color_image_flag = 1  # cv2.IMREAD_COLOR로도 설정 가능
    image = cv2.imdecode(data, color_image_flag)

    # todo : 이미지에서 얼굴 추출
    faces = detect_faces(image)

    # todo : face 이미지 s3 저장
    s3 = boto3.client('s3', aws_access_key_id=ACCESS_KEY_ID, aws_secret_access_key=ACCESS_SECRET_KEY)
    # sse_key = generate_aes_key(key)
    # encode = base64.b64encode(sse_key).decode('utf-8')
    md5 = generate_key_md5(key)
    print(key)
    sse_conf = {"SSECustomerAlgorithm": "AES256", "SSECustomerKey": key, "SSECustomerKeyMD5": md5, "ACL": "public-read", "ContentType": 'image/png'}

    for face in faces:
        is_success, buffer = cv2.imencode('.png', face.image)
        io_buf = io.BytesIO(buffer)
        s3.upload_fileobj(io_buf, BUCKET_NAME, face.filename, sse_conf)
        face.filename = f"https://{BUCKET_NAME}.s3.ap-northeast-2.amazonaws.com/{face.filename}"
    # todo : 생성된 그룹과 인코딩 값 비교를 위해 그룹 데이터 로드
    sql = 'select * from face_group where party_id = %s'
    cursor.execute(sql, partyId)
    face_group_list = list(cursor.fetchall())

    # face_id_list 를 통해서 실제 face 객체를 로드
    for face_group in face_group_list :
        print(face_group['face_list'])
        if face_group['face_list'] == '[]':
            continue
        face_id_list = [int(x) for x in face_group['face_list'][1:-1].split(',')]
        face_list = []
        for face_id in face_id_list:
            sql = 'select * from face where id = %s'
            cursor.execute(sql, face_id)
            face_list.append(cursor.fetchall())

        face_group['face_list'] = face_list

    # 감지된 얼굴별 분류 로직 실행
    for face in faces:
        # 분류된 그룹이 없다면 그룹 0으로 하나 생성
        face_group_list_len = len(face_group_list)
        # 아직 그룹이 하나도 없다면 unknown 그룹을 만들고 해당 그룹에 추가
        if face_group_list_len == 0:
            sql = 'insert into face(party_id, face_image_url, origin_image_url, encoding, face_group_number, photo_id) values(%s, %s, %s, %s, %s, %s)'
            cursor.execute(sql, (partyId, face.filename, file.filename, face.encoding, 0, photoId))
            sql = 'select * from face where face_image_url = %s'
            cursor.execute(sql, face.filename)
            facedb = cursor.fetchall()
            face_list = [facedb]
            face_group_list.append({'face_group_number' : 0, 'encoding' : face.encoding, 'face_list' : face_list, 'image_count' : 1})
            face_id_list = [facedb[0]['id']]
            sql = 'insert into face_group(party_id, face_group_number, encoding, face_list, image_count) values (%s, %s, %s, %s, %s)'
            cursor.execute(sql, (partyId, 0, face.encoding, str(face_id_list), 1))
            db.commit()
        # 이미 그룹이 존재한다면
        else:
            # 그룹 리스트를 아는 사람과 모르는 사람으로 구분
            unknown_face_list = []
            known_face_group_list = []

            # 모르는 사람 그룹의 리스트 초기화
            for face_group in face_group_list:
                if face_group['face_group_number'] == 0:
                    if face_group['image_count'] == 0:
                        continue
                    else:
                        if np.ndim(face_group['face_list']) == 2:
                            for unknown_face in face_group['face_list']:
                                unknown_face_list.append(unknown_face[0])
                        else:
                            unknown_face_list = face_group['face_list']
                else:
                    known_face_group_list.append(face_group)

            print("언너운페이스리스트")
            print(unknown_face_list)
            print("노운페이스그룹리스트")
            print(known_face_group_list)

            flag = compare_with_known_face_group(known_face_group_list, face, partyId, file, cursor, db, photoId)
            if flag:
                group_zero = {"party_id": partyId, 'face_group_number': 0, 'face_list': unknown_face_list, 'image_count' : len(unknown_face_list)}
                face_group_list = []
                face_group_list.append(group_zero)
                for face_group in known_face_group_list:
                    face_group_list.append(face_group)
                continue

            compare_with_unknown_face(unknown_face_list,face,partyId,file,face_group_list_len,known_face_group_list, cursor, db, photoId)
            group_zero = {"party_id": partyId, 'face_group_number': 0, 'face_list': unknown_face_list, 'image_count' : len(unknown_face_list)}
            face_group_list = []
            face_group_list.append(group_zero)
            for face_group in known_face_group_list:
                face_group_list.append(face_group)
    return True

def compare_with_known_face_group(known_face_group_list, face, partyId, file, cursor, db, photoId):
    if len(known_face_group_list) == 0:
        return False

    # 그룹별 얼굴 특징 평균값을 불러 온다.
    encodings = []
    for face_group in known_face_group_list:
        data = str(face_group['encoding'])
        data_string_no_newlines = data[1:-1].replace('\n', '')
        encodings.append(np.fromstring(data_string_no_newlines, sep=' '))

    # 현재 비교 대상인 얼굴의 특징값과 비교
    distances = face_recognition.face_distance(encodings, face.encoding)
    # 가장 가까운 그룹의 인덱스 생성
    index = np.argmin(distances)
    # 가장 가까운 그룹의 결과값 생성
    min_value = distances[index]
    # 결과값이 기준치를 통과할 경우
    if min_value < similarity_threshold:
        # face of known person
        sql = 'insert into face(party_id, face_image_url, origin_image_url, encoding, face_group_number, photo_id) values(%s, %s, %s, %s, %s, %s)'
        cursor.execute(sql, (partyId, face.filename, file.filename, face.encoding, known_face_group_list[index]['face_group_number'], photoId))
        sql = 'select * from face where face_image_url = %s'
        cursor.execute(sql, face.filename)
        # 해당 그룹에 추가
        face_data = cursor.fetchall()
        known_face_group_list[index]['face_list'].append(face_data)
        # 해당 그룹의 특징값 평균 재계산
        re_encodings = []
        for face_list in known_face_group_list[index]['face_list']:
            for known_face in face_list:
                re_encodings.append(np.fromstring(known_face['encoding'][1:-1], sep=' '))
        known_face_group_list[index]['encoding'] = np.average(re_encodings, axis=0)
        sql = 'update face_group set encoding = %s, face_list = %s, image_count = %s where face_group_number = %s and party_id = %s'
        face_id_list = []
        for face_list in known_face_group_list[index]['face_list']:
            for known_face in face_list:
                face_id_list.append(known_face['id'])
        cursor.execute(sql, (known_face_group_list[index]['encoding'], str(face_id_list), len(face_id_list), known_face_group_list[index]['face_group_number'], partyId))
        db.commit()
        return True

def compare_with_unknown_face(unknown_face_list, face, partyId, file, face_group_list_len, known_face_group_list, cursor, db, photoId):
    if len(unknown_face_list) == 0:
        sql = 'insert into face(party_id, face_image_url, origin_image_url, encoding, face_group_number, photo_id) values(%s, %s, %s, %s, %s, %s)'
        cursor.execute(sql, (partyId, face.filename, file.filename, face.encoding, 0, photoId))
        sql = 'select * from face where face_image_url = %s'
        cursor.execute(sql, face.filename)
        # 모르는 사람 그룹에 추가
        facedb = cursor.fetchall()
        unknown_face_list.append(facedb[0])
        face_id_list = []
        for unknown_face in unknown_face_list:
            face_id_list.append(unknown_face['id'])
        # 그룹 DB 업데이트
        sql = 'update face_group set face_list = %s, image_count = %s where face_group_number = %s and party_id = %s'
        cursor.execute(sql,(str(face_id_list), len(face_id_list), 0, partyId))
        db.commit()
        return 0

    # 모르는 사람 얼굴 특징값 불러 온다.
    encodings = [np.fromstring(unknown_face['encoding'][1:-1], sep=' ') for unknown_face in unknown_face_list]
    # 특징값 비교
    distances = face_recognition.face_distance(encodings, face.encoding)
    # 인덱스 생성
    index = np.argmin(distances)
    # 비교 결과값 중 가장 가까운 정도
    min_value = distances[index]
    # 가장 가까운 결과가 기준치를 통과한 경우
    if min_value < similarity_threshold:
        # two faces are similar - create new person with two faces
        sql = 'insert into face(party_id, face_image_url, origin_image_url, encoding, face_group_number, photo_id) values(%s, %s, %s, %s, %s, %s)'
        cursor.execute(sql, (partyId, face.filename, file.filename, face.encoding, face_group_list_len, photoId))
        sql = 'select * from face where face_image_url = %s'
        cursor.execute(sql, face.filename)
        facedb = cursor.fetchall()
        # unknown face와 현재 face의 특징값 평균 계산
        encoding = np.average([encodings[index], face.encoding], axis=0)
        face_id_list = [unknown_face_list[index]['id'], facedb[0]['id']]
        # 아는 사람 그룹에 추가
        known_face_group_list.append(
            {'face_group_number': face_group_list_len, 'encoding': str(encoding), 'face_list': face_id_list})
        sql = 'insert into face_group(party_id, face_group_number, encoding, face_list, image_count) values (%s, %s, %s, %s, %s)'
        cursor.execute(sql, (partyId, face_group_list_len, encoding, str(face_id_list), len(face_id_list)))
        sql = 'update face set face_group_number = %s where id = %s'
        cursor.execute(sql, (face_group_list_len, unknown_face_list[index]['id']))
        # face_group 0번에서 face_list 수정
        unknown_face_list.pop(index)
        face_id_list = []
        for unknown_face in unknown_face_list:
            face_id_list.append(unknown_face['id'])
        # 모르는 사람 그룹 DB 업데이트
        sql = 'update face_group set face_list = %s, image_count = %s where face_group_number = 0 and party_id = %s'
        cursor.execute(sql, (str(face_id_list), len(face_id_list), partyId))
        db.commit()
        return 0
    else:
        sql = 'insert into face(party_id, face_image_url, origin_image_url, encoding, face_group_number, photo_id) values(%s, %s, %s, %s, %s, %s)'
        cursor.execute(sql, (partyId, face.filename, file.filename, face.encoding, 0, photoId))
        sql = 'select * from face where face_image_url = %s'
        cursor.execute(sql, face.filename)
        facedb = cursor.fetchall()
        unknown_face_list.append(facedb[0])
        face_id_list = []
        # 모르는 사람 그룹에 추가
        for unknown_face in unknown_face_list:
            face_id_list.append(unknown_face['id'])
        # 모르는 사람 그룹 DB 업데이트
        sql = 'update face_group set face_list = %s, image_count = %s where face_group_number = 0 and party_id = %s'
        cursor.execute(sql, (str(face_id_list), len(face_id_list), partyId))
        db.commit()
        return 0

def get_face_image(frame, box):
    img_height, img_width = frame.shape[:2]
    (box_top, box_right, box_bottom, box_left) = box
    box_width = box_right - box_left
    box_height = box_bottom - box_top
    crop_top = max(box_top - box_height, 0)
    pad_top = -min(box_top - box_height, 0)
    crop_bottom = min(box_bottom + box_height, img_height - 1)
    pad_bottom = max(box_bottom + box_height - img_height, 0)
    crop_left = max(box_left - box_width, 0)
    pad_left = -min(box_left - box_width, 0)
    crop_right = min(box_right + box_width, img_width - 1)
    pad_right = max(box_right + box_width - img_width, 0)
    face_image = frame[crop_top:crop_bottom, crop_left:crop_right]
    if (pad_top == 0 and pad_bottom == 0):
        if (pad_left == 0 and pad_right == 0):
            return face_image
    padded = cv2.copyMakeBorder(face_image, pad_top, pad_bottom,
                                pad_left, pad_right, cv2.BORDER_CONSTANT)
    return padded


def locate_faces(frame):
    if ratio == 1.0:
        rgb = frame[:, :, ::-1]
    else:
        small_frame = cv2.resize(frame, (0, 0), fx=ratio, fy=ratio)
        rgb = small_frame[:, :, ::-1]

    boxes = face_recognition.face_locations(rgb,1, 'cnn')

    if ratio == 1.0:
        return boxes
    boxes_org_size = []
    for box in boxes:
        (top, right, bottom, left) = box
        left = int(left / ratio)
        right = int(right / ratio)
        top = int(top / ratio)
        bottom = int(bottom / ratio)
        box_org_size = (top, right, bottom, left)
        boxes_org_size.append(box_org_size)
    return boxes_org_size

def detect_faces(frame):
    boxes = locate_faces(frame)
    if len(boxes) == 0:
        return []

    # faces found
    faces = []
    now = datetime.now()
    str_ms = now.strftime('%Y%m%d_%H%M%S.%f')[:-3] + '-' + str(uuid.uuid4())
    encodings = face_recognition.face_encodings(frame, boxes)
    for i, box in enumerate(boxes):
        face_image = get_face_image(frame, box)
        face = Face(str_ms + str(i) + ".jpg", face_image, encodings[i])
        face.location = box
        faces.append(face)
    return faces

class Face():
    def __init__(self, filename, image, face_encoding):
        self.filename = filename
        self.image = image
        self.encoding = face_encoding