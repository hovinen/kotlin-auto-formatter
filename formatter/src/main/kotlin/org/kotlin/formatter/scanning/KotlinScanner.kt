package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

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
 * Volume 2 Issue 4, Oct. 1980, pp. 465-483.
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
    internal fun scanNodes(
        nodes: Iterable<ASTNode>,
        scannerState: ScannerState,
        singleNodeScanner: (ASTNode, ScannerState) -> List<Token> = this::scanSingleNode
    ): List<Token> =
        nodePattern(scannerState, singleNodeScanner).matchSequence(nodes)

    private fun nodePattern(scannerState: ScannerState, singleNodeScanner: (ASTNode, ScannerState) -> List<Token>) = nodePattern {
        zeroOrMore {
            either {
                possibleWhitespace() thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        toForcedBreak(nodes.first())
                    } else {
                        listOf()
                    }
                }
                nodeOfOneOfTypes(KtTokens.EOL_COMMENT, KtTokens.BLOCK_COMMENT) thenMapToTokens { nodes ->
                    LeafScanner().scanCommentNode(nodes.first())
                }
                possibleWhitespace() thenMapToTokens { nodes ->
                    if (nodes.isNotEmpty()) {
                        toForcedBreak(nodes.first())
                    } else {
                        listOf()
                    }
                }
            } or {
                anyNode() thenMapToTokens { nodes ->
                    singleNodeScanner(nodes.first(), scannerState)
                }
            }
        }
        end()
    }

    private fun scanSingleNode(node: ASTNode, scannerState: ScannerState): List<Token> {
        return if (node is LeafPsiElement && node.elementType == KtTokens.WHITE_SPACE) {
            whitespaceElementToTokens(node, scannerState)
        } else {
            scanInState(node, scannerState)
        }
    }

    private fun whitespaceElementToTokens(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = if (node.isAtEndOfFile || hasNewlineInBlockState(node, scannerState)) {
            toForcedBreak(node)
        } else {
            listOf(WhitespaceToken(node.text))
        }

    private val ASTNode.isAtEndOfFile: Boolean get() = treeNext == null
    
    private fun hasNewlineInBlockState(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = node.textContains('\n') && setOf(ScannerState.BLOCK, ScannerState.PACKAGE_IMPORT).contains(scannerState)

    private fun toForcedBreak(node: ASTNode) =
        listOf(ForcedBreakToken(count = if (hasDoubleNewline(node)) 2 else 1))

    private fun hasDoubleNewline(node: ASTNode): Boolean = node.text.count { it == '\n' } > 1
}
