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
            nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    .plus(WhitespaceToken(content = " "))
            }
            whitespace()
        }
        nodeOfType(KtTokens.TYPE_ALIAS_KEYWORD) thenMapToTokens { listOf(LeafNodeToken("typealias")) }
        whitespace() thenMapToTokens { listOf(WhitespaceToken(content = " "))}
        nodeOfType(KtTokens.IDENTIFIER) thenMapToTokens { nodes ->
            listOf(LeafNodeToken(nodes[0].text))
        }
        possibleWhitespace()
        nodeOfType(KtTokens.EQ) thenMapToTokens { listOf(LeafNodeToken(" =")) }
        possibleWhitespace() thenMapToTokens { listOf(WhitespaceToken(content = " ")) }
        nodeOfType(KtNodeTypes.TYPE_REFERENCE) thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
