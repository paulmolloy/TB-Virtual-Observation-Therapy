import traceback

from flask import Blueprint
from flask import jsonify
from flask import request

import firebase_admin
from firebase_admin import firestore
from firebase_admin import auth
from firebase_admin import credentials

# Init Firebase
firebase_admin.initialize_app(credentials.Certificate("key.json"))
fire = firestore.client()

users_blueprint = Blueprint('users', __name__)


@users_blueprint.route('/api/insecure/add-patient')
def insecurely_add_patient():
    """Adds a patient using params in the query string"""

    name = request.args["name"]
    email = request.args["email"]
    password = request.args["password"]
    nurseID = request.args["nurseID"]

    return create_patient(name, email, password, nurseID)


@users_blueprint.route('/api/insecure/add-nurse')
def insecurely_add_nurse():
    """Adds a nurse using params in the query string"""

    name = request.args["name"]
    email = request.args["email"]
    password = request.args["password"]

    return create_nurse(name, email, password)


@users_blueprint.route('/api/patients', methods=["POST"])
def add_patient():
    """Expects a JSON that looks like this to be POSTed:

    {
        name: string
        email: string
        password: string
        nurseID: string
        authToken: string
    }

    """

    json = request.get_json()

    if not verify_auth_token(json['authToken']):
        return jsonify({ 'status': "failed", "error": "There's something wrong with the auth token" })

    return create_patient(json['name'], json['email'], json['password'], json['nurseID'])


@users_blueprint.route('/api/nurses', methods=["POST"])
def add_nurse():
    """Expects a JSON that looks like this to be POSTed:

    {
        name: string
        email: string
        password: string
        authToken: string
    }

    """

    json = request.get_json()

    if not verify_auth_token(json['authToken']):
        return jsonify({ 'status': "failed", "error": "There's something wrong with the auth token" })

    return create_nurse(json['name'], json['email'], json['password'])







def create_account(email, password):
    """Returns two items, either: (True, newUID) or (False, error)"""
    try:
        user = auth.create_user(email=email, password=password)
        return True, user.uid
    except Exception as e:
        return False, str(e).split("\n")


def verify_auth_token(token):
    """Returns True if the token is verified and False if verification failed"""
    try:
        auth.verify_id_token(token)
        return True
    except Exception as e:
        return False


def create_nurse(name, email, password):

    success, uid = create_account(email, password)

    if not success:
        return jsonify({ 'status': "failed", 'error': uid })

    doc = { 'name': name, 'email': email }
    fire.document("nurses/" + uid).set(doc)

    doc['uid'] = uid
    doc['password'] = password
    return jsonify({ 'status': "ok", 'data': doc })


def create_patient(name, email, password, nurseID):

    success, uid = create_account(email, password)

    if not success:
        return jsonify({ 'status': "failed", 'error': uid })

    doc = { 'name': name, 'email': email, 'nurseID': nurseID }
    fire.document("patients/" + uid).set(doc)

    doc['uid'] = uid
    doc['password'] = password
    return jsonify({ 'status': "ok", 'data': doc })
