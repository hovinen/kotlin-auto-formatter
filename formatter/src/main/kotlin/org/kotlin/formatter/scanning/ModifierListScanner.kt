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

                        // We use a ClosingForcedBreakToken rather than a ForcedBreakToken here
                        // because the modifier list is inside a block representing the full
                        // property, class, or function declaration, and a ForcedBreakToken would
                        // then cause the next lines to indent with the next standard indent.
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
