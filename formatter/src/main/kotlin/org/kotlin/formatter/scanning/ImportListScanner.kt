package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock

/**
 * A [NodeScanner] for import lists.
 *
 * Sorts the imports in ASCII order as per the
 * [Google Kotlin style guide](https://developer.android.com/kotlin/style-guide#import_statements)
 * and removes extraneous whitespaces from the list.
 *
 * *Warning:* This removes all non-import tokens, including comments, from the import list.
 */
internal class ImportListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(
            kotlinScanner.scanNodes(
                sortImports(node.children().asIterable()),
                ScannerState.PACKAGE_IMPORT
            ),
            State.PACKAGE_IMPORT
        )

    private fun sortImports(nodes: Iterable<ASTNode>): Iterable<ASTNode> =
        joinWithNode(
            nodes.filter { it.elementType == KtNodeTypes.IMPORT_DIRECTIVE }.sortedBy { it.text },
            LeafPsiElement(KtTokens.WHITE_SPACE, "\n")
        )

    private fun joinWithNode(nodes: List<ASTNode>, node: ASTNode): List<ASTNode> {
        return if (nodes.isEmpty()) {
            listOf()
        } else {
            listOf(nodes.first()).plus(nodes.tail().flatMap { listOf(node, it) })
        }
    }

    private fun <E> List<E>.tail(): List<E> = subList(1, size)
}
