package ie.tcd.paulm.tbvideojournal.steps

import ie.tcd.paulm.tbvideojournal.MainActivity
import ie.tcd.paulm.tbvideojournal.misc.Misc
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import ie.tcd.paulm.tbvideojournal.R
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator


class PillIntakeGuide(private val root: MainActivity, private val cameraScreen: RelativeLayout) {

    // Inflate and add the overlay to the camera screen
    private val guideContainer = LayoutInflater.from(root).inflate(R.layout.layout_pill_intake_guide, cameraScreen)
    private val doneScreen = LayoutInflater.from(root).inflate(R.layout.layout_pill_intake_guide_done, cameraScreen, false)

    // Find all the required components
    private val stepInstruction: TextView = find(R.id.PillGuide_stepDescription)
    private val takingPillName: TextView = find(R.id.PillGuide_takingPillName)
    private val stepImage: ImageView = find(R.id.PillGuide_stepImage)
    private val nextStepButton: Button = find(R.id.PillGuide_nextStep)
    private val progressContainer: LinearLayout = find(R.id.PillGuide_pillProgressContainer)
    private val progressScrollview: HorizontalScrollView = find(R.id.PillGuide_pillProgressScrollview)
    private val uploadProgressBar: ProgressBar = doneScreen.findViewById(R.id.upload_progressbar)
    private val closeButton: Button = doneScreen.findViewById(R.id.PillGuideDone_close)

    // Define all the steps
    private val steps = arrayOf(
        StepDescription(1, R.drawable.step_1, "Place pill into circle"),
        StepDescription(2, R.drawable.step_2, "Place pill in mouth"),
        StepDescription(3, R.drawable.step_3, "Swallow pill"),
        StepDescription(4, R.drawable.step_4, "Show empty mouth")
    )

    // This will eventually be downloaded from Firebase
    private val prescriptions = arrayOf(
        PillPrescription("Paracetamol", 1),
        PillPrescription("Penicillin", 2),
        PillPrescription("Ibuprofen", 1)
    )

    private val progresses: MutableList<PillProgress> = mutableListOf()

    private var currentCounter = 0
    private val currentStep get() = (currentCounter - 1) % 4
    private val currentPill get() = (currentCounter - 1) / 4

    private val haveAllPillsBeenTaken get() = currentPill >= progresses.size
    private val currentStepsDescription get() = steps[currentStep]
    private val currentPillsUI get() = progresses[currentPill]

    private var onStepChangedListener = { pillNumber: Int, stepNumber: Int -> }
    private var onFinishedListener = { }

    init {

        fillProgressIndicator()

        nextStepButton.setOnClickListener {
            if(!haveAllPillsBeenTaken) nextStep()
        }

        closeButton.setOnClickListener { root.onBackPressed() }

    }

    /**
     * Moves on to the next step, for example:
     * - If currently on pill 2, step 2, moves on to pill 2, step 3
     * - If currently on pill 8, step 4, moves on to pill 9, step 1
     */
    fun nextStep(){

        currentPillsUI.markAsFinished(currentStep) // Mark current one as done
        currentCounter++ // Move on to the next step

        // Check if the user has taken all their required pills
        if(haveAllPillsBeenTaken) finished()
        else {

            currentPillsUI.markAsOngoing(currentStep)

            // Update text and image at the top of the screen
            val pillName = currentPillsUI.prescription.pillName
            takingPillName.text = "Taking $pillName"

            stepInstruction.text = currentStepsDescription.instruction
            stepImage.setImageResource(currentStepsDescription.imageID)

            // Scroll to current pill's progress
            scrollToView(currentPillsUI.container)

            onStepChangedListener(currentPill, currentStep)

        }

    }

    /** Out of 100 */
    fun setUploadProgress(progress: Int){
        uploadProgressBar.isIndeterminate = false
        uploadProgressBar.progress = progress
    }

    /** This will be called every time the user moves on to the next step (i.e. when nextStep() is called)  */
    fun onStepChanged(listener: (pillNumber: Int, stepNumber: Int) -> Unit) {
        onStepChangedListener = listener
    }

    /** This will be called when the user has taken all the pills they are required to take  */
    fun onAllPillsTaken(listener: () -> Unit) {
        onFinishedListener = listener
    }









    private fun finished(){
        cameraScreen.addView(doneScreen)
        uploadProgressBar.isIndeterminate = true
        onFinishedListener()
    }

    private fun scrollToView(view: View){

        val target = view.left - (view.width/2)

        val animation = ObjectAnimator.ofInt(progressScrollview, "scrollX", target)
        animation.interpolator = DecelerateInterpolator()
        animation.setDuration(1000).start()

    }

    /** Inits all progress UIs (the stuff in the scroll view at the bottom) using the data in prescriptions */
    private fun fillProgressIndicator(){

        for (prescription in prescriptions){
            for(i in 1..prescription.amount) progresses.add(PillProgress(prescription, progressContainer, root))
        }

    }


    private fun <T : View>find(id: Int) = guideContainer.findViewById<T>(id)


}

data class PillPrescription(val pillName: String, val amount: Int, val color: String = "#FFF")
data class StepDescription(val stepNumber: Int, val imageID: Int, val instruction: String)

