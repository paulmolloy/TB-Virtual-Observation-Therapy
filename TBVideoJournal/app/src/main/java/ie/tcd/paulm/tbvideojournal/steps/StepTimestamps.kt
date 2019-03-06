package ie.tcd.paulm.tbvideojournal.steps

class StepTimestamps {

    /**
     * The actual data structure that contains all of the data.
     * Can be directly uploaded to Firestore.
     *
     * It looks like this:
     * ```
     * {
     *    pill0: {
     *      step0: { time: int, confidence: float },
     *      step1: { time: int, confidence: float },
     *      step2: { time: int, confidence: float },
     *      step3: { time: int, confidence: float }
     *    },
     *    pill1: {
     *      step0: { time: int, confidence: float },
     *      step1: { time: int, confidence: float },
     *      ...
     *    },
     *    ...
     * }
     * ```
     */
    val map: Dict<Dict<Dict<Number>>> = mutableMapOf()

    fun setConfidence(pill: Int, step: Int, confidence: Float){
        setValue(pill, step, "confidence", confidence)
    }

    fun setTimestamp(pill: Int, step: Int, timestamp: Int){
        setValue(pill, step, "time", timestamp)
    }

    private fun setValue(pill: Int, step: Int, key: String, value: Number){

        val p = "pill$pill"
        val s = "step$step"

        if(map[p] == null) map[p] = mutableMapOf()
        if(map[p]!![s] == null) map[p]!![s] = mutableMapOf()

        map[p]!![s]!![key] = value

    }

}

typealias Dict<T> = MutableMap<String, T>