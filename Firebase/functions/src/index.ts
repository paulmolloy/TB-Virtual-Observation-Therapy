import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';


// ---------------- Firebase Functions ----------------

export const addNurse = functions.https.onRequest(async (request, response) => {
    
    try {

        let { name, email, password } = request.query

        let user = await firebase().auth().createUser({ email, password })

        await fs().doc("nurses/" + user.uid).set({ name, email })

        respond(
            response,
            "Success",
            `<h3>The nurse was added successfully!</h3>
            <p>Name: ${name}</p>
            <p>Email: ${email}</p>
            <p>Password: ${password}</p>
            <p>ID: ${user.uid}</p>`
        )
        return "ok"

    } catch (e) {
        respond(
            response,
            "Something went wrong",
            `<h3>${e}</h3>
            <p>${JSON.stringify(e)}</p>`
        )
        return "fail"
    }

});

export const addPatient = functions.https.onRequest(async (request, response) => {
    
    try {

        let { name, email, nurseID, password } = request.query

        let user = await firebase().auth().createUser({ email, password })

        await fs().doc("patients/" + user.uid).set({ name, email, nurseID })

        respond(
            response,
            "Success",
            `<h3>The patient was added successfully!</h3>
            <p>Name: ${name}</p>
            <p>Email: ${email}</p>
            <p>Password: ${password}</p>
            <p>NurseID: ${nurseID}</p>
            <p>PatientID: ${user.uid}</p>`
        )
        return "ok"

    } catch (e) {
        respond(
            response,
            "Something went wrong",
            `<h3>${e}</h3>
            <p>${JSON.stringify(e)}</p>`
        )
        return "fail"
    }

});





// --------------------- Helpers ---------------------

let _fb: admin.app.App

/** Lazy inits Firebase */
function firebase(){

    if(!_fb) _fb = admin.initializeApp(functions.config().firebase)

    return _fb

}

/** Shortcut to access Firestore */
function fs(){
    return firebase().firestore()
}

/** Responds in HTML */
function respond(res: functions.Response, title: string, body: string){
    res.send(`
        <html>
            <head>
                <title>${title}</title>
            </head>
            <body>
                ${body}
            </body>
        </html>
    `)
}