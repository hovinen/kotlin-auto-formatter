package org.kotlin.formatter

enum class State {
    CODE,
    STRING_LITERAL,
    MULTILINE_STRING_LITERAL,
    LINE_COMMENT,
    TODO_COMMENT,
    LONG_COMMENT,
    KDOC_DIRECTIVE
}
