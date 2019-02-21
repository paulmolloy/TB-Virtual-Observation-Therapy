package ie.tcd.paulm.tbvideojournal.firestore;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Firestore {

    // Lazy init Firestore
    private static FirebaseFirestore _fs;
    public static FirebaseFirestore fs(){
        if(_fs == null) _fs = FirebaseFirestore.getInstance();
        return _fs;
    }

    /** Convenience method to download a document from Firestore */
    public static Task<DocumentSnapshot> getDocument(String path){
        return fs().document(path).get();
    }

}
