package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class FunctionDeclarationScanner(
    private val kotlinScanner: KotlinScanner,
    private val propertyScanner: PropertyScanner
): NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        val blockNodeTypes = setOf(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY)
        val innerTokens = if (blockNodeTypes.contains(node.lastChildNode.elementType)) {
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
