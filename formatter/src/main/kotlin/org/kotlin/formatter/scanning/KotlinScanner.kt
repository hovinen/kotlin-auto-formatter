package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtScriptElementType
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

class KotlinScanner {
    private var isFirstEntry = false

    fun scan(node: ASTNode): List<Token> {
        return scanInState(node, ScannerState.BLOCK)
    }

    internal fun scanInState(node: ASTNode, scannerState: ScannerState): List<Token> {
        return when (node) {
            is LeafPsiElement -> LeafScanner().scanLeaf(node, scannerState)
            else -> scanNodeWithChildren(node, scannerState)
        }
    }

    private fun scanNodeWithChildren(node: ASTNode, scannerState: ScannerState): List<Token> {
        return when (node.elementType) {
            KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY -> {
                BlockScanner(this).scanBlock(node)
            }
            KtNodeTypes.WHEN -> {
                val innerTokens = WhenForExpressionScanner(this).tokensForWhenOrForExpression(node)
                val tokens = inBeginEndBlock(innerTokens, State.CODE)
                replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(tokens)
            }
            KDocTokens.KDOC -> {
                tokensForBlockNode(node, State.LONG_COMMENT, ScannerState.KDOC)
            }
            KDocElementTypes.KDOC_SECTION -> {
                val innerTokens = tokensForBlockNode(node, State.LONG_COMMENT, ScannerState.KDOC)
                listOf(
                    WhitespaceToken(length = lengthOfTokensForWhitespace(innerTokens), content = " "),
                    *innerTokens.toTypedArray(),
                    WhitespaceToken(length = 1, content = " ")
                )
            }
            KDocElementTypes.KDOC_TAG, KDocTokens.MARKDOWN_LINK, KDocElementTypes.KDOC_NAME -> {
                tokensForBlockNode(node, State.KDOC_DIRECTIVE, ScannerState.KDOC)
            }
            KtNodeTypes.STRING_TEMPLATE -> {
                StringLiteralScanner(this).tokensForStringLiteralNode(node)
            }
            KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY -> {
                scanNodes(node.children().asIterable(), ScannerState.BLOCK)
            }
            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST -> {
                isFirstEntry = true
                scanNodes(node.children().asIterable(), ScannerState.SYNC_BREAK_LIST)
            }
            KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.VALUE_ARGUMENT -> {
                val childTokens = tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
                if (scannerState == ScannerState.SYNC_BREAK_LIST) {
                    val breakToken =
                        SynchronizedBreakToken(whitespaceLength = if (isFirstEntry) 0 else 1)
                    isFirstEntry = false
                    listOf(breakToken, *childTokens.toTypedArray())
                } else {
                    childTokens
                }
            }
            KtNodeTypes.CLASS -> {
                ClassScanner(this).scanClassNode(node)
            }
            KtNodeTypes.FUN -> {
                val blockNodeTypes = setOf(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY)
                val innerTokens = if (blockNodeTypes.contains(node.lastChildNode.elementType)) {
                    scanDeclarationWithBlock(node)
                } else {
                    PropertyScanner(this).tokensForProperty(node)
                }
                inBeginEndBlock(innerTokens, State.CODE)
            }
            KtNodeTypes.PRIMARY_CONSTRUCTOR -> {
                scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
            }
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
                val tokens = DotQualifiedExpressionScanner(this).scanDotQualifiedExpression(node)
                inBeginEndBlock(tokens, stateForDotQualifiedExpression(scannerState))
            }
            KtNodeTypes.BODY, KtNodeTypes.THEN -> {
                scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
            }
            KtNodeTypes.CONDITION -> {
                val innerTokens = scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
                listOf(
                    BeginToken(length = lengthOfTokens(innerTokens), state = State.CODE),
                    *innerTokens.toTypedArray(),
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    EndToken
                )
            }
            KtNodeTypes.FOR -> {
                WhenForExpressionScanner(this).tokensForWhenOrForExpression(node)
            }
            KtNodeTypes.BINARY_EXPRESSION -> {
                BinaryExpressionScanner(this).tokensForBinaryExpression(node)
            }
            KtNodeTypes.PROPERTY -> {
                PropertyScanner(this).tokensForProperty(node)
            }
            KtNodeTypes.PACKAGE_DIRECTIVE,
            KtNodeTypes.IMPORT_LIST,
            KtNodeTypes.IMPORT_DIRECTIVE,
            KtNodeTypes.IMPORT_ALIAS -> {
                tokensForBlockNode(node, State.PACKAGE_IMPORT, ScannerState.PACKAGE_IMPORT)
            }
            KtFileElementType.INSTANCE, is KtScriptElementType -> {
                scanNodes(node.children().asIterable(), ScannerState.BLOCK)
            }
            else -> {
                tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
            }
        }
    }

    private fun scanDeclarationWithBlock(node: ASTNode): List<Token> {
        val children = node.children().toList()
        val index = indexOfTerminatingBlockIncludingPrecedingWhitespace(children)
        val childrenWithoutBlock = children.subList(0, index + 1)
        val blockChildren = children.subList(index + 1, children.size)
        val declarationParts = scanNodes(childrenWithoutBlock, ScannerState.STATEMENT)
        return listOf(
            *inBeginEndBlock(declarationParts, State.CODE).toTypedArray(),
            *scanNodes(blockChildren, ScannerState.BLOCK).toTypedArray()
        )
    }

    private fun indexOfTerminatingBlockIncludingPrecedingWhitespace(children: List<ASTNode>): Int {
        var index = children.size - 2
        while (children[index].elementType == KtTokens.WHITE_SPACE) {
            index--
        }
        return index
    }

    private fun replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(
        tokens: List<Token>
    ): List<Token> {
        var index = tokens.size - 1
        while (index > 0 && tokens[index] is EndToken) {
            index--
        }
        return if (index > 0 && tokens[index - 1] is ForcedBreakToken) {
            listOf(
                *tokens.subList(0, index - 1).toTypedArray(),
                ClosingForcedBreakToken,
                *tokens.subList(index, tokens.size).toTypedArray()
            )
        } else {
            tokens
        }
    }

    internal fun tokensForBlockNode(
        node: ASTNode,
        state: State,
        scannerState: ScannerState
    ): List<Token> = inBeginEndBlock(scanNodes(node.children().asIterable(), scannerState), state)

    internal fun scanNodes(
        nodes: Iterable<ASTNode>,
        scannerState: ScannerState
    ): List<Token> {
        val childIterator = nodes.iterator()
        val tokens = mutableListOf<Token>()
        while (childIterator.hasNext()) {
            val child = childIterator.next()
            if (child is LeafPsiElement && child.elementType == KtTokens.WHITE_SPACE) {
                val nextChildTokens =
                    if (childIterator.hasNext()) {
                        scanInState(childIterator.next(), scannerState)
                    } else {
                        listOf()
                    }
                tokens.addAll(whitespaceElementToTokens(child, scannerState, nextChildTokens))
                tokens.addAll(nextChildTokens)
            } else {
                tokens.addAll(scanInState(child, scannerState))
            }
        }
        return tokens
    }

    private fun whitespaceElementToTokens(
        node: LeafPsiElement,
        scannerState: ScannerState,
        nextTokens: List<Token>
    ) = if (scannerState == ScannerState.SYNC_BREAK_LIST) {
            listOf()
        } else if (node.isAtEndOfFile || hasNewlineInBlockState(node, scannerState)) {
            listOf(ForcedBreakToken(count = if (hasDoubleNewline(node)) 2 else 1))
        } else if (hasDoubleNewlineInKDocState(node, scannerState)) {
            listOf(ForcedBreakToken(count = 2))
        } else if (scannerState == ScannerState.KDOC) {
            if (!node.textContains('\n')) {
                listOf(WhitespaceToken(1 + lengthOfTokensForWhitespace(nextTokens), " "))
            } else {
                listOf()
            }
        } else {
            listOf(WhitespaceToken(1 + lengthOfTokensForWhitespace(nextTokens), node.text))
        }

    private fun hasDoubleNewline(node: LeafPsiElement): Boolean =
        node.text.matches(Regex(".*\n.*\n.*", RegexOption.DOT_MATCHES_ALL))

    private fun lengthOfTokensForWhitespace(nextTokens: List<Token>): Int =
        when (val firstToken = nextTokens.firstOrNull()) {
            is LeafNodeToken -> firstToken.textLength
            else -> lengthOfTokens(nextTokens)
        }

    private val ASTNode.isAtEndOfFile: Boolean get() = treeNext == null

    private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
        if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE

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
