package ie.tcd.paulm.tbvideojournal.firestore;

/** Nurse data downloaded from Firestore */
public class FSNurse {

    public String name, email;

    public static void download(String nurseID, NurseDownloadSuccess onSuccess, NurseDownloadFail onFail){

        Firestore.getDocument("nurses/" + nurseID)
            .addOnSuccessListener(d -> {
                FSNurse n = new FSNurse();
                n.name = d.getString("name");
                n.email = d.getString("email");
                onSuccess.run(n);
            })
            .addOnFailureListener(e -> onFail.run(e.getMessage()));

    }

    public interface NurseDownloadSuccess {
        void run(FSNurse nurse);
    }

    public interface NurseDownloadFail {
        void run(String error);
    }

}
