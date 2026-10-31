package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import java.util.Locale

/**
 * Shows the detected language of the destination notes and lets the user translate them
 * on-device with ML Kit.
 *
 * Renders nothing while no language has been detected and no translation is in progress.
 *
 * @param state The current [NotesLanguageState].
 * @param onTranslate Called when the user requests a translation.
 * @param onDismissTranslation Called when the user hides the translation.
 * @param onReplaceWithTranslation Called when the user wants to replace the notes with the
 *   translated text.
 */
@Composable
internal fun NotesLanguageSection(
    state: NotesLanguageState,
    onTranslate: () -> Unit,
    onDismissTranslation: () -> Unit,
    onReplaceWithTranslation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val detected = state.detectedLanguage
    val target = state.targetLanguage
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (detected != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.notes_detected_language,
                        languageDisplayName(detected, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                if (state.canTranslate && target != null &&
                    state.translation !is NotesTranslationState.Translated
                ) {
                    TextButton(
                        onClick = onTranslate,
                        enabled = state.translation != NotesTranslationState.Translating,
                    ) {
                        Text(
                            stringResource(
                                R.string.notes_translate_to,
                                languageDisplayName(target, locale),
                            ),
                        )
                    }
                }
            }
        } else if (state.detectionFailed) {
            Text(
                text = stringResource(R.string.notes_language_detection_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (val translation = state.translation) {
            NotesTranslationState.Idle -> Unit
            NotesTranslationState.Translating -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.notes_translating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NotesTranslationState.Unsupported -> Text(
                text = stringResource(R.string.notes_translation_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            NotesTranslationState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.notes_translation_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onTranslate) {
                    Text(stringResource(R.string.notes_translation_retry))
                }
            }
            is NotesTranslationState.Translated -> TranslatedNotesCard(
                translation = translation,
                locale = locale,
                onDismiss = onDismissTranslation,
                onReplace = onReplaceWithTranslation,
            )
        }
    }
}

@Composable
private fun TranslatedNotesCard(
    translation: NotesTranslationState.Translated,
    locale: Locale,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) {
            Text(
                text = stringResource(
                    R.string.notes_translated_from_to,
                    languageDisplayName(translation.sourceLanguage, locale),
                    languageDisplayName(translation.targetLanguage, locale),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            SelectionContainer(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = translation.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.notes_translation_hide))
                }
                TextButton(onClick = onReplace) {
                    Text(stringResource(R.string.notes_translation_replace))
                }
            }
        }
    }
}

/**
 * Returns the localized, capitalised display name of the language identified by the BCP-47
 * [tag] (e.g. "es" → "Spanish" in an English UI), falling back to the tag itself.
 */
internal fun languageDisplayName(tag: String, locale: Locale): String =
    Locale.forLanguageTag(tag).getDisplayLanguage(locale)
        .ifBlank { tag }
        .replaceFirstChar { it.titlecase(locale) }

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun NotesLanguageDetectedPreview() {
    WanderVaultTheme {
        NotesLanguageSection(
            state = NotesLanguageState(
                detectedLanguage = "es",
                targetLanguage = "en",
                canTranslate = true,
            ),
            onTranslate = {},
            onDismissTranslation = {},
            onReplaceWithTranslation = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesLanguageTranslatedPreview() {
    WanderVaultTheme {
        NotesLanguageSection(
            state = NotesLanguageState(
                detectedLanguage = "es",
                targetLanguage = "en",
                canTranslate = true,
                translation = NotesTranslationState.Translated(
                    text = "Visit the market on Sunday morning.",
                    sourceLanguage = "es",
                    targetLanguage = "en",
                ),
            ),
            onTranslate = {},
            onDismissTranslation = {},
            onReplaceWithTranslation = {},
        )
    }
}
