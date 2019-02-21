import logging
import os

from flask import Flask
from flask import jsonify
from flask import request

from users import users_blueprint

app = Flask(__name__)
app.register_blueprint(users_blueprint)


@app.route('/')
def hello():
    """Return a friendly HTTP greeting."""
    return 'Hello World!'


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)

