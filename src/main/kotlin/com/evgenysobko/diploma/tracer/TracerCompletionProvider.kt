package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.tracer.TracerCompletionUtil.LenientPrefixMatcher
import com.intellij.codeInsight.completion.AddSpaceInsertHandler
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.util.textCompletion.TextCompletionProvider

class TracerCompletionProvider : TextCompletionProvider, DumbAware {

    override fun fillCompletionVariants(
        parameters: CompletionParameters,
        prefix: String,
        initialResult: CompletionResultSet
    ) {
        var result = initialResult
        val textBeforeCaret = parameters.editor.document.text.substring(0, parameters.offset)

        val words = textBeforeCaret.split(' ', '\t').filter(String::isNotBlank)
        val normalizedText = words.joinToString(" ")

        fun isTokenSeparator(c: Char): Boolean = c.isWhitespace() || c == '#'
        var tokenIndex = normalizedText.count(::isTokenSeparator)
        if (textBeforeCaret.isNotBlank() && isTokenSeparator(textBeforeCaret.last())) {
            ++tokenIndex
        }

        when (tokenIndex) {
            0 -> {
                // We want all commands to be shown regardless of the prefix (for discoverability).
                val allCommands = setOf("clear", "reset", "trace", "untrace")
                val prefixMatcher = LenientPrefixMatcher(result.prefixMatcher, allCommands)
                result = result.withPrefixMatcher(prefixMatcher)

                result.addElement(LookupElementBuilder.create("clear"))
                result.addElement(LookupElementBuilder.create("reset"))
                result.addElement(
                    LookupElementBuilder.create("trace")
                        .withTailText(" <method>")
                        .withInsertHandler(AddSpaceInsertHandler.INSTANCE_WITH_AUTO_POPUP)
                )
                result.addElement(
                    LookupElementBuilder.create("untrace")
                        .withTailText(" <method>")
                        .withInsertHandler(AddSpaceInsertHandler.INSTANCE_WITH_AUTO_POPUP)
                )
            }
            1 -> TracerCompletionUtil.addLookupElementsForLoadedClasses(result)
        }

        result.stopHere()
    }

    override fun getPrefix(text: String, offset: Int): String {
        val separators = charArrayOf(' ', '\t', '#')
        val lastSeparatorPos = text.lastIndexOfAny(separators, offset - 1)
        return text.substring(lastSeparatorPos + 1, offset)
    }

    override fun applyPrefixMatcher(
        result: CompletionResultSet, prefix: String
    ): CompletionResultSet {
        return result.withPrefixMatcher(prefix)
    }

    override fun acceptChar(c: Char): CharFilter.Result {
        return when (c) {
            ' ', '\t', '#' -> CharFilter.Result.HIDE_LOOKUP
            else -> CharFilter.Result.ADD_TO_PREFIX
        }
    }

    override fun getAdvertisement(): String? = null
}
