package ie.tcd.paulm.tbvideojournal.firestore;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.type.Date;

import java.util.ArrayList;
import java.util.List;

import ie.tcd.paulm.tbvideojournal.auth.Auth;

public class FSVotVideoRef {
    public String videoPath;
    public String label;
    public Object timestampsAndConfidences;
    @ServerTimestamp Timestamp timestamp; // This will be automatically set by the server when the document gets uploaded
    private static final String  TAG              = "FSVotVideoRef";
    public static final String VOT_PART_PATH = "/votVideoRefs";

    // addVideoReference saves a reference to every vot video a patient has made.
    // It keeps the path in Firebase Storage, human redable label for video, and will have
    // VoT section timestamps datastructure.
    public static void addVideoReference(String documentID, String videoPath, String label, Object timestamps){
        // Add a new document with a generated ID

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FSVotVideoRef vot = new FSVotVideoRef();
        vot.videoPath = videoPath;
        vot.label = label;
        vot.timestampsAndConfidences = timestamps;
        db.document("patients/" + Auth.getCurrentUserID() + VOT_PART_PATH + "/" + documentID)
                .set(vot)
                .addOnSuccessListener(r -> Log.d(TAG, "Document " + documentID + " has been uploaded"))
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    // downloadBotReferences just downloads the votVideoReferences for a patient.
    public static void downloadVotReferences(String patientID, FSVotVideoRef.VotVideoRefDownloadSuccess onSuccess, FSVotVideoRef.VotVideoRefDownloadFail onFail){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("patients/" + patientID + VOT_PART_PATH).get()
                .addOnSuccessListener(d -> {
                    List<FSVotVideoRef> vots = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : d) {
                        FSVotVideoRef vot = new FSVotVideoRef();
                        vot.videoPath = doc.getString("videoPath");
                        vot.label = doc.getString("label");
                        vot.timestampsAndConfidences = doc.get("timestampsAndConfidences");
                        vot.timestamp = doc.getTimestamp("timestamp");
                        vots.add(vot);
                    }
                    onSuccess.run(vots);
                })
                .addOnFailureListener(e -> onFail.run(e.getMessage()));

    }

    public interface VotVideoRefDownloadSuccess {
        void run(List<FSVotVideoRef> vots);
    }

    public interface VotVideoRefDownloadFail {
        void run(String error);
    }

}
