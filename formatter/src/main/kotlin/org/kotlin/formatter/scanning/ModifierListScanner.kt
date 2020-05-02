package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
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
                        ForcedBreakToken(count = 1)
                    )
                }
            } or {
                anyNode()
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
