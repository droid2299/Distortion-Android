package app.myspoti.power

/** Playback transport and listening events are independent of this visual policy. */
data class EnergyPolicy(
    val saverEnabled: Boolean = false,
    val oledBlack: Boolean = false,
    val allowCanvas: Boolean = true,
    val lightweightUi: Boolean = false,
    val videoMaxHeight: Int = 1080,
    val allowBackgroundLookups: Boolean = true,
    val progressFps: Int = 30,
    /** 0 = no preference: the LTPO screen already drops its rate when nothing moves. */
    val displayRefreshCap: Float = 0f,
) {
    companion object {
        /**
         * Limit optional work on low-memory devices and when the app's energy saver is enabled.
         */
        fun resolve(
            manualSaver: Boolean,
            blackPreference: Boolean,
            canvasEnabled: Boolean = true,
            lowMemoryDevice: Boolean = false,
        ): EnergyPolicy {
            val saver = manualSaver || lowMemoryDevice
            return EnergyPolicy(
                saverEnabled = saver,
                lightweightUi = saver,
                oledBlack = blackPreference,
                allowCanvas = !saver && canvasEnabled,
                videoMaxHeight = if (saver) 480 else 1080,
                allowBackgroundLookups = !saver,
                progressFps = if (saver) 20 else 30,
                displayRefreshCap = if (saver) 60f else 0f,
            )
        }
    }
}

data class EnergyState(
    val manualSaver: Boolean = false,
    /** AMOLED appearance stays user-controlled while saver limits visual work. */
    val blackPreference: Boolean = false,
    /** The user's Canvas switch (Settings), independent of the saver. */
    val canvasEnabled: Boolean = true,
    val lowMemoryDevice: Boolean = false,
) {
    val policy: EnergyPolicy = EnergyPolicy.resolve(manualSaver, blackPreference, canvasEnabled, lowMemoryDevice)
}
