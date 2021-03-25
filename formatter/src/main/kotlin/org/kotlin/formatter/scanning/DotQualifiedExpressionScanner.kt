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
            outermostDotQualifiedExpressionPattern.matchSequence(node.children().asIterable()),
            stateForDotQualifiedExpression(scannerState)
        )

    private fun stateForDotQualifiedExpression(scannerState: ScannerState) =
        if (scannerState == ScannerState.PACKAGE_IMPORT) State.PACKAGE_IMPORT else State.CODE

    private fun scanInner(node: ASTNode): List<Token> =
        innerDotQualifiedExpressionPattern.matchSequence(node.children().asIterable())

    private val outermostDotQualifiedExpressionPattern =
        nodePattern {
            either { stringDotQualifiedExpression() } or {
                either { nestedDotQualifiedExpression() } or {
                    either { methodCallOnReferenceExpression() } or {
                        arbitraryDotQualifiedExpression()
                    }
                }
            }
            end()
        }

    private fun NodePatternBuilder.methodCallOnReferenceExpression() {
        nodeOfOneOfTypes(
            KtNodeTypes.REFERENCE_EXPRESSION,
            KtNodeTypes.THIS_EXPRESSION,
            KtNodeTypes.SUPER_EXPRESSION
        ) thenMapToTokens { nodes -> kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT) }
        possibleWhitespace()
        dotToken() thenMapToTokens { nodes ->
            listOf(emptyBreakPoint(), LeafNodeToken(nodes.first().text))
        }
        possibleWhitespace()
        nodeOfType(KtNodeTypes.CALL_EXPRESSION) thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
    }

    private val innerDotQualifiedExpressionPattern =
        nodePattern {
            either { stringDotQualifiedExpression() } or {
                either { nestedDotQualifiedExpression() } or { arbitraryDotQualifiedExpression() }
            }
            end()
        }

    private fun NodePatternBuilder.nestedDotQualifiedExpression() {
        nodeOfOneOfTypes(
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION,
            KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) thenMapToTokens { nodes -> scanInner(nodes.first()) }
        possibleWhitespaceWithComment()
        dotToken() thenMapToTokens { nodes ->
            listOf(
                SynchronizedBreakToken(whitespaceLength = 0),
                BeginToken(State.CODE),
                LeafNodeToken(nodes.first().text)
            )
        }
        possibleWhitespace()
        oneOrMore { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(EndToken)
        }
    }

    private fun NodePatternBuilder.stringDotQualifiedExpression() {
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
            singleLineStringTemplate() thenMapToTokens { nodes ->
                listOf(BeginToken(State.MULTILINE_STRING_LITERAL))
                    .plus(stringTemplateToTokens(nodes.first()))
                    .plus(EndToken)
                    .plus(emptyBreakPoint())
                    .plus(LeafNodeToken("."))
            }
            possibleWhitespace()
            nodeOfType(KtTokens.DOT)
            possibleWhitespace()
            anyNode() thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
        }
    }

    private fun NodePatternBuilder.arbitraryDotQualifiedExpression() {
        anyNode() thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        possibleWhitespaceWithComment()
        dotToken() thenMapToTokens { nodes ->
            listOf(emptyBreakPoint(), BeginToken(State.CODE), LeafNodeToken(nodes.first().text))
        }
        possibleWhitespace()
        oneOrMore { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(EndToken)
        }
    }

    private fun NodePatternBuilder.dotToken(): NodePatternBuilder =
        nodeOfOneOfTypes(KtTokens.DOT, KtTokens.SAFE_ACCESS)

    private fun NodePatternBuilder.multilineStringTemplate(): NodePatternBuilder =
        nodeMatching {
            it.elementType == KtNodeTypes.STRING_TEMPLATE && it.firstChildNode.text == "\"\"\""
        }

    private fun NodePatternBuilder.singleLineStringTemplate(): NodePatternBuilder =
        nodeMatching {
            it.elementType == KtNodeTypes.STRING_TEMPLATE && it.firstChildNode.text == "\""
        }

    private fun NodePatternBuilder.trimIndentCall(): NodePatternBuilder =
        nodeMatching {
            it.elementType == KtNodeTypes.CALL_EXPRESSION && it.firstChildNode.text == "trimIndent"
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
