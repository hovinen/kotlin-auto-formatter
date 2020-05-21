package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for function call expressions. */
internal class CallExpressionScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        zeroOrOne {
            possibleWhitespace()
            nodeOfType(KtNodeTypes.LAMBDA_ARGUMENT) thenMapToTokens { nodes ->
                listOf(nonBreakingSpaceToken())
                    .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
