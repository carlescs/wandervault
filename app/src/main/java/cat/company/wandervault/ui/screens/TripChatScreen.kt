package cat.company.wandervault.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.TripChatSession
import cat.company.wandervault.ui.theme.WanderVaultTheme
import cat.company.wandervault.ui.util.formatBytes
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Trip chat tab entry point.
 */
@Composable
fun TripChatTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: TripChatViewModel = koinViewModel(
        key = "TripChatViewModel:$tripId",
        parameters = { parametersOf(tripId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripChatContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onSendMessage = viewModel::sendMessage,
        onSelectChatSession = viewModel::selectChatSession,
        onCreateNewChat = viewModel::createNewChat,
        onDeleteChatSession = viewModel::deleteChatSession,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TripChatContent(
    uiState: TripChatUiState,
    innerPadding: PaddingValues,
    onSendMessage: (String) -> Unit = {},
    onSelectChatSession: (Int) -> Unit = {},
    onCreateNewChat: () -> Unit = {},
    onDeleteChatSession: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is TripChatUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is TripChatUiState.NotFound -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.trip_chat_not_found))
            }
        }

        is TripChatUiState.Success -> {
            TripChatSuccessContent(
                uiState = uiState,
                innerPadding = innerPadding,
                onSendMessage = onSendMessage,
                onSelectChatSession = onSelectChatSession,
                onCreateNewChat = onCreateNewChat,
                onDeleteChatSession = onDeleteChatSession,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripChatSuccessContent(
    uiState: TripChatUiState.Success,
    innerPadding: PaddingValues,
    onSendMessage: (String) -> Unit,
    onSelectChatSession: (Int) -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChatSession: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messageCount = uiState.messages.size + if (uiState.isThinking) 1 else 0

    LaunchedEffect(uiState.selectedChatSessionId, messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
    ) {
        TripChatHistoryBar(
            uiState = uiState,
            onSelectChatSession = onSelectChatSession,
            onCreateNewChat = onCreateNewChat,
            onDeleteChatSession = onDeleteChatSession,
        )
        HorizontalDivider()
        if (uiState.messages.isEmpty() && !uiState.isThinking) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.isAiAvailable) {
                        stringResource(R.string.trip_chat_empty, uiState.trip.title)
                    } else {
                        stringResource(R.string.trip_chat_unavailable)
                    },
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
                    TripChatMessageRow(message = message)
                }
                if (uiState.isThinking) {
                    item {
                        TripChatThinkingIndicator(downloadingBytes = uiState.downloadingBytes)
                    }
                }
            }
        }

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
                placeholder = { Text(stringResource(R.string.trip_chat_input_hint)) },
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                enabled = uiState.isAiAvailable && !uiState.isThinking,
            )
            IconButton(
                onClick = {
                    val question = inputText.trim()
                    if (question.isNotEmpty() && uiState.isAiAvailable && !uiState.isThinking) {
                        inputText = ""
                        onSendMessage(question)
                    }
                },
                enabled = inputText.trim().isNotEmpty() && uiState.isAiAvailable && !uiState.isThinking,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.trip_chat_send),
                    tint = if (
                        inputText.trim().isNotEmpty() &&
                        uiState.isAiAvailable &&
                        !uiState.isThinking
                    ) {
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
private fun TripChatHistoryBar(
    uiState: TripChatUiState.Success,
    onSelectChatSession: (Int) -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChatSession: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedSession = uiState.chatSessions.firstOrNull { it.id == uiState.selectedChatSessionId }
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TextButton(
                onClick = { expanded = true },
                enabled = uiState.chatSessions.isNotEmpty(),
            ) {
                Text(
                    text = selectedSession?.let {
                        stringResource(R.string.trip_chat_session_item, it.updatedAt.format(formatter))
                    } ?: stringResource(R.string.trip_chat_history_empty),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                uiState.chatSessions.forEach { session ->
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.trip_chat_session_item, session.updatedAt.format(formatter)))
                        },
                        onClick = {
                            expanded = false
                            onSelectChatSession(session.id)
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onCreateNewChat,
            enabled = !uiState.isThinking,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.trip_chat_new_chat),
            )
        }
        IconButton(
            onClick = {
                uiState.selectedChatSessionId?.let(onDeleteChatSession)
            },
            enabled = uiState.selectedChatSessionId != null && !uiState.isThinking,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.trip_chat_delete_chat),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripChatMessageRow(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message is ChatMessage.UserMessage
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedLabel = stringResource(R.string.trip_chat_message_copied)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        when (message) {
            is ChatMessage.UserMessage -> {
                TripChatBubble(
                    text = message.text,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isUser = true,
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, copiedLabel, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            is ChatMessage.AiMessage -> {
                TripChatBubble(
                    text = message.text,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    isUser = false,
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, copiedLabel, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            is ChatMessage.ErrorMessage -> {
                val errorText = if (message.message == null) {
                    stringResource(R.string.trip_chat_error_unavailable)
                } else {
                    stringResource(R.string.trip_chat_error_generic)
                }
                TripChatBubble(
                    text = errorText,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    isUser = false,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripChatBubble(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
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
        modifier = modifier
            .widthIn(max = 300.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
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
private fun TripChatThinkingIndicator(
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
                Text(
                    text = if (downloadingBytes != null) {
                        stringResource(R.string.trip_chat_downloading, formatBytes(downloadingBytes))
                    } else {
                        stringResource(R.string.trip_chat_thinking)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripChatContentPreview() {
    WanderVaultTheme {
        TripChatContent(
            uiState = TripChatUiState.Success(
                trip = Trip(
                    id = 1,
                    title = "Japan Spring Trip",
                    startDate = LocalDate.of(2026, 4, 3),
                    endDate = LocalDate.of(2026, 4, 12),
                ),
                messages = listOf(
                    ChatMessage.UserMessage("What hotel am I staying at in Kyoto?"),
                    ChatMessage.AiMessage("You are staying at **Sakura Inn Kyoto** during that stop."),
                ),
                chatSessions = listOf(
                    TripChatSession(
                        id = 1,
                        tripId = 1,
                        createdAt = ZonedDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                        updatedAt = ZonedDateTime.of(2026, 3, 1, 9, 5, 0, 0, ZoneOffset.UTC),
                    ),
                ),
                selectedChatSessionId = 1,
            ),
            innerPadding = PaddingValues(),
        )
    }
}
