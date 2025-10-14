package io.jacob.episodive.core.designsystem.component

import android.util.Patterns
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun HtmlTextContainer(text: String, content: @Composable (AnnotatedString) -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val htmlText = text.replace("\n", "<br>")
    val annotatedString = remember(htmlText) {
        val baseString = AnnotatedString.fromHtml(htmlString = htmlText)

        buildAnnotatedString {
            append(baseString)

            val patterns = listOf(
                Patterns.EMAIL_ADDRESS to "mailto:",
                Patterns.WEB_URL to "",
                Patterns.PHONE to "tel:"
            )

            val allMatches = patterns.flatMap { (pattern, scheme) ->
                pattern.toRegex().findAll(baseString.text).mapNotNull { match ->
                    when (scheme) {
                        "" -> { // URL 검증
                            if (!match.value.startsWith("http") && !match.value.startsWith("www")) {
                                return@mapNotNull null
                            }
                        }

                        "tel:" -> { // 전화번호 검증
                            val digitCount = match.value.replace(Regex("[^0-9]"), "").length
                            if (digitCount < 7) return@mapNotNull null
                        }
                    }
                    Triple(match.range.first, match.range.last, scheme + match.value)
                }
            }.sortedBy { it.first }

            val nonOverlappingMatches = mutableListOf<Triple<Int, Int, String>>()
            allMatches.forEach { match ->
                if (nonOverlappingMatches.none { it.second >= match.first }) {
                    nonOverlappingMatches.add(match)
                }
            }

            nonOverlappingMatches.forEach { (start, end, uri) ->
                val link = LinkAnnotation.Url(
                    url = uri,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
                addLink(link, start, end + 1)
            }
        }
    }

    content(annotatedString)
}