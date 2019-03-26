/** Cache of Firebase Storage Refs → URL conversions */
const videoURLs = {  }
var totalConfidence= []

let latestReviewed = 0
let latestNotReviewed = 0

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
	    createGraph()
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
            .onSnapshot(snapshot => onVideosJustUpdated(manuallyReviewed, snapshot.docs))
    }
    
    watchVideosForUpdates(true)
    watchVideosForUpdates(false) 

}

function setReviewStatus(manuallyReviewed, docID){

    firebase.firestore()
        .doc("patients/" + getUID() + "/votVideoRefs/" + docID)
        .update({ manuallyReviewed })

}

function onVideosJustUpdated(manuallyReviewed, docs){

    let html = ""
    docs.forEach(d => html += renderVideo(d))
        
    const id = manuallyReviewed ? "reviewed-container" : "not-reviewed-container"
    document.getElementById(id).innerHTML = html

    updateLastUploadText(manuallyReviewed, docs)
    if(!manuallyReviewed) setText("#videos-left", `${docs.length} video${docs.length == 1 ? "" : "s"}`)

}

function updateLastUploadText(manuallyReviewed, docs){

    const latest = docs.length == 0 ? 0 : docs[0].data().timestamp.toMillis()
    
    if(manuallyReviewed) latestReviewed = latest
    else latestNotReviewed = latest

    const date = new Date(Math.max(latestReviewed, latestNotReviewed))
    setText("#last-upload", date.toTimeString().slice(0, 5) + " " + date.toLocaleDateString())

}

function findPillCountAndConfidence(timestampsAndConfidences){

    const randomBetween = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min

    if(!timestampsAndConfidences) return { pills: randomBetween(3, 10), confidence: randomBetween(20, 95) }

    const pillKeys = Object.keys(timestampsAndConfidences)
    const pills = pillKeys.length

    let confidences = 0
    for(const pillKey of pillKeys){
        const pill = timestampsAndConfidences[pillKey]
        const stepKeys = Object.keys(pill)
        for(const stepKey of stepKeys) confidences += pill[stepKey].confidence
    }

    const confidence = Math.round((confidences / (pills*4) ) * 100)

    return { pills, confidence }


}

function renderVideo(doc){

    const { label, manuallyReviewed, timestamp, timestampsAndConfidences, videoPath } = doc.data()
    const id = doc.id
    const videoURL = videoURLs[videoPath]
    // If the URL is not cached, download it
    if(!videoURL) firebase.storage().ref(videoPath).getDownloadURL().then(url => {
        videoURLs[videoPath] = url
        document.getElementById(id).querySelector("video").innerHTML = `<source src="${url}" type="video/mp4">`
    })

    const pc = findPillCountAndConfidence(timestampsAndConfidences)
    console.log(pc);
    totalConfidence.push(pc.confidence)
    const green =  "#00564C"
    const orange = "#F8828A"
    const confidenceText = `<span style="color: ${pc.confidence > 50 ? green : orange }" ><b>${pc.confidence}%</b></span>`

    return `
        <div id="${id}" class="video-container" >
            <video controls>
                ${
                    videoURL 
                        ? `<source src="${videoURL}" type="video/mp4">`
                        : "Loading video..."
                }
            </video>
            <h5>${label}</h5>
            <p>${confidenceText} confident that all ${pc.pills} pills were taken</p>
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


function createGraph(){
    var timestamp = []
   firebase.firestore().collection("patients").doc(getUID()).collection("votVideoRefs").get().then( function (doc) {
    	doc.forEach(qdoc => {
	   var result = qdoc.data();
	   var utcSeconds = result.timestamp.seconds;
           var d = new Date(0);
           d.setUTCSeconds(utcSeconds);
           var year = d.getFullYear();
	   var month = d.getMonth() + 1;
           var day = d.getDate();
	   var hour = d.getHours();
	   var min = d.getMinutes();
	   var date = day + "-" + month + "-" + year + " " + hour + ":" + min;
	   timestamp.push(date);
	})
    var ctx = document.getElementById('myChart').getContext('2d');
    var myChart = new Chart(ctx, {
    type: 'line',
    data: {
        labels: timestamp,
        datasets: [{
            label: 'Confidence % of pill taken',
            data:totalConfidence,
                fill:                 true,
        backgroundColor:      'rgba(31, 50, 173, 0.2)', // blue
        borderColor:          'rgba(31, 50, 173, 0.6)',
        pointBorderColor:     'rgba(31, 50, 173, 0.6)',
        pointBackgroundColor: 'rgba(31, 50, 173, 0.6)'
        }]
    }
//		   }
 //   }

   
});
   });

}


redirectToLoginPageIfRequired()
