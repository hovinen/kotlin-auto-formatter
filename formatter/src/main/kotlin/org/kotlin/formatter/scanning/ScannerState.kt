package org.kotlin.formatter.scanning

internal enum class ScannerState {
    /** Newlines create forced breaks */
    BLOCK,

    /** Newlines are treated as ordinary whitespace */
    STATEMENT,

    /** Single newlines are treated as ordinary whitespace, double newlines create forced breaks */
    KDOC,

    /** Like BLOCK, but State.CODE should be replaced by State.PACKAGE_IMPORT */
    PACKAGE_IMPORT
}
