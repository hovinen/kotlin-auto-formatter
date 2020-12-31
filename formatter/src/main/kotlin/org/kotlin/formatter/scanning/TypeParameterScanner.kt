package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for a parameter in a type parameter list. */
internal class TypeParameterScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            oneOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            zeroOrOne {
                possibleWhitespace()
                nodeOfType(KtTokens.COLON) thenMapToTokens {
                    listOf(LeafNodeToken(" :"), WhitespaceToken(" "))
                }
                possibleWhitespace()
                oneOrMore { anyNode() } thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(nodePattern.matchSequence(node.children().asIterable()), State.CODE)
}
