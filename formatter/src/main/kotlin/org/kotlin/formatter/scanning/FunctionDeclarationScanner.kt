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
        zeroOrMore { nodeOfType(KtTokens.WHITE_SPACE) }
        zeroOrOne { nodeOfType(KtNodeTypes.BLOCK) }.andThen { nodes ->
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

    private fun scanDeclarationWithBlock(node: ASTNode): List<Token> {
        val children = node.children().toList()
        val index = indexOfTerminatingBlockIncludingPrecedingWhitespace(children)
        val childrenWithoutBlock = children.subList(0, index + 1)
        val blockChildren = children.subList(index + 1, children.size)
        val declarationParts = kotlinScanner.scanNodes(childrenWithoutBlock, ScannerState.STATEMENT)
        return listOf(
            *inBeginEndBlock(declarationParts, State.CODE).toTypedArray(),
            *kotlinScanner.scanNodes(blockChildren, ScannerState.BLOCK).toTypedArray()
        )
    }

    private fun indexOfTerminatingBlockIncludingPrecedingWhitespace(children: List<ASTNode>): Int {
        var index = children.size - 2
        while (children[index].elementType == KtTokens.WHITE_SPACE) {
            index--
        }
        return index
    }
}
