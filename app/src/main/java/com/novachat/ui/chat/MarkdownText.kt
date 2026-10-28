package com.novachat.ui.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal markdown renderer (no third-party library).
 * Supports #/##/### headings, bold, italic, inline code, "- "/"1. " lists
 * and fenced code blocks with a language label + copy button.
 * Tolerates partial streaming input: an unclosed fence renders as if closed,
 * unfinished markers stay literal. Re-parse on every delta via remember(text).
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val bodyStyle = MaterialTheme.typography.bodyMedium
    Column(modifier = modifier.fillMaxWidth()) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inline(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                is MdBlock.Paragraph -> Text(
                    text = inline(block.text),
                    style = bodyStyle,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                is MdBlock.Bullets -> Column(Modifier.padding(bottom = 6.dp)) {
                    for (item in block.items) {
                        Row(Modifier.padding(bottom = 2.dp)) {
                            Text("•  ", style = bodyStyle)
                            Text(inline(item), style = bodyStyle)
                        }
                    }
                }
                is MdBlock.Numbered -> Column(Modifier.padding(bottom = 6.dp)) {
                    for ((i, item) in block.items.withIndex()) {
                        Row(Modifier.padding(bottom = 2.dp)) {
                            Text("${i + 1}.  ", style = bodyStyle)
                            Text(inline(item), style = bodyStyle)
                        }
                    }
                }
                is MdBlock.Code -> CodeBlock(block)
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Numbered(val items: List<String>) : MdBlock
    data class Code(val language: String, val code: String) : MdBlock
}

private fun parseMarkdown(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = text.split("\n")
    var i = 0
    val para = StringBuilder()
    val bullets = mutableListOf<String>()
    val numbered = mutableListOf<String>()

    fun flushPara() {
        if (para.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(para.toString().trim()))
            para.clear()
        }
    }
    fun flushLists() {
        if (bullets.isNotEmpty()) {
            blocks.add(MdBlock.Bullets(bullets.toList())); bullets.clear()
        }
        if (numbered.isNotEmpty()) {
            blocks.add(MdBlock.Numbered(numbered.toList())); numbered.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            flushPara(); flushLists()
            val lang = trimmed.removePrefix("```").trim()
            val code = StringBuilder()
            i++
            var closed = false
            while (i < lines.size) {
                if (lines[i].trim().startsWith("```")) { closed = true; break }
                code.appendLine(lines[i])
                i++
            }
            // Unclosed fence renders as if closed (streaming tolerance).
            blocks.add(MdBlock.Code(lang, code.toString().trimEnd('\n')))
            if (closed) i++ else i = lines.size
            continue
        }
        val heading = Regex("^(#{1,3})\\s+(.*)$").matchEntire(trimmed)
        if (heading != null) {
            flushPara(); flushLists()
            blocks.add(MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2]))
            i++
            continue
        }
        val bullet = Regex("^[-*]\\s+(.*)$").matchEntire(trimmed)
        if (bullet != null) {
            flushPara()
            if (numbered.isNotEmpty()) flushLists()
            bullets.add(bullet.groupValues[1])
            i++
            continue
        }
        val num = Regex("^\\d+[.)]\\s+(.*)$").matchEntire(trimmed)
        if (num != null) {
            flushPara()
            if (bullets.isNotEmpty()) flushLists()
            numbered.add(num.groupValues[1])
            i++
            continue
        }
        if (trimmed.isEmpty()) {
            flushPara(); flushLists()
            i++
            continue
        }
        if (bullets.isNotEmpty() || numbered.isNotEmpty()) flushLists()
        if (para.isNotEmpty()) para.append("\n")
        para.append(line.trimEnd())
        i++
    }
    flushPara(); flushLists()
    return blocks
}

private val InlinePattern = Regex("(`[^`\\n]+`)|(\\*\\*.+?\\*\\*)|(\\*[^*\\n]+\\*)")

private fun inline(raw: String): AnnotatedString {
    return buildAnnotatedString {
        var pos = 0
        for (m in InlinePattern.findAll(raw)) {
            if (m.range.first > pos) append(raw.substring(pos, m.range.first))
            val token = m.value
            when {
                token.startsWith("`") -> withStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                ) { append(token.substring(1, token.length - 1)) }
                token.startsWith("**") -> withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ) { append(token.substring(2, token.length - 2)) }
                else -> withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic)
                ) { append(token.substring(1, token.length - 1)) }
            }
            pos = m.range.last + 1
        }
        if (pos < raw.length) append(raw.substring(pos))
    }
}

@Composable
private fun CodeBlock(block: MdBlock.Code) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(block.code) { mutableStateOf(false) }
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp)
            ) {
                Text(
                    text = block.language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(block.code))
                    copied = true
                }) {
                    Text(if (copied) "Copied" else "Copy")
                }
            }
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            )
        }
    }
}
