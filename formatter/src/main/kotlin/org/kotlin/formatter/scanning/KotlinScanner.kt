package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

/**
 * Scans a tree of [ASTNode] representing the abstract syntax tree of a Kotlin source file or
 * fragment thereof to produce a sequence of [Token] representing the logical structure of the file
 * for the purpose of line breaking.
 *
 * This implementation is based loosely on the "scan" operation of the "Prettyprinting" algorithm of
 * Derek Oppen [1] with some inspiration from the
 * [Google Java formatter](https://github.com/google/google-java-format).
 *
 * [1] Oppen, Derek C. "Prettyprinting". ACM Transactions on Programming Languages and Systems,
 * Volume 2 Issue 4, Oct. 1980, pp. 465–483.
 */
class KotlinScanner {
    /**
     * Returns a list of [Token] derived from the given [ASTNode].
     *
     * Performs scanning in [ScannerState.BLOCK], which assumes that the given [ASTNode] represents
     * a larger syntactic unit such as a file.
     */
    fun scan(node: ASTNode): List<Token> {
        return scanInState(node, ScannerState.BLOCK)
    }

    /**
     * Returns a list of [Token] derived from the given [ASTNode] using the given [ScannerState].
     */
    internal fun scanInState(node: ASTNode, scannerState: ScannerState): List<Token> {
        return when (node) {
            is LeafPsiElement -> LeafScanner().scanLeaf(node)
            else -> nodeScannerForElementType(this, node.elementType).scan(node, scannerState)
        }
    }

    /**
     * Returns a list of [Token] derived from the given sequence of [ASTNode] using the given
     * [ScannerState].
     */
    internal fun scanNodes(nodes: Iterable<ASTNode>, scannerState: ScannerState): List<Token> =
        nodes.flatMap { node ->
            if (node is LeafPsiElement && node.elementType == KtTokens.WHITE_SPACE) {
                whitespaceElementToTokens(node, scannerState)
            } else {
                scanInState(node, scannerState)
            }
        }

    private fun whitespaceElementToTokens(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = if (node.isAtEndOfFile || hasNewlineInBlockState(node, scannerState)) {
            listOf(ForcedBreakToken(count = if (hasDoubleNewline(node)) 2 else 1))
        } else if (hasDoubleNewlineInKDocState(node, scannerState)) {
            listOf(ForcedBreakToken(count = 2))
        } else if (scannerState == ScannerState.KDOC) {
            if (!node.textContains('\n')) {
                listOf(WhitespaceToken(" "))
            } else {
                listOf()
            }
        } else {
            listOf(WhitespaceToken(node.text))
        }

    private fun hasDoubleNewline(node: LeafPsiElement): Boolean =
        node.text.matches(Regex(".*\n.*\n.*", RegexOption.DOT_MATCHES_ALL))

    private val ASTNode.isAtEndOfFile: Boolean get() = treeNext == null
    
    private fun hasNewlineInBlockState(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = node.textContains('\n') && setOf(ScannerState.BLOCK, ScannerState.PACKAGE_IMPORT).contains(scannerState)

    private fun hasDoubleNewlineInKDocState(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = scannerState == ScannerState.KDOC &&
        node.textContains('\n') &&
        node.treeNext.elementType == KDocTokens.LEADING_ASTERISK &&
        node.treeNext.treeNext.text.matches(Regex(" *\n.*"))
}
