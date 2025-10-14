package io.jacob.episodive.core.designsystem.component

import android.util.Patterns
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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

    val annotatedString = remember(htmlText, linkColor) {
        val baseString = AnnotatedString.fromHtml(htmlString = htmlText)
        addLinksToAnnotatedString(baseString, linkColor)
    }

    content(annotatedString)
}

private fun addLinksToAnnotatedString(
    baseString: AnnotatedString,
    linkColor: Color
): AnnotatedString = buildAnnotatedString {
    append(baseString)

    val allMatches = findAllMatches(baseString.text)
    val nonOverlappingMatches = filterOverlappingMatches(allMatches)

    nonOverlappingMatches.forEach { (start, end, uri) ->
        addLink(
            LinkAnnotation.Url(
                url = uri,
                styles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                )
            ),
            start,
            end + 1
        )
    }
}

private fun findAllMatches(text: String): List<Triple<Int, Int, String>> {
    val patterns = listOf(
        Patterns.EMAIL_ADDRESS to "mailto:",
        Patterns.WEB_URL to "",
        Patterns.PHONE to "tel:"
    )

    return patterns.flatMap { (pattern, scheme) ->
        pattern.toRegex().findAll(text).mapNotNull { match ->
            if (isValidMatch(match.value, scheme)) {
                Triple(match.range.first, match.range.last, scheme + match.value)
            } else null
        }
    }.sortedBy { it.first }
}

private fun isValidMatch(value: String, scheme: String): Boolean = when (scheme) {
    "" -> value.startsWith("http") || value.startsWith("www")
    "tel:" -> value.replace(Regex("[^0-9]"), "").length >= 7
    else -> true
}

private fun filterOverlappingMatches(
    matches: List<Triple<Int, Int, String>>
): List<Triple<Int, Int, String>> {
    val result = mutableListOf<Triple<Int, Int, String>>()
    matches.forEach { match ->
        if (result.none { it.second >= match.first }) {
            result.add(match)
        }
    }
    return result
}