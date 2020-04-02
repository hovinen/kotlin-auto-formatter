package org.kotlin.formatter.scanning

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
import org.kotlin.formatter.nonBreakingSpaceToken
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children

class KotlinScanner {
    private var isFirstEntry = false

    fun scan(node: ASTNode): List<Token> {
        return scanInState(node, ScannerState.BLOCK)
    }

    private fun scanInState(node: ASTNode, scannerState: ScannerState): List<Token> {
        return when (node) {
            is LeafPsiElement -> scanLeaf(node, scannerState)
            else -> scanNodeWithChildren(node, scannerState)
        }
    }

    private fun scanNodeWithChildren(node: ASTNode, scannerState: ScannerState): List<Token> {
        return when (node.elementType) {
            KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY -> {
                val tokens = scanNodes(node.children().asIterable(), ScannerState.BLOCK)
                replaceTerminalForcedBreakTokenWithClosingForcedBreakToken(tokens)
            }
            KtNodeTypes.WHEN -> {
                val innerTokens = tokensForWhenOrForExpression(node)
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
                tokensForStringLiteralNode(node)
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
                val blockNodeTypes = setOf(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY)
                if (blockNodeTypes.contains(node.lastChildNode.elementType)) {
                    scanDeclarationWithBlock(node)
                } else {
                    tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
                }
            }
            KtNodeTypes.FUN -> {
                val blockNodeTypes = setOf(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY)
                val innerTokens = if (blockNodeTypes.contains(node.lastChildNode.elementType)) {
                    scanDeclarationWithBlock(node)
                } else {
                    tokensForProperty(node)
                }
                inBeginEndBlock(innerTokens, State.CODE)
            }
            KtNodeTypes.PRIMARY_CONSTRUCTOR -> {
                scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
            }
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
                val tokens = scanDotQualifiedExpression(node)
                inBeginEndBlock(tokens, State.CODE)
            }
            KtNodeTypes.BODY, KtNodeTypes.THEN -> {
                scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
            }
            KtNodeTypes.CONDITION -> {
                val innerTokens = scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
                listOf(
                    BeginToken(length = lengthOfTokens(innerTokens), state = State.CODE),
                    *innerTokens.toTypedArray(),
                    ClosingSynchronizedBreakToken,
                    EndToken
                )
            }
            KtNodeTypes.FOR -> {
                tokensForWhenOrForExpression(node)
            }
            KtNodeTypes.BINARY_EXPRESSION -> {
                tokensForBinaryExpression(node)
            }
            KtNodeTypes.PROPERTY -> {
                tokensForProperty(node)
            }
            else -> {
                tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
            }
        }
    }

    private fun tokensForProperty(node: ASTNode): List<Token> {
        val childNodes = node.children().toList()
        val indexOfAssignmentOperator = childNodes.indexOfFirst { it.elementType == KtTokens.EQ }
        if (indexOfAssignmentOperator == -1) {
            return tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
        } else {
            var lastNonWhitespaceIndex = indexOfAssignmentOperator - 1
            while (childNodes[lastNonWhitespaceIndex].elementType == KtTokens.WHITE_SPACE) {
                lastNonWhitespaceIndex--
            }
            val nodesUpToAssignmentOperator = childNodes.subList(0, lastNonWhitespaceIndex + 1)
            val tokensUpToAssignmentOperator = scanNodes(nodesUpToAssignmentOperator, ScannerState.STATEMENT)
            var firstNonWhitespaceIndex = indexOfAssignmentOperator + 1
            while (childNodes[firstNonWhitespaceIndex].elementType == KtTokens.WHITE_SPACE) {
                firstNonWhitespaceIndex++
            }
            val nodesAfterAssignmentOperator = childNodes.subList(firstNonWhitespaceIndex, childNodes.size)
            val tokensAfterAssignmentOperator = scanNodes(nodesAfterAssignmentOperator, ScannerState.STATEMENT)
            val innerTokens = listOf(
                *tokensUpToAssignmentOperator.toTypedArray(),
                nonBreakingSpaceToken(content = " "),
                LeafNodeToken("="),
                WhitespaceToken(
                    length = 1 + lengthOfTokens(tokensAfterAssignmentOperator),
                    content = " "
                ),
                *tokensAfterAssignmentOperator.toTypedArray()
            )
            return inBeginEndBlock(innerTokens, State.CODE)
        }
    }

    private fun tokensForBinaryExpression(node: ASTNode): List<Token> {
        val nodesWithoutWhitespace = node.children().filter { it.elementType != KtTokens.WHITE_SPACE }.toList()
        val secondArgumentNodes = scanNodes(listOf(nodesWithoutWhitespace[2]), ScannerState.STATEMENT)
        val innerTokens = listOf(
            *scanNodes(listOf(nodesWithoutWhitespace[0]), ScannerState.STATEMENT).toTypedArray(),
            nonBreakingSpaceToken(content = " "),
            *scanNodes(listOf(nodesWithoutWhitespace[1]), ScannerState.STATEMENT).toTypedArray(),
            WhitespaceToken(
                length = 1 + lengthOfTokensForWhitespace(secondArgumentNodes),
                content = " "
            ),
            *secondArgumentNodes.toTypedArray()
        )
        return inBeginEndBlock(innerTokens, State.CODE)
    }

    private fun tokensForStringLiteralNode(node: ASTNode): List<Token> {
        val tokens = mutableListOf<Token>()
        var lastChild: ASTNode? = null
        for (child in node.children()) {
            val childTokens = scanInState(child, ScannerState.BLOCK)
            if (child.elementType != KtTokens.OPEN_QUOTE && child.elementType != KtTokens.CLOSING_QUOTE
                && lastChild?.elementType != KtTokens.OPEN_QUOTE) {
                tokens.add(
                    WhitespaceToken(length = lengthOfTokensForWhitespace(childTokens), content = "")
                )
            }
            tokens.addAll(childTokens)
            lastChild = child
        }
        return inBeginEndBlock(tokens, stateForStringLiteral(node))
    }

    private fun stateForStringLiteral(node: ASTNode): State =
        if (node.text.startsWith("\"\"\"")) {
            State.MULTILINE_STRING_LITERAL
        } else {
            State.STRING_LITERAL
        }

    private fun scanDotQualifiedExpression(node: ASTNode): List<Token> {
        val dotExpressionTypes =
            setOf(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
        return if (dotExpressionTypes.contains(node.firstChildNode.elementType)) {
            listOf(
                *scanDotQualifiedExpression(node.firstChildNode).toTypedArray(),
                *scanNodes(node.children().toList().tail(), ScannerState.STATEMENT).toTypedArray()
            )
        } else {
            scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
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
        while (tokens[index] is EndToken) {
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

    private fun tokensForBlockNode(
        node: ASTNode,
        state: State,
        scannerState: ScannerState
    ): List<Token> = inBeginEndBlock(scanNodes(node.children().asIterable(), scannerState), state)

    private fun scanNodes(
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
            if (node.text.matches(Regex(".*\n.*\n.*", RegexOption.DOT_MATCHES_ALL))) {
                listOf(ForcedBreakToken(count = 2))
            } else {
                listOf(ForcedBreakToken(count = 1))
            }
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

    private fun lengthOfTokensForWhitespace(nextTokens: List<Token>): Int =
        when (val firstToken = nextTokens.firstOrNull()) {
            is LeafNodeToken -> firstToken.textLength
            else -> lengthOfTokens(nextTokens)
        }

    private fun lengthOfTokens(nextTokens: List<Token>): Int =
        nextTokens.map {
            when (it) {
                is WhitespaceToken -> if (it.content.isEmpty()) 0 else 1
                is SynchronizedBreakToken -> it.whitespaceLength
                is LeafNodeToken -> it.textLength
                else -> 0
            }
        }.sum()

    private val ASTNode.isAtEndOfFile: Boolean get() = treeNext == null

    private fun scanLeaf(node: LeafPsiElement, scannerState: ScannerState): List<Token> =
        when (node.elementType) {
            KtTokens.EOL_COMMENT -> {
                listOf(
                    BeginToken(node.textLength, stateBasedOnCommentContent(node.text)),
                    *tokenizeString(node.text).toTypedArray(),
                    EndToken
                )
            }
            KtTokens.BLOCK_COMMENT -> {
                listOf(
                    BeginToken(node.textLength, State.LONG_COMMENT),
                    *tokenizeNodeContentInBlockComment(node).toTypedArray(),
                    EndToken
                )
            }
            KDocTokens.TEXT -> tokenizeString(node.text.trim())
            KDocTokens.LEADING_ASTERISK -> listOf()
            KDocTokens.END -> listOf(ClosingSynchronizedBreakToken, LeafNodeToken(node.text))
            KtTokens.REGULAR_STRING_PART -> tokenizeString(node.text)
            KtTokens.RPAR -> {
                if (scannerState == ScannerState.SYNC_BREAK_LIST) {
                    listOf(ClosingSynchronizedBreakToken, LeafNodeToken(node.text))
                } else {
                    listOf(LeafNodeToken(node.text))
                }
            }
            KtTokens.DOT, KtTokens.SAFE_ACCESS -> {
                listOf(
                    SynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken(node.text)
                )
            }
            else -> listOf(LeafNodeToken(node.text))
        }

    private fun tokensForWhenOrForExpression(node: ASTNode): List<Token> {
        val childNodes = node.children().toList()
        val indexOfLeftParenthesis = childNodes.indexOfFirst { it.elementType == KtTokens.LPAR }
        val nodesUntilLeftParenthesis = childNodes.subList(0, indexOfLeftParenthesis)
        val tokensUntilLeftParenthesis = scanNodes(nodesUntilLeftParenthesis, ScannerState.STATEMENT)
        val indexOfRightParenthesis = childNodes.indexOfFirst { it.elementType == KtTokens.RPAR }
        val nodesBetweenParentheses = childNodes.subList(indexOfLeftParenthesis + 1, indexOfRightParenthesis)
        val tokensBetweenParentheses = scanNodes(nodesBetweenParentheses, ScannerState.STATEMENT)
        val nodesAfterRightParenthesis = childNodes.subList(indexOfRightParenthesis + 1, childNodes.size)
        val tokensAfterRightParenthesis = scanNodes(nodesAfterRightParenthesis, ScannerState.BLOCK)
        val innerTokens = listOf(
            *tokensUntilLeftParenthesis.toTypedArray(),
            LeafNodeToken("("),
            BeginToken(length = lengthOfTokens(tokensBetweenParentheses), state = State.CODE),
            *tokensBetweenParentheses.toTypedArray(),
            ClosingSynchronizedBreakToken,
            EndToken,
            LeafNodeToken(")"),
            *tokensAfterRightParenthesis.toTypedArray()
        )
        return inBeginEndBlock(innerTokens, State.CODE)
    }

    private fun inBeginEndBlock(innerTokens: List<Token>, state: State): List<Token> =
        listOf(
            BeginToken(length = lengthOfTokens(innerTokens), state = state),
            *innerTokens.toTypedArray(),
            EndToken
        )

    private fun hasNewlineInBlockState(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = node.textContains('\n') && scannerState == ScannerState.BLOCK

    private fun hasDoubleNewlineInKDocState(
        node: LeafPsiElement,
        scannerState: ScannerState
    ) = scannerState == ScannerState.KDOC &&
        node.textContains('\n') &&
        node.treeNext.elementType == KDocTokens.LEADING_ASTERISK &&
        node.treeNext.treeNext.text.matches(Regex(" *\n.*"))

    private fun stateBasedOnCommentContent(content: String) =
        if (content.startsWith("// TODO")) State.TODO_COMMENT else State.LINE_COMMENT

    private fun tokenizeNodeContentInBlockComment(node: ASTNode): List<Token> =
        tokenizeString(node.text.replace(Regex("\n[ ]+\\* "), "\n "))

    private fun tokenizeString(text: String): List<Token> {
        val parts = text.split(" ")
        return listOf(
            LeafNodeToken(parts.first()),
            *parts.tail().flatMap {
                listOf(
                    WhitespaceToken(it.length + 1, " "),
                    LeafNodeToken(it)
                )
            }.toTypedArray()
        )
    }

    companion object {
        private fun <T> List<T>.tail() = this.subList(1, this.size)
    }

    private enum class ScannerState {
        /** Newlines create forced breaks */
        BLOCK,

        /** Newlines are treated as ordinary whitespace */
        STATEMENT,

        /** ClosingSynchronizedBreakToken on right parenthesis */
        SYNC_BREAK_LIST,

        /** Single newlines are treated as ordinary whitespace, double newlines create forced breaks */
        KDOC
    }
}
