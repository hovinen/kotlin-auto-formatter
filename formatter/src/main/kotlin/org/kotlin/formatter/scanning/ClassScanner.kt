package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for class and interface definitions. */
internal class ClassScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        optionalKDoc(kotlinScanner)
        possibleWhitespaceWithComment()
        zeroOrOne {
            nodeOfType(KtNodeTypes.MODIFIER_LIST)
        } thenMapToTokens { nodes ->
            listOf(MarkerToken).plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
        }
        possibleWhitespace()
        exactlyOne {
            nodeOfOneOfTypes(KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD, KtTokens.INTERFACE_KEYWORD)
            zeroOrMoreFrugal { anyNode() }
        } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
        possibleWhitespace()
        zeroOrOne {
            nodeOfType(KtNodeTypes.CLASS_BODY)
        } thenMapToTokens { nodes ->
            if (nodes.isNotEmpty()) {
                listOf(nonBreakingSpaceToken()).plus(kotlinScanner.scanNodes(nodes, ScannerState.BLOCK))
            } else {
                listOf(BlockFromMarkerToken)
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
