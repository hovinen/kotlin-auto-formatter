package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `return` expressions. */
internal class ReturnScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KtTokens.RETURN_KEYWORD) andThen { listOf(LeafNodeToken("return")) }
        possibleWhitespace()
        zeroOrMore { anyNode() } andThen { nodes ->
            if (nodes.isNotEmpty()) {
                listOf(
                    LeafNodeToken(" "),
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray()
                )
            } else {
                listOf()
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
