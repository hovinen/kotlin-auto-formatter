package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ForceSynchronizedBreaksInBlockToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for the list of annotations at the top of a Kotlin file. */
internal class FileAnnotationListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            zeroOrMore {
                either {
                    simpleAnnotation() thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    }
                } or {
                    nodeOfOneOfTypes(
                        KtNodeTypes.ANNOTATION,
                        KtNodeTypes.ANNOTATION_ENTRY
                    ) thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                            .plus(ForceSynchronizedBreaksInBlockToken)
                    }
                }
                possibleWhitespace() thenMapToTokens { nodes ->
                    if (nodes.firstOrNull()?.textContains('\n') == true) {
                        listOf(ForcedBreakToken(count = 1))
                    } else {
                        listOf(WhitespaceToken(" "))
                    }
                }
                zeroOrOne {
                    comment() thenMapTokens { tokens -> tokens.plus(ForcedBreakToken(count = 1)) }
                }
                possibleWhitespace()
            }
            end()
        }

    private fun NodePatternBuilder.simpleAnnotation(): NodePatternBuilder =
        nodeMatching {
            it.elementType == KtNodeTypes.ANNOTATION_ENTRY &&
                it.lastChildNode.elementType == KtNodeTypes.CONSTRUCTOR_CALLEE
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
