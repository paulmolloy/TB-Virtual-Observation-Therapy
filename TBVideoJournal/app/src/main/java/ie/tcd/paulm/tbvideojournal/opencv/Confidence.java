package ie.tcd.paulm.tbvideojournal.opencv;

import android.util.Log;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Confidence {
    private static final String  TAG              = "Confidence";
    List<Integer> faceConfidences;
    List<Integer> pillConfidences;
    boolean isSwallowed = false;

    public Confidence() {

        faceConfidences = new ArrayList<Integer>();
        pillConfidences = new ArrayList<Integer>();

    }
    public void addFaceConfidence(int faceConfidence) {
        faceConfidences.add(faceConfidence);
    }

    // TODO(paulmolloy)

    // Percentage of frames where pill was detected.
    //
    public void addPillConfidence(int pillConfidence) {
        pillConfidences.add(pillConfidence);
    }

    public void setIsSwallowed(boolean isSwallowed) {
        this.isSwallowed = isSwallowed;
    }

// getConfidence gets the likely-hood that step was completed properly.
// If swallow pill step:
//  if swallowed (1 + %frames_with_face)/2
//  else:    (%frames_with_face + %frames_with_pill)/2
// If other step %frames_with_pill
    public float getConfidence() {
        float avgFace = 0;
        if(faceConfidences.size()>0){
            // Avoid possible div 0.
            float sum = 0;
            faceConfidences.toArray(new Integer[faceConfidences.size()]);
            // Copy List into array to get around possible concurrent exception.
            for(int confidence : faceConfidences.toArray(new Integer[faceConfidences.size()])) {
                sum += confidence;
            }
            avgFace = sum/faceConfidences.size();
        }

        float avgPill = 0;
        if(pillConfidences.size()>0){
            // Avoid possible div 0.
            float sum = 0;
            pillConfidences.toArray(new Integer[pillConfidences.size()]);
            // Copy List into array to get around possible concurrent exception.
            for(int confidence : pillConfidences.toArray(new Integer[pillConfidences.size()])) {
                sum += confidence;
            }
            avgPill = sum/pillConfidences.size();
        }
        Log.d(TAG, "Step is swallowed:" + isSwallowed);
        if (isSwallowed) return (1+avgFace)/2;
        return (avgPill+avgFace)/2;

    }

    public void reset() {
        faceConfidences.clear();
        pillConfidences.clear();
        isSwallowed = false;
    }

}
