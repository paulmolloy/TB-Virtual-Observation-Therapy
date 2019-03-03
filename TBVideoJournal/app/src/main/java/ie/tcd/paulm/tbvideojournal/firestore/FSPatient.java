package ie.tcd.paulm.tbvideojournal.firestore;

import android.util.Log;

import java.util.Map;

/** Patient data downloaded from Firestore */
public class FSPatient {

    public String name, email, nurseID;
    public FSVotVideoRef[] votVideoRefs;

    public static void download(String patientID, PatientDownloadSuccess onSuccess, PatientDownloadFail onFail){

        Firestore.getDocument("patients/" + patientID)
            .addOnSuccessListener(d -> {
                FSPatient p = new FSPatient();
                p.name = d.getString("name");
                p.email = d.getString("email");
                p.nurseID = d.getString("nurseID");
                // TODO(paulmolloy): Figure out if you really want to download
                // all references here.
                p.votVideoRefs = null;
                onSuccess.run(p);
            })
            .addOnFailureListener(e -> onFail.run(e.getMessage()));

    }

    public interface PatientDownloadSuccess {
        void run(FSPatient patient);
    }

    public interface PatientDownloadFail {
        void run(String error);
    }



}
