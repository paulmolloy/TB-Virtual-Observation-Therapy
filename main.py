import logging
import os

from flask import Flask
from flask import jsonify
from flask import request
from flask import send_from_directory

from users import users_blueprint

app = Flask(__name__)
app.register_blueprint(users_blueprint)


@app.route('/')
def hello():
    """Return a friendly HTTP greeting."""
    return 'Hello World!'


@app.route('/files/<path:path>')
def load_file(path):
    """Sends back whatever file is specified in `path`. For example:

    If you go to: http://34.73.42.71:8080/files/login.html
    it will return the file located at: server/files/login.html

    If you go to: http://34.73.42.71:8080/files/videos/style.css
    it will return the file located at: server/files/videos/style.css

    """
    return send_file(path)


@app.route('/login')
def login():
    return send_file("login.html")


@app.route('/videos/<userID>')
def videos(userID):
    return send_file("videos/videos.html")

@app.route('/nurseLogin')
def nurseLogin():
    return send_file("nurseLogin.html")

@app.route('/nurseSignUp')
def nurseSignUp():
    return send_file("nurseSignUp.html")

@app.route('/patientSignUp')
def patientSignUp():
    return send_file("patientSignUp.html")

@app.route('/graph')
def graph():
    return send_file("graph/graph.html")

@app.route('/Roboto-Regular.ttf')
def RobotoRegular():
    return send_file("Roboto-Regular.ttf")



def send_file(path):
    return send_from_directory('server/files', path)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
    #app.run(host='127.0.0.1', port=4000, debug=True)
