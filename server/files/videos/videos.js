/** Returns UID thats in the URL */
function getUID() {
    const split = window.location.href.split("/")
    return split[split.length - 1]
}

function setText(query, text) {
    document.querySelector(query).textContent = text
}

function setHTML(query, text) {
    document.querySelector(query).innerHTML = text
}

/** Redirects to the login page if the user is not signed in */
function redirectToLoginPageIfRequired() {
    firebase.auth().onAuthStateChanged(user => {
        if(!user) window.location.replace("/login?redirect=/videos/" + getUID())
        else {
            watchPatientsDataForUpdates()
            watchPatientsVideosForUpdates()
        }
    })
}

function watchPatientsDataForUpdates() {

    firebase.firestore().doc("patients/" + getUID()).onSnapshot(doc => {
        const { name, email } = doc.data()
        setText("#username", name)
        setHTML("#useremail", '<i class="fas fa-envelope"></i> ' + email)
    });

}

function watchPatientsVideosForUpdates(){

    const watchVideosForUpdates = manuallyReviewed => {
        firebase.firestore()
            .collection("patients/" + getUID() + "/votVideoRefs")
            .where("manuallyReviewed", "==", manuallyReviewed)
            .limit(20)
            .orderBy("timestamp", "desc")
            .onSnapshot(snapshot => videosJustUpdated(manuallyReviewed, snapshot.docs))
    }
    
    watchVideosForUpdates(true)
    watchVideosForUpdates(false) 

}

function setReviewStatus(manuallyReviewed, docID){

    firebase.firestore()
        .doc("patients/" + getUID() + "/votVideoRefs/" + docID)
        .update({ manuallyReviewed })

}

function videosJustUpdated(manuallyReviewed, docs){

    let html = ""
    docs.forEach(d => html += renderVideo(d))
        
    const id = manuallyReviewed ? "reviewed-container" : "not-reviewed-container"
    document.getElementById(id).innerHTML = html

}

videoURLs = {  }

function renderVideo(doc){

    const { label, manuallyReviewed, timestamp, timestampsAndConfidences, videoPath } = doc.data()
    const id = doc.id

    const videoURL = videoURLs[videoPath]
    // If the URL is not cached, download it
    if(!videoURL) firebase.storage().ref(videoPath).getDownloadURL().then(url => {
        videoURLs[videoPath] = url
        document.getElementById(id).querySelector("video").innerHTML = `<source src="${url}" type="video/mp4">`
    })

    return `
        <div id="${id}" class="video-container" >
            <video controls>
                ${
                    videoURL 
                        ? `<source src="${videoURL}" type="video/mp4">`
                        : "Loading video..."
                }
            </video>
            <p>${label}</p>
            <div 
                onclick="setReviewStatus(${!manuallyReviewed}, '${id}')" 
                class="${manuallyReviewed ? "orange" : "green"}"
            >
                ${manuallyReviewed 
                    ? '<i class="far fa-window-close"></i> Unmark as reviewed' 
                    : '<i class="far fa-check-circle"></i> Mark as reviewed'}
            </div>
        </div>
    `
    
}

redirectToLoginPageIfRequired()
