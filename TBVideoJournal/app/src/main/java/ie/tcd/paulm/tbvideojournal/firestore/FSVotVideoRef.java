package ie.tcd.paulm.tbvideojournal.firestore;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import ie.tcd.paulm.tbvideojournal.auth.Auth;

public class FSVotVideoRef {
    String videoPath;
    // TODO(edvardas): Add timestamp data-structure here.
    private static final String  TAG              = "FSVotVideoRef";

    public static void addVideoReference(String videoPath){
        // Add a new document with a generated ID

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FSVotVideoRef vot = new FSVotVideoRef();
        vot.videoPath = videoPath;
        db.collection("patients/" + Auth.getCurrentUserID() + "/vot-video-refs")
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



}
