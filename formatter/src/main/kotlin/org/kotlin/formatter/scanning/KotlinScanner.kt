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
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken

class KotlinScanner {
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
                WhenForExpressionScanner(this).scanWhenExpression(node)
            }
            KDocTokens.KDOC -> {
                tokensForBlockNode(node, State.LONG_COMMENT, ScannerState.KDOC)
            }
            KDocElementTypes.KDOC_TAG, KDocTokens.MARKDOWN_LINK, KDocElementTypes.KDOC_NAME -> {
                tokensForBlockNode(node, State.KDOC_DIRECTIVE, ScannerState.KDOC)
            }
            KDocElementTypes.KDOC_SECTION -> {
                KDocSectionScanner(this).scanKDocSection(node)
            }
            KtNodeTypes.STRING_TEMPLATE -> {
                StringLiteralScanner(this).tokensForStringLiteralNode(node)
            }
            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST -> {
                ParameterListScanner(this).scan(node)
            }
            KtNodeTypes.CLASS -> {
                ClassScanner(this).scanClassNode(node)
            }
            KtNodeTypes.FUN -> {
                FunctionDeclarationScanner(this, PropertyScanner(this)).tokensForFunctionDeclaration(node)
            }
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
                DotQualifiedExpressionScanner(this).scanDotQualifiedExpression(node, scannerState)
            }
            KtNodeTypes.CONDITION -> {
                ConditionScanner(this).scanCondition(node)
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
            KtFileElementType.INSTANCE, is KtScriptElementType, KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY -> {
                scanNodes(node.children().asIterable(), ScannerState.BLOCK)
            }
            KtNodeTypes.PRIMARY_CONSTRUCTOR, KtNodeTypes.BODY, KtNodeTypes.THEN -> {
                scanNodes(node.children().asIterable(), ScannerState.STATEMENT)
            }
            KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.VALUE_ARGUMENT -> {
                tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
            }
            else -> {
                tokensForBlockNode(node, State.CODE, ScannerState.STATEMENT)
            }
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
