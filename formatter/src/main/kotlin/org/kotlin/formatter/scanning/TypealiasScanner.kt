package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for `typealias` declarations. */
internal class TypealiasScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        optionalKDoc(kotlinScanner)
        possibleWhitespace()
        zeroOrOne {
            nodeOfType(KtNodeTypes.MODIFIER_LIST) andThen { nodes ->
                listOf(
                    *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray(),
                    WhitespaceToken(content = " ")
                )
            }
            whitespace()
        }
        nodeOfType(KtTokens.TYPE_ALIAS_KEYWORD) andThen { listOf(LeafNodeToken("typealias")) }
        whitespace() andThen { listOf(WhitespaceToken(content = " "))}
        nodeOfType(KtTokens.IDENTIFIER) andThen { nodes ->
            listOf(LeafNodeToken(nodes[0].text))
        }
        possibleWhitespace()
        nodeOfType(KtTokens.EQ) andThen { listOf(LeafNodeToken(" =")) }
        possibleWhitespace() andThen { listOf(WhitespaceToken(content = " ")) }
        nodeOfType(KtNodeTypes.TYPE_REFERENCE) andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
