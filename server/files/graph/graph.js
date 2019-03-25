/** Shared variables **/
var dateList = []
var usernamesList = []
var timestampConfidenceList = []
var idList = []
var patientsConfidence = {}
var result = []
var videoList = []

/** Returns the confidence for the number of pills taken */
function findConfidence(timestampsAndConfidences) {
    const randomBetween = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min

    if (!timestampsAndConfidences) {
        return confidence
    }

    const pillKeys = Object.keys(timestampsAndConfidences)
    const pills = pillKeys.length

    let confidences = 0
    for (const pillKey of pillKeys) {
        const pill = timestampsAndConfidences[pillKey]
        const stepKeys = Object.keys(pill)
        for (const stepKey of stepKeys) confidences += pill[stepKey].confidence
    }
    const confidence = Math.round((confidences / (pills * 4)) * 100)

    return confidence
}


/** Generates the grid/table in HTML for patient data */
function createColouredGrid() {
    var body = document.body, tbl = document.createElement("table"), tbdy = document.createElement('tbody'), thead = document.createElement("thead")
    tbl.setAttribute('class', 'table table-bordered table-striped')
    tbl.style.width = "auto"
    tbl.style.margin = "0 auto"
    tbl.style.marginTop = "100px"
    thead.setAttribute('class', 'thead-dark')
    var tr = document.createElement('tr'), th = document.createElement('th')
    th.style.textAlign = "center"
    thead.appendChild(tr)
    tr.appendChild(th)
    th.appendChild(document.createTextNode('Most recent 30 days of data'))
    th.setAttribute('colspan', 31)
    tbdy.appendChild(tr)
    var tr = document.createElement('tr'), th = document.createElement('th')
    thead.appendChild(tr)
    tr.appendChild(th)
    th.appendChild(document.createTextNode('Patient Name'))
    for (var i = 1; i < 31; i++) {
        var th = document.createElement('th')
        th.appendChild(document.createTextNode(i))
        th.setAttribute('scope', 'col')
        tr.appendChild(th)
    }
    tbdy.appendChild(tr)

    for (var i = 0; i < usernamesList.length; i++) {
        var a = document.createElement('a'), td = document.createElement('td'), tr = document.createElement('tr')
        a.href = "http://34.73.42.71:8080/videos/" + idList[i]
        a.innerHTML = usernamesList[i]
        td.appendChild(a)
        tr.appendChild(td)
        for (var j = 0; j < 30; j++) {
            if (patientsConfidence[usernamesList[i]]['confidence'][j] != undefined) {
                if (patientsConfidence[usernamesList[i]]['confidence'][j] >= 55 && patientsConfidence[usernamesList[i]]['confidence'][j] <= 100) {
                    var patientColour = document.createElement('td')
                    patientColour.id = i
                    patientColour.setAttribute('bgcolor', '#77c548')
                    patientColour.innerHTML = patientsConfidence[usernamesList[i]]['timestamp'][j]
                    tr.appendChild(patientColour)

                }
                else if (patientsConfidence[usernamesList[i]]['confidence'][j] >= 50 && patientsConfidence[usernamesList[i]]['confidence'][j] < 55) {
                    var patientColour = document.createElement('td')
                    patientColour.id = i
                    patientColour.setAttribute('bgcolor', '#ffa500')
                    patientColour.innerHTML = patientsConfidence[usernamesList[i]]['timestamp'][j]
                    tr.appendChild(patientColour)
                }
                else {
                    var patientColour = document.createElement('td')
                    patientColour.id = i
                    patientColour.setAttribute('bgcolor', '#ff0000')
                    patientColour.innerHTML = patientsConfidence[usernamesList[i]]['timestamp'][j]
                    tr.appendChild(patientColour)
                }
            }
            else {
                var emptySpace = document.createElement('td')
                tr.appendChild(emptySpace)
            }
        }
        tbdy.appendChild(tr)
    }
    tbdy.appendChild(tr)
    thead.appendChild(tbdy)
    tbl.appendChild(thead)
    body.appendChild(tbl)

}

/**  Creates a legend for the graph **/
function createLegend() {
    var body = document.body, tbl = document.createElement("table"), tbdy = document.createElement('tbody'), thead = document.createElement("thead")
    tbl.setAttribute('class', 'table table-bordered')
    tbl.style.width = "auto"
    tbl.style.margin = "0 auto"
    tbl.style.marginTop = "100px"
    thead.setAttribute('class', 'thead-light')
    var tr = document.createElement('tr'), th = document.createElement('th')
    th.style.textAlign = "center"
    tr.appendChild(th)
    th.appendChild(document.createTextNode('Colour Code'))
    var th = document.createElement('th')
    th.style.textAlign = "center"
    tr.appendChild(th)
    th.appendChild(document.createTextNode('Confidence Interval (%)'))
    tbdy.appendChild(tr)
    const colourList = ["#77c548", "#ffa500", "#ff0000"]
    const confidenceIntervalList = ["100 - 65", "65 - 35", "35 - 0"]
    for (var i = 0; i < colourList.length; i++) {
        var tr = document.createElement('tr'), td = document.createElement('td'), td1 = document.createElement('td')
        td.style.backgroundColor = colourList[i]
        var confidence = document.createTextNode(confidenceIntervalList[i])
        td1.appendChild(confidence)
        td.appendChild(td1)
        tr.appendChild(td)
        td1.style.textAlign = "center"
        tr.appendChild(td1)
        tbdy.appendChild(tr)
    }
    thead.appendChild(tbdy)
    tbl.appendChild(thead)
    body.appendChild(tbl)
}

/** Gets the patient name, ID and confidence for pills**/
function getPatientData() {
	firebase.auth().onAuthStateChanged(function(user) {
	if (!user){
		window.location.replace("/nurseLogin");
	}
//	var user = firebase.auth().currentUser;
  	console.log(user.uid);
//	firebase.firestore().doc("patients/Y7PjItKBXyeroWFb1YgzphlQ0FN2").get().then( doc => console.log(doc.data()));
	firebase.firestore().collection("patients").where("nurseID","==",user.uid).orderBy("name", "asc").get().then(querySnapshot => {
        querySnapshot.forEach(doc => {
            usernamesList.push(doc.data().name)
            patientsConfidence[doc.data().name] = []
            idList.push(doc.id)
            var confidence = firebase.firestore().collection("patients").doc(doc.id).collection("votVideoRefs").get().then(q => {
                q.forEach(d => {
                    result = d.data()
                    const pc = findConfidence(result.timestampsAndConfidences)
                    var vid = result.videoPath
                    videoList.push(vid)
                    timestampConfidenceList.push(pc)
                    var utcSeconds = result.timestamp['seconds']
                    var date = new Date(0);
                    date.setUTCSeconds(utcSeconds);
                    var formattedTime = (date.getMonth() + 1) + "/" + date.getDate()
                    dateList.push(formattedTime)
                })
                patientsConfidence[doc.data().name] = { confidence: timestampConfidenceList, timestamp: dateList, videos: videoList }
                timestampConfidenceList = []
                dateList = []
                videoList = []
                return timestampConfidenceList
            })
            result = confidence
            return result
        });
 
	if(result.then)
		{
	    console.log(result)
	    result.then(function (value) {
            createColouredGrid()
            createLegend()
            displayVideo()
	    })
        }
    });
});
}

/** Displays video if a table cell is clicked **/
function displayVideo() {
    var modal = document.getElementById('myModal')
    var span = document.getElementsByClassName("close")[0]
    span.onclick = function () {
        modal.style.display = "none"

    }
    document.querySelectorAll('td')
        .forEach(e => e.addEventListener("click", function () {
            if (patientsConfidence[usernamesList[e.id]] != undefined) {
                for (var i = 0; i < Object.keys(patientsConfidence[usernamesList[e.id]]['timestamp']).length; i++) {
                    if (patientsConfidence[usernamesList[e.id]]['timestamp'][i] == e.innerHTML) {
                        if (patientsConfidence[usernamesList[e.id]]['videos'][i] != undefined && patientsConfidence[usernamesList[e.id]]['videos'][i] != "") {
                            firebase.storage().ref(patientsConfidence[usernamesList[e.id]]['videos'][i]).getDownloadURL().then(url => {
                                modal.style.display = "block"
                                var video = document.getElementById('video')
                                var source = document.getElementById('source')
                                var patientHeader = document.getElementById("patientHeader")
                                var patientTimestamp = document.getElementById("patientTimestamp")
                                patientHeader.innerHTML = "Currently viewing: "+  usernamesList[e.id]
                                console.log(patientsConfidence[usernamesList[e.id]]['timestamp'][i])
                                patientTimestamp.innerHTML = "Recorded on: " + e.innerHTML
                                source.src = url
                                source.type = "video/mp4"
                                modal.style.display = "block";
                                video.load()
                            })
                        }
                        else
                        {
                            modal.style.display = "none"
                        }
                    }
                }
            }
        }));
}

getPatientData()
