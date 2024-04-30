from flask import Flask, render_template, request
import face_classifier
import numpy as np
import cv2
from PIL import Image
import io
import pymysql

db = pymysql.connect(host='127.0.0.1', user='root', password='ssafy', db='ukkikki', charset='utf8')

app = Flask(__name__)
fc = face_classifier

@app.route('/upload')
def hello_world():
    return render_template('upload.html')

@app.route('/uploader', methods=['POST'])
def uploader_file():
    if request.method == 'POST':
        # 비디오 아웃풋명 및 디렉토리 네이밍을 위한 파라미터
        pk = request.form['PK']
        file = request.files.get("file")
        fc.face_classifier(file, 1, 'mykey')
    return 'upload'


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)