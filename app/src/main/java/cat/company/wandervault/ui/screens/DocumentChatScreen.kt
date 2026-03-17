package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Document Chat screen entry point.
 *
 * Allows the user to ask consecutive free-form questions about a document and view the
 * conversation history in a chat-style layout.
 *
 * @param documentId The ID of the document to chat about.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun DocumentChatScreen(
    documentId: Int,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentChatViewModel = koinViewModel(
        key = "DocumentChatViewModel:$documentId",
        parameters = { parametersOf(documentId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentChatContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onSendMessage = viewModel::sendMessage,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Document Chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentChatContent(
    uiState: DocumentChatUiState,
    onNavigateUp: () -> Unit,
    onSendMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = if (uiState is DocumentChatUiState.Success) {
        uiState.documentName
    } else {
        stringResource(R.string.document_chat_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.document_chat_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is DocumentChatUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is DocumentChatUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.document_chat_not_found))
                }
            }

            is DocumentChatUiState.Success -> {
                DocumentChatSuccessContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onSendMessage = onSendMessage,
                )
            }
        }
    }
}

@Composable
private fun DocumentChatSuccessContent(
    uiState: DocumentChatUiState.Success,
    innerPadding: PaddingValues,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to the bottom whenever new messages are added.
    val messageCount = uiState.messages.size + if (uiState.isThinking) 1 else 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding(),
    ) {
        // ── Message list ───────────────────────────────────────────────────────

        if (uiState.messages.isEmpty() && !uiState.isThinking) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.document_chat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.messages) { message ->
                    ChatMessageRow(message = message)
                }
                if (uiState.isThinking) {
                    item {
                        ThinkingIndicator(downloadingBytes = uiState.downloadingBytes)
                    }
                }
            }
        }

        // ── Input bar ──────────────────────────────────────────────────────────

        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(stringResource(R.string.document_chat_input_hint)) },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
            )
            IconButton(
                onClick = {
                    val q = inputText.trim()
                    if (q.isNotEmpty() && !uiState.isThinking) {
                        inputText = ""
                        onSendMessage(q)
                    }
                },
                enabled = inputText.trim().isNotEmpty() && !uiState.isThinking,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.document_chat_send),
                    tint = if (inputText.trim().isNotEmpty() && !uiState.isThinking) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message is ChatMessage.UserMessage
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        when (message) {
            is ChatMessage.UserMessage -> {
                ChatBubble(
                    text = message.text,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isUser = true,
                )
            }

            is ChatMessage.AiMessage -> {
                ChatBubble(
                    text = message.text,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    isUser = false,
                )
            }

            is ChatMessage.ErrorMessage -> {
                val errorText = if (message.message == null) {
                    stringResource(R.string.document_chat_error_unavailable)
                } else {
                    stringResource(R.string.document_chat_error_generic)
                }
                ChatBubble(
                    text = errorText,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    isUser = false,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    isUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = modifier.widthIn(max = 300.dp),
    ) {
        if (isUser) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            MarkdownText(
                markdown = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(
    downloadingBytes: Long? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                val label = if (downloadingBytes != null) {
                    stringResource(R.string.document_chat_downloading, formatBytes(downloadingBytes))
                } else {
                    stringResource(R.string.document_chat_thinking)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun DocumentChatContentPreview() {
    WanderVaultTheme {
        DocumentChatContent(
            uiState = DocumentChatUiState.Success(
                documentName = "boarding_pass.pdf",
                documentUri = "file:///boarding_pass.pdf",
                documentMimeType = "application/pdf",
                messages = listOf(
                    ChatMessage.UserMessage("What is the departure gate?"),
                    ChatMessage.AiMessage("The departure gate is **B12**."),
                    ChatMessage.UserMessage("When does boarding start?"),
                    ChatMessage.AiMessage("Boarding starts at **07:50**, 55 minutes before the scheduled departure."),
                ),
                isThinking = false,
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentChatContentEmptyPreview() {
    WanderVaultTheme {
        DocumentChatContent(
            uiState = DocumentChatUiState.Success(
                documentName = "itinerary.pdf",
                documentUri = "file:///itinerary.pdf",
                documentMimeType = "application/pdf",
                messages = emptyList(),
                isThinking = false,
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentChatContentThinkingPreview() {
    WanderVaultTheme {
        DocumentChatContent(
            uiState = DocumentChatUiState.Success(
                documentName = "hotel_confirmation.pdf",
                documentUri = "file:///hotel.pdf",
                documentMimeType = "application/pdf",
                messages = listOf(
                    ChatMessage.UserMessage("What are the check-in and check-out dates?"),
                ),
                isThinking = true,
            ),
            onNavigateUp = {},
        )
    }
}
