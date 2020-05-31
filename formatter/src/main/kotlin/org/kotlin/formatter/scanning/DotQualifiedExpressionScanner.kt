package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.emptyBreakPoint
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for member access and safe member access expressions. */
internal class DotQualifiedExpressionScanner(private val kotlinScanner: KotlinScanner) :
    NodeScanner {
        override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
            inBeginEndBlock(
                dotQualifiedExpressionPattern(true).matchSequence(node.children().asIterable()),
                stateForDotQualifiedExpression(scannerState)
            )

        private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
            if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE

        private fun scanInner(node: ASTNode): List<Token> =
            dotQualifiedExpressionPattern(false).matchSequence(node.children().asIterable())

        private fun dotQualifiedExpressionPattern(isOutermost: Boolean) =
            nodePattern {
                either {
                    multilineStringTemplate() thenMapToTokens { nodes ->
                        listOf(
                            LeafNodeToken("\"\"\""),
                            BeginToken(State.CODE),
                            SynchronizedBreakToken(whitespaceLength = 0)
                        ).plus(stringTemplateToTokens(nodes.first()))
                            .plus(ClosingSynchronizedBreakToken(whitespaceLength = 0))
                            .plus(EndToken)
                            .plus(LeafNodeToken("\"\"\".trimIndent()"))
                    }
                    possibleWhitespace()
                    nodeOfType(KtTokens.DOT)
                    possibleWhitespace()
                    trimIndentCall()
                } or {
                    either {
                        nodeOfOneOfTypes(
                            KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
                            KtNodeTypes.SAFE_ACCESS_EXPRESSION
                        ) thenMapToTokens { nodes -> scanInner(nodes.first()) }
                        possibleWhitespace()
                        nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) thenMapToTokens
                            { nodes ->
                                listOf(
                                    SynchronizedBreakToken(whitespaceLength = 0),
                                    BeginToken(State.CODE),
                                    LeafNodeToken(nodes.first().text)
                                )
                            }
                    } or {
                        anyNode() thenMapToTokens { nodes ->
                            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                        }
                        possibleWhitespace()
                        nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS) thenMapToTokens
                            { nodes ->
                                val tokens =
                                    listOf(
                                        BeginToken(State.CODE),
                                        LeafNodeToken(nodes.first().text)
                                    )
                                if (isOutermost) tokens else listOf(emptyBreakPoint()).plus(tokens)
                            }
                    }
                    possibleWhitespace()
                    oneOrMore { anyNode() } thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(EndToken)
                    }
                }
                end()
            }

        private fun NodePatternBuilder.multilineStringTemplate(): NodePatternBuilder =
            nodeMatching {
                it.elementType == KtNodeTypes.STRING_TEMPLATE && it.firstChildNode.text == "\"\"" +
                    "\""
            }

        private fun NodePatternBuilder.trimIndentCall(): NodePatternBuilder =
            nodeMatching {
                it.elementType == KtNodeTypes.CALL_EXPRESSION &&
                    it.firstChildNode.text == "trimIndent"
            }

        private fun stringTemplateToTokens(stringTemplateNode: ASTNode): List<Token> {
            val templateContent =
                stringTemplateNode.text.removePrefix("\"\"\"").removeSuffix("\"\"\"").trimIndent()
            val lines = templateContent.split("\n")
            val result = mutableListOf<Token>()
            result.add(LeafNodeToken(lines.first()))
            for (line in lines.tail()) {
                result.add(ForcedBreakToken(count = 1))
                result.add(LeafNodeToken(line))
            }
            return result
        }

        private fun <T> List<T>.tail(): List<T> = subList(1, size)
    }
