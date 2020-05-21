package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for binary expressions. */
internal class BinaryExpressionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val expressionPattern = nodePattern {
        anyNode() andThen { firstNode ->
            kotlinScanner.scanNodes(firstNode, ScannerState.STATEMENT)
                .plus(nonBreakingSpaceToken())
        }
        possibleWhitespace()
        nodeOfType(KtNodeTypes.OPERATION_REFERENCE) andThen { operator ->
            kotlinScanner.scanNodes(operator, ScannerState.STATEMENT)
        }
        possibleWhitespace()
        anyNode() andThen { nodes ->
            listOf(WhitespaceToken(" "))
                .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(expressionPattern.matchSequence(node.children().toList()), State.CODE)
}
