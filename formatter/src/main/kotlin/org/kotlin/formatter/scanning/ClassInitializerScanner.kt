package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for class initializers, i.e., blocks of the form `init {...}`. */
internal class ClassInitializerScanner(private val kotlinScaner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            possibleWhitespaceWithComment()
            nodeOfType(KtTokens.INIT_KEYWORD) thenMapToTokens {
                listOf(MarkerToken, LeafNodeToken("init "))
            }
            possibleWhitespace()
            zeroOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScaner.scanNodes(nodes, ScannerState.BLOCK)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
