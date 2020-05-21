package org.kotlin.formatter.scanning

/**
 * Mode which informs the conversion of abstract syntax tree nodes into [org.kotlin.formatter.Token]
 * in [KotlinScanner].
 *
 * Each call to [KotlinScanner.scanInState] or [KotlinScanner.scanNodes] may specify [ScannerState]
 * in which the given nodes are scanned. In particular, the type of an different abstract syntax
 * tree nodes inform the [ScannerState] of its children or a subsequence thereof.
 */
internal enum class ScannerState {
    /** Newlines are transformed into [org.kotlin.formatter.ForcedBreakToken]. */
    BLOCK,

    /** Newlines are transformed into [org.kotlin.formatter.WhitespaceToken]. */
    STATEMENT,

    /**
     * Behaves like [BLOCK], except that [org.kotlin.formatter.BeginToken] are output in state
     * [org.kotlin.formatter.State.PACKAGE_IMPORT] in order to suppress insertion of line breaks.
     */
    PACKAGE_IMPORT
}
