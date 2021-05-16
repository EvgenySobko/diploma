package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.tracer.TraceOption.COUNT_AND_WALL_TIME
import com.evgenysobko.diploma.tracer.TraceOption.COUNT_ONLY

/** A tracer CLI command */
sealed class TracerCommand {
    /** Unrecognized command. */
    object Unknown: TracerCommand()

    /** Zero out all tracepoint data, but keep call tree. */
    object Clear: TracerCommand()

    /** Zero out all tracepoint data and reset the call tree. */
    object Reset: TracerCommand()

    /** Trace or untrace a set of methods. */
    data class Trace(
        val enable: Boolean,
        val traceOption: TraceOption?,
        val target: TraceTarget?
    ): TracerCommand()

    /**
     * Checks for syntax errors. If no error exists, then all fields within this structure are
     * non-null values.
     */
    val errors: List<String>
        get() = when (this) {
            Unknown -> listOf("Unknown command")
            is Trace -> when {
                traceOption == null -> listOf("Expected a trace option")
                target == null -> listOf("Expected a trace target")
                else -> target.errors
            }
            else -> emptyList()
        }
}

/** Represents what to trace */
enum class TraceOption {
    COUNT_AND_WALL_TIME,
    COUNT_ONLY;
}

/** A set of methods that the tracer will trace. */
sealed class TraceTarget {
    /** Trace everything. */
    object All: TraceTarget()

    /** Trace a specific method. */
    data class Method(
        val className: String,
        val methodName: String?,
        val parameterIndexes: List<Int>? = emptyList()
    ): TraceTarget()

    val errors: List<String>
        get() = when (this) {
            is Method -> when {
                methodName.isNullOrBlank() -> listOf("Expected a method name")
                parameterIndexes == null -> listOf("Invalid parameter index syntax")
                else -> emptyList()
            }
            else -> emptyList()
        }
}

private sealed class Token
private object UnrecognizedToken: Token()
private data class Identifier(val text: CharSequence): Token() {
    val textString: String get() = text.toString()
}
private data class IntLiteral(val value: Int): Token()
private object EndOfLine: Token()
private object ClearKeyword: Token()
private object ResetKeyword: Token()
private object TraceKeyword: Token()
private object UntraceKeyword: Token()
private object AllKeyword: Token()
private object CountKeyword: Token()
private object WallTimeKeyword: Token()
private object HashSymbol: Token()
private object CommaSymbol: Token()
private object OpenBracketSymbol: Token()
private object CloseBracketSymbol: Token()

private fun tokenize(text: CharSequence): List<Token> {
    fun Char.isIdentifierChar() =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '.' || this == '-' ||
                this == '_' || this == '$' || this == '*' || this == '<' || this == '>'

    val tokens = mutableListOf<Token>()
    var offset = 0

    while (true) {
        while (offset < text.length && text[offset].isWhitespace()) {
            offset++
        }

        if (offset >= text.length) {
            break
        }

        when (text[offset]) {
            in 'A'..'Z', in 'a'..'z', '.', '-', '_', '$', '*', '<', '>' -> {
                val startOffset = offset
                while (offset < text.length && text[offset].isIdentifierChar()) {
                    offset++
                }

                when (val identifierText = text.subSequence(startOffset, offset)) {
                    "clear" -> tokens.add(ClearKeyword)
                    "reset" -> tokens.add(ResetKeyword)
                    "trace" -> tokens.add(TraceKeyword)
                    "untrace" -> tokens.add(UntraceKeyword)
                    "all" -> tokens.add(AllKeyword)
                    "count" -> tokens.add(CountKeyword)
                    "wall-time" -> tokens.add(WallTimeKeyword)
                    else -> tokens.add(Identifier(identifierText))
                }
            }
            in '0'..'9' -> {
                var value = 0
                while (offset < text.length && text[offset] in '0'..'9') {
                    value = (value * 10) + (text[offset].toInt() - '0'.toInt())
                    offset++
                }
                tokens.add(IntLiteral(value))
            }
            '#' -> { tokens.add(HashSymbol); offset++ }
            ',' -> { tokens.add(CommaSymbol); offset++ }
            '[' -> { tokens.add(OpenBracketSymbol); offset++ }
            ']' -> { tokens.add(CloseBracketSymbol); offset++ }
            else -> { tokens.add(UnrecognizedToken); offset++ }
        }
    }

    tokens.add(EndOfLine)
    return tokens
}
