package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for references to types. */
internal class TypeReferenceScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            zeroOrOne {
                nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
            possibleWhitespace()
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
