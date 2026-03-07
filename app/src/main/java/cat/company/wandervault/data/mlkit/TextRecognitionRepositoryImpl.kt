package cat.company.wandervault.data.mlkit

import android.content.Context
import android.net.Uri
import cat.company.wandervault.domain.repository.TextRecognitionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implements [TextRecognitionRepository] using ML Kit's on-device Latin text recognizer.
 */
class TextRecognitionRepositoryImpl(private val context: Context) : TextRecognitionRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractTextFromImage(imageUri: String): String? {
        return try {
            val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
            processImage(image)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Runs text recognition on a pre-built [InputImage] and returns the recognized text or null.
     */
    suspend fun recognizeImage(image: InputImage): String? = processImage(image)

    private suspend fun processImage(image: InputImage): String? =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text.takeIf { it.isNotBlank() }
                    cont.resume(text)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
}
