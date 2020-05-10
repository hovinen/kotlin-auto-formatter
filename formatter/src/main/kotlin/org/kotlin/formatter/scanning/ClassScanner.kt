package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BlockFromLastForcedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for class and interface definitions. */
internal class ClassScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        zeroOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
        }
        exactlyOne {
            nodeOfOneOfTypes(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD)
            oneOrMoreFrugal { anyNode() }
        } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        zeroOrOne { nodeOfType(KtNodeTypes.CLASS_BODY) } andThen { nodes ->
            if (nodes.isNotEmpty()) {
                kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
            } else {
                listOf(BlockFromLastForcedBreakToken)
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
