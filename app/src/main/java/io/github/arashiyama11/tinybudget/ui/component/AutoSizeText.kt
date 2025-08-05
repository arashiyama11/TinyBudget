package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.takeOrElse
import androidx.wear.compose.material.LocalContentAlpha

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    autoSize: TextAutoSize = TextAutoSize.StepBased(),
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val resolvedColor = color
        .takeOrElse { style.color.takeOrElse { LocalContentColor.current.copy(alpha = LocalContentAlpha.current) } }

    val mergedStyle = style.copy(
        color = resolvedColor,
        textAlign = textAlign ?: style.textAlign
    )

    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        autoSize = autoSize
    )
}

@Composable
fun AutoSizePreferSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    autoSize: TextAutoSize = TextAutoSize.StepBased(),
    fallbackMaxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
) {
    val baseFontSize = style.fontSize.takeOrElse {
        MaterialTheme.typography.bodyLarge.fontSize
    }

    var useMultiLine by remember(text) { mutableStateOf(false) }

    AutoSizeText(
        text = text,
        modifier = modifier,
        autoSize = autoSize,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = if (useMultiLine) fallbackMaxLines else 1,
        style = style,
        onTextLayout = { result ->
            val currentSize = result.layoutInput.style.fontSize
            val shrunk = currentSize < baseFontSize
            val overflowed = result.didOverflowWidth || result.lineCount > 1

            if ((shrunk || overflowed) && !useMultiLine) {
                useMultiLine = true
            } else if (!shrunk && !overflowed && useMultiLine) {
                useMultiLine = false
            }
        }
    )
}
