from flask import Flask, render_template, request, json
import face_classifier
import pymysql
import os
from dotenv import load_dotenv

load_dotenv()

DB_HOST = os.environ.get('DB_HOST')
DB_PORT = int(os.environ.get('DB_PORT'))
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME')



app = Flask(__name__)
fc = face_classifier

@app.route('/uploader', methods=['POST'])
def uploader_file():
    if request.method == 'POST':
        db = pymysql.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD, db=DB_NAME, charset='utf8')
        cursor = db.cursor(pymysql.cursors.DictCursor)
        # 비디오 아웃풋명 및 디렉토리 네이밍을 위한 파라미터
        partyId = request.form['partyId']
        key = request.form['key']
        photoId = request.form['photoId']
        index = request.form['index']
        file = request.files.get("file")

        fc.face_classifier(file, partyId, key, cursor, db, photoId)
        response = app.response_class(
            response=json.dumps({
                'result': 'finish',
                'index': index
            }),
            mimetype='application/json'
        )
    return response


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)