package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class FunctionDeclarationScanner(
    private val kotlinScanner: KotlinScanner,
    private val propertyScanner: PropertyScanner
): NodeScanner {
    private val blockPattern = nodePattern {
        oneOrMore { anyNode() }.andThen { nodes ->
            inBeginEndBlock(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT), State.CODE)
        }
        possibleWhitespace()
        zeroOrOne { nodeOfType(KtNodeTypes.BLOCK) } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val innerTokens =
            if (node.lastChildNode.elementType == KtNodeTypes.BLOCK) {
                scanDeclarationWithBlock(node)
            } else {
                propertyScanner.scan(node, scannerState)
            }
        return inBeginEndBlock(innerTokens, State.CODE)
    }

    private fun scanDeclarationWithBlock(node: ASTNode): List<Token> =
        blockPattern.matchSequence(node.children().asIterable())
}
