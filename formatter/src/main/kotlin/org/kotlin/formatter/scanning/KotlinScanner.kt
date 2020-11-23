package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
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
 * The parameter [importPolicy] determines whether imports are retained or removed in the output.
 *
 * [1] Oppen, Derek C. "Prettyprinting". ACM Transactions on Programming Languages and Systems,
 * Volume 2 Issue 4, Oct. 1980, pp. 465-483.
 */
class KotlinScanner(private val importPolicy: (String, String) -> Boolean) {
    private val nodeScannerProvider: Lazy<NodeScannerProvider> =
        lazy { NodeScannerProvider(this, importPolicy) }

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
            else ->
                nodeScannerProvider.value.nodeScannerForElementType(node.elementType)
                    .scan(node, scannerState)
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
    ): List<Token> = nodePattern(scannerState, singleNodeScanner).matchSequence(nodes)

    private fun nodePattern(
        scannerState: ScannerState,
        singleNodeScanner: (ASTNode, ScannerState) -> List<Token>
    ) =
        nodePattern {
            zeroOrMore {
                either { commentWithPossibleWhitespace(ignoreTrailingWhitespace = false) } or {
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

    private fun whitespaceElementToTokens(node: LeafPsiElement, scannerState: ScannerState):
        List<Token> =
            if (node.isAtEndOfFile || hasNewlineInBlockState(node, scannerState)) {
                toForcedBreak(node)
            } else if (hasNewlineInPackageImportState(node, scannerState)) {
                listOf(ForcedBreakToken(count = 1))
            } else {
                listOf(WhitespaceToken(node.text))
            }

    private val ASTNode.isAtEndOfFile: Boolean
        get() = treeNext == null

    private fun hasNewlineInBlockState(node: LeafPsiElement, scannerState: ScannerState) =
        node.textContains('\n') && scannerState == ScannerState.BLOCK

    private fun hasNewlineInPackageImportState(node: LeafPsiElement, scannerState: ScannerState) =
        node.textContains('\n') && scannerState == ScannerState.PACKAGE_IMPORT
}

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching whitespace, a comment surrounded by
 * whitespace, or the empty sequence.
 *
 * The output comment is surrounded by forced line breaks if it is an end of line comment or a block
 * comment with more than one line. A block comment in one line is followed by a simple whitespace.
 *
 * Pass the flag [ignoreTrailingWhitespace] to ignore whitespace after the comment. Otherwise
 * newlines after EOL comments are turned into [ForcedBreakToken].
 */
fun NodePatternBuilder.possibleWhitespaceWithComment(ignoreTrailingWhitespace: Boolean = false):
    NodePatternBuilder =
        either { commentWithPossibleWhitespace(ignoreTrailingWhitespace) } or {
            possibleWhitespace()
        }

private fun NodePatternBuilder.commentWithPossibleWhitespace(ignoreTrailingWhitespace: Boolean) {
    either {
        possibleWhitespaceOutputToToken()
        nodeOfType(KtTokens.BLOCK_COMMENT) thenMapToTokens { nodes ->
            inBeginEndBlock(LeafScanner().scanCommentNode(nodes.first()), State.CODE)
        }
        possibleWhitespaceOutputToToken()
    } or {
        possibleWhitespace() thenMapToTokens { nodes ->
            if (nodes.isNotEmpty()) {
                if (nodes.first().textContains('\n')) {
                    toForcedBreak(nodes.first())
                } else {
                    listOf(WhitespaceToken(" "))
                }
            } else {
                listOf()
            }
        }
        zeroOrMore {
            singleComment()
            possibleWhitespace() thenMapToTokens { nodes ->
                if (nodes.isNotEmpty()) {
                    toForcedBreak(nodes.first())
                } else {
                    listOf()
                }
            }
        }
        singleComment()
        possibleWhitespace() thenMapToTokens { nodes ->
            if (nodes.isNotEmpty() && !ignoreTrailingWhitespace) {
                toForcedBreak(nodes.first())
            } else {
                listOf()
            }
        }
    }
}

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching a sequence of comments.
 *
 * The comments are output as closely as possible to the original formatting.
 */
fun NodePatternBuilder.comment(): NodePatternBuilder {
    zeroOrMore {
        singleComment()
        possibleWhitespaceOutputToToken()
    }
    singleComment()
    return this
}

private fun NodePatternBuilder.singleComment() {
    either {
        nodeOfType(KtTokens.EOL_COMMENT) thenMapToTokens { nodes ->
            LeafScanner().scanCommentNode(nodes.first())
        }
    } or {
        nodeOfType(KtTokens.BLOCK_COMMENT) thenMapToTokens { nodes ->
            inBeginEndBlock(LeafScanner().scanCommentNode(nodes.first()), State.CODE)
        }
    }
}

/**
 * Matches optional whitespace and, if it is present, converts it into a suitable [Token].
 *
 * Whitespace containing a newline is converted into a single line [ForcedBreakToken]. Any other
 * whitespace is converted into an equivalent [WhitespaceToken].
 */
fun NodePatternBuilder.possibleWhitespaceOutputToToken(): NodePatternBuilder {
    possibleWhitespace() thenMapToTokens { nodes ->
        if (nodes.isNotEmpty()) {
            if (nodes.first().textContains('\n')) {
                listOf(ForcedBreakToken(count = 1))
            } else {
                listOf(WhitespaceToken(nodes.first().text))
            }
        } else {
            listOf()
        }
    }
    return this
}

private fun toForcedBreak(node: ASTNode) =
    listOf(ForcedBreakToken(count = if (hasDoubleNewline(node)) 2 else 1))

private fun hasDoubleNewline(node: ASTNode): Boolean = node.text.count { it == '\n' } > 1
