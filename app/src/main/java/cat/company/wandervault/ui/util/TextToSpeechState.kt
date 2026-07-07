package cat.company.wandervault.ui.util

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val TTS_UTTERANCE_ID = "wandervault_tts"

/**
 * Manages a [TextToSpeech] engine lifecycle and exposes simple speak/stop controls
 * as observable Compose state.
 */
class TextToSpeechState(tts: TextToSpeech) {

    /** The text currently being spoken, or `null` when silent. */
    var speakingText: String? by mutableStateOf(null)
        private set

    private val engine: TextToSpeech = tts

    init {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                speakingText = null
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speakingText = null
            }
        })
    }

    /**
     * Speaks [text] (stripping markdown syntax beforehand).
     * Stops any in-progress speech first.
     */
    fun speak(text: String) {
        speakingText = text
        val result = engine.speak(text.stripMarkdown(), TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            speakingText = null
        }
    }

    /** Stops any in-progress speech. */
    fun stop() {
        engine.stop()
        speakingText = null
    }

    internal fun dispose() {
        engine.stop()
        engine.shutdown()
    }

    /**
     * Strips common Markdown syntax from the string so the TTS engine reads
     * plain prose rather than raw markdown tokens.
     */
    private fun String.stripMarkdown(): String =
        replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")   // bold / italic
            .replace(Regex("_{1,3}([^_]+)_{1,3}"), "$1")   // underscores
            .replace(Regex("#+\\s*"), "")                   // headings
            .replace(Regex("`+([^`]+)`+"), "$1")            // inline code / code blocks
            .replace(Regex("!?\\[([^]]*)]\\([^)]+\\)"), "$1") // links / images
            .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "") // list bullets
            .replace(Regex("^>+\\s*", RegexOption.MULTILINE), "")    // blockquotes
            .replace(Regex("-{3,}|\\*{3,}"), "")            // horizontal rules
            .trim()
}

/**
 * Creates and remembers a [TextToSpeechState] that is automatically shut down
 * when the composable leaves the composition.
 */
@Composable
fun rememberTextToSpeech(): TextToSpeechState {
    val context = LocalContext.current
    // Allocate state holder first so the TTS init callback can reference it.
    val ttsState = remember {
        // TextToSpeech.OnInitListener is called on the main thread after the engine
        // connects – we don't need any special handling; speak() is a no-op until the
        // engine is ready (QUEUE_FLUSH on an uninitialised engine is silently ignored).
        val tts = TextToSpeech(context) { /* init status intentionally ignored */ }
        TextToSpeechState(tts)
    }
    DisposableEffect(Unit) {
        onDispose { ttsState.dispose() }
    }
    return ttsState
}
