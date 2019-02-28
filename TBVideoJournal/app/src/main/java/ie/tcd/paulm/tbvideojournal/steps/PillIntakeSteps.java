package ie.tcd.paulm.tbvideojournal.steps;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.databinding.LayoutPillIntakeStepsBinding;
import ie.tcd.paulm.tbvideojournal.misc.Misc;

public class PillIntakeSteps {

    private LayoutPillIntakeStepsBinding bindings;

    private MainActivity root;
    private View cameraScreen;

    public final StepDescription[] stepDescriptions = {
        new StepDescription(0, R.drawable.step_0, "Make sure your face is visible"),
        new StepDescription(1, R.drawable.step_1, "Place pill into circle"),
        new StepDescription(2, R.drawable.step_2, "Place pill in mouth"),
        new StepDescription(3, R.drawable.step_3, "Swallow pill"),
        new StepDescription(4, R.drawable.step_4, "Show empty mouth")
    };

    public PillIntakeSteps(MainActivity root, RelativeLayout cameraScreen) {

        this.root = root;
        this.cameraScreen = cameraScreen;

        // Inflate and add overlay view to camera screen
        bindings = LayoutPillIntakeStepsBinding.inflate(LayoutInflater.from(root));
        cameraScreen.addView(bindings.getRoot());

        bindings.setSteps(this);
        bindings.setCurrentStep(0);
        bindings.setCurrentPill(1);

    }

    /**
     * Manually set the current pill the user is in the process of taking (The number that is being
     * displayed beside "Taking pill number _")
     */
    public void setCurrentPillNumber(int pillNumber){
        bindings.setCurrentPill(pillNumber);
    }

    /**
     * @return  The current pill the user is in the process of taking (The number that is being
     *          displayed beside "Taking pill number _")
     */
    public int getCurrentStepNumber(){
        return bindings.getCurrentStep();
    }

    /**
     * Manually set the current step the user is on: <br>
     *      - Step 0: Make sure your face is visible <br>
     *      - Step 1: Place pill into circle <br>
     *      - Step 2: Place pill in mouth <br>
     *      - Step 3: Swallow pill <br>
     *      - Step 4: Show empty mouth <br>
     */
    public void setCurrentStepNumber(int stepNumber){
        bindings.setCurrentStep(stepNumber);
    }

    /**
     * @return The current step the user is on: <br>
     *      - Step 0: Make sure your face is visible <br>
     *      - Step 1: Place pill into circle <br>
     *      - Step 2: Place pill in mouth <br>
     *      - Step 3: Swallow pill <br>
     *      - Step 4: Show empty mouth <br>
     */
    public int getCurrentPillNumber(){
        return bindings.getCurrentPill();
    }

    /**
     * Moves on to the next step, for example:<br>
     *     - If currently on pill 2, step 2, moves on to pill 2, step 3 <br>
     *     - If currently on pill 8, step 4, moves on to pill 9, step 1
     */
    public void nextStep(){
        int currentStep = bindings.getCurrentStep() + 1;

        if(currentStep > 4){
            currentStep = 1;
            bindings.setCurrentPill(bindings.getCurrentPill() + 1);
        }

        bindings.setCurrentStep(currentStep);
    }

    public void onNextPressed(View view){
        nextStep();
    }

    public static class StepDescription {

        public String description;
        public int imageID, stepNumber;

        public StepDescription(int stepNumber, int imageID, String description){

            this.stepNumber = stepNumber;
            this.imageID = imageID;
            this.description = description;

        }

    }

    @BindingAdapter("android:src")
    public static void setImageResource(ImageView imageView, int resource){
        imageView.setImageResource(resource);
    }


}
