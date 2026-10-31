package cat.company.wandervault.data.mlkit

import android.content.Context
import android.net.Uri
import cat.company.wandervault.domain.repository.TextRecognitionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML Kit implementation of [TextRecognitionRepository].
 *
 * Images are decoded with [InputImage.fromFilePath] (which honours EXIF rotation, so photos
 * taken in portrait are recognised correctly) and processed by the on-device Latin text
 * recogniser. Nothing leaves the device and the image is not copied or stored.
 */
class TextRecognitionRepositoryImpl(private val context: Context) : TextRecognitionRepository {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeText(imageUri: String): String? = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
        textRecognizer.process(image).awaitResult().text.takeIf { it.isNotBlank() }
    }
}
