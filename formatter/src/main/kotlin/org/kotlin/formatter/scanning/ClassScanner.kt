package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class ClassScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        zeroOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, scannerState = ScannerState.BLOCK)
        }
        exactlyOne {
            nodeOfType(KtTokens.CLASS_KEYWORD)
            oneOrMoreFrugal { anyNode() }
        } andThen { nodes ->
            inBeginEndBlock(kotlinScanner.scanNodes(nodes, scannerState = ScannerState.STATEMENT), State.CODE)
        }
        zeroOrOne { nodeOfType(KtNodeTypes.CLASS_BODY) } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
