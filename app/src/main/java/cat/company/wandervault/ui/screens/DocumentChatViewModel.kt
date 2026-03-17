package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.AskDocumentQuestionUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Document Chat screen.
 *
 * Loads the document identified by [documentId] and maintains a conversation history.
 * Each user question is appended to [messages] immediately; the AI answer (or an error
 * message) is appended once the request completes.
 *
 * @param documentId The ID of the document to chat about.
 * @param getDocumentById Use-case that streams the document entity.
 * @param askDocumentQuestion Use-case that sends a free-form question to Gemini Nano.
 */
class DocumentChatViewModel(
    private val documentId: Int,
    private val getDocumentById: GetDocumentByIdUseCase,
    private val askDocumentQuestion: AskDocumentQuestionUseCase,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isThinking = MutableStateFlow(false)
    private val _downloadingBytes = MutableStateFlow<Long?>(null)

    /** Job running the current AI request, kept so concurrent sends are prevented. */
    private var askJob: Job? = null

    val uiState: StateFlow<DocumentChatUiState> = combine(
        getDocumentById(documentId),
        _messages,
        _isThinking,
        _downloadingBytes,
    ) { document, messages, isThinking, downloadingBytes ->
        if (document == null) {
            DocumentChatUiState.NotFound
        } else {
            DocumentChatUiState.Success(
                documentName = document.name,
                documentUri = document.uri,
                documentMimeType = document.mimeType,
                messages = messages,
                isThinking = isThinking,
                downloadingBytes = downloadingBytes,
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DocumentChatUiState.Loading,
        )

    /**
     * Sends [question] to Gemini Nano and appends both the question and the AI answer to the
     * chat history. A no-op when the document is not yet loaded or a request is already in flight.
     */
    fun sendMessage(question: String) {
        val state = uiState.value as? DocumentChatUiState.Success ?: return
        if (_isThinking.value) return

        askJob?.cancel()
        askJob = viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage.UserMessage(question)
            _isThinking.value = true
            _downloadingBytes.value = null
            try {
                val answer = askDocumentQuestion(
                    fileUri = state.documentUri,
                    mimeType = state.documentMimeType,
                    question = question,
                ) { bytesDownloaded ->
                    _downloadingBytes.value = bytesDownloaded
                }
                if (answer == null) {
                    Log.w(TAG, "askDocumentQuestion returned null for document $documentId; AI unavailable")
                    _messages.value = _messages.value + ChatMessage.ErrorMessage(null)
                } else {
                    _messages.value = _messages.value + ChatMessage.AiMessage(answer)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Chat question failed for document $documentId", e)
                _messages.value = _messages.value + ChatMessage.ErrorMessage(e.message ?: e.toString())
            } finally {
                _isThinking.value = false
                _downloadingBytes.value = null
            }
        }
    }

    companion object {
        private const val TAG = "DocumentChatViewModel"
    }
}
