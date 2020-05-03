package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class ModifierListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        oneOrMore {
            either {
                nodeOfType(KtNodeTypes.ANNOTATION_ENTRY) andThen { nodes ->
                    listOf(
                        *kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).toTypedArray(),
                        ClosingForcedBreakToken
                    )
                }
                possibleWhitespace()
            } or {
                anyNode() andThen { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
