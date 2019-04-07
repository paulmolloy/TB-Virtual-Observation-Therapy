package ie.tcd.paulm.tbvideojournal.opencv;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Confidence {
    List<Integer> faceConfidences;

    public Confidence() {
        faceConfidences = new ArrayList<Integer>();
    }
    public void addFaceConfidence(int faceConfidence) {
        faceConfidences.add(faceConfidence);
    }
    // TODO(paulmolloy)
    public void addHandConfidence(int handConfidence) {

    }

    public float getConfidence() {
        if(faceConfidences.size()==0) return 0; // Avoid possible div 0.
        float sum = 0;
        faceConfidences.toArray(new Integer[faceConfidences.size()]);
        // Copy List into array to get around possible concurrent exception.
        for(int confidence : faceConfidences.toArray(new Integer[faceConfidences.size()])) {
            sum += confidence;
        }
        return sum/faceConfidences.size();

    }

    public void reset() {
        faceConfidences.clear();
    }

}
