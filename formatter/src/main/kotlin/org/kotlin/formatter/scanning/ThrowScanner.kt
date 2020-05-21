package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `throw` expressions. */
internal class ThrowScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KtTokens.THROW_KEYWORD)
        whitespace()
        oneOrMore { anyNode() } thenMapToTokens { nodes ->
            listOf(LeafNodeToken("throw "))
                .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
