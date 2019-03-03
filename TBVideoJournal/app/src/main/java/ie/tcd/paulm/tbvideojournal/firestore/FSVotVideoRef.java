package ie.tcd.paulm.tbvideojournal.firestore;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ie.tcd.paulm.tbvideojournal.auth.Auth;

public class FSVotVideoRef {
    public String videoPath;
    public String label;
    // TODO(edvardas): Add timestamp data-structure here.
    private static final String  TAG              = "FSVotVideoRef";
    public static final String VOT_PART_PATH = "/votVideoRefs";

    public static void addVideoReference(String videoPath, String label){
        // Add a new document with a generated ID

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FSVotVideoRef vot = new FSVotVideoRef();
        vot.videoPath = videoPath;
        vot.label = label;
        db.collection("patients/" + Auth.getCurrentUserID() + VOT_PART_PATH)
                .add(vot)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
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
                        // TODO(edvardas): Get timestamps here.
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
