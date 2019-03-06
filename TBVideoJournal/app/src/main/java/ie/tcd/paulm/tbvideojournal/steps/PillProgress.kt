package ie.tcd.paulm.tbvideojournal.steps

import android.content.Context
import android.graphics.Color
import android.opengl.Visibility
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ie.tcd.paulm.tbvideojournal.R

class PillProgress(val prescription: PillPrescription, addTo: ViewGroup, private val context: Context){

    val container = LayoutInflater.from(context).inflate(R.layout.layout_pill_intake_guide_progress, addTo,false)

    val pillName: TextView = find(R.id.PillProgress_pillName)
    val ticks: Array<ImageView> = arrayOf(
            find(R.id.PillProgress_tick0),
            find(R.id.PillProgress_tick1),
            find(R.id.PillProgress_tick2),
            find(R.id.PillProgress_tick3)
    )

    init {

        addTo.addView(container)

        pillName.text = prescription.pillName

    }

    fun markAsFinished(index: Int){
        if(index in 0..3) {

            val t = ticks[index]
            val c = Color.parseColor("#008577")
            t.setImageResource(R.drawable.tick)
            t.setColorFilter(c)
            t.alpha = 1f

            if(index == 3) pillName.setTextColor(c)

        }
    }

    fun markAsOngoing(index: Int){
        if(index in 0..3) {
            val t = ticks[index]
            t.setImageResource(R.drawable.ticknt)
            t.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            t.alpha = 0.5f
        }
    }





    private fun <T : View>find(id: Int) = container.findViewById<T>(id)

}