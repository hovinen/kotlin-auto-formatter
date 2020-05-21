package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/**
 * A [NodeScanner] for the list of annotations and modifiers on a type or function declaration.
 * 
 * @property markerCount how many [MarkerToken] should be inserted after each annotation
 */
internal class ModifierListScanner(
    private val kotlinScanner: KotlinScanner,
    private val markerCount: Int = 1
) : NodeScanner {
    private val nodePattern = nodePattern {
        oneOrMore {
            either {
                nodeOfType(KtNodeTypes.ANNOTATION_ENTRY) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                        .plus(ForcedBreakToken(count = 1))
                        .plus(List(markerCount) { MarkerToken })
                }
                possibleWhitespace()
            } or {
                anyNode() thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                        .plus(WhitespaceToken(" "))
                }
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
