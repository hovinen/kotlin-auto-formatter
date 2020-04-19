package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

internal class ClassScanner(private val kotlinScanner: KotlinScanner) {
    fun scanClassNode(node: ASTNode): List<Token> {
        val children = node.children().toList()
        val indexOfClassKeyword = children.indexOfFirst { it.elementType == KtTokens.CLASS_KEYWORD }
        val indexOfClassBody = children.indexOfFirst { it.elementType == KtNodeTypes.CLASS_BODY }
        val nodesBeforeClassKeyword = children.subList(0, indexOfClassKeyword)
        val tokensBeforeClassKeyword = kotlinScanner.scanNodes(nodesBeforeClassKeyword, scannerState = KotlinScanner.ScannerState.BLOCK)
        val nodesWithinClassDeclaration =
            if (indexOfClassBody == -1) {
                children.subList(indexOfClassKeyword, children.size)
            } else {
                children.subList(indexOfClassKeyword, indexOfClassBody)
            }
        val tokensWithinClassDeclaration = kotlinScanner.scanNodes(nodesWithinClassDeclaration, scannerState = KotlinScanner.ScannerState.STATEMENT)
        val tokensOfBlock =
            if (indexOfClassBody != -1) {
                kotlinScanner.scanNodes(children.subList(indexOfClassBody, children.size), KotlinScanner.ScannerState.BLOCK)
            } else {
                listOf()
            }
        return listOf(
            *tokensBeforeClassKeyword.toTypedArray(),
            *inBeginEndBlock(tokensWithinClassDeclaration, State.CODE).toTypedArray(),
            *tokensOfBlock.toTypedArray()
        )
    }
}
