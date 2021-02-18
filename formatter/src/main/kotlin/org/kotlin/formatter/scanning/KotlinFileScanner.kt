package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for a full Kotlin file. */
internal class KotlinFileScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            either {
                nonEmptyPackageDirective() thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
                possibleWhitespace()
                exactlyOne {
                    nodeOfType(KtNodeTypes.IMPORT_LIST) thenMapToTokens { nodes ->
                        kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                    }
                } thenMapTokens { tokens ->
                    if (tokens.filterIsInstance<LeafNodeToken>().isNotEmpty()) {
                        listOf(ForcedBreakToken(count = 2)).plus(tokens)
                    } else {
                        listOf()
                    }
                }
                nodeOfType(KtNodeTypes.SCRIPT) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
            } or {
                nodeOfType(KtNodeTypes.PACKAGE_DIRECTIVE)
                possibleWhitespace()
                nodeOfType(KtNodeTypes.IMPORT_LIST) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
                nodeOfType(KtNodeTypes.SCRIPT) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.BLOCK)
                }
            }
            end()
        }

    private fun NodePatternBuilder.nonEmptyPackageDirective(): NodePatternBuilder =
        nodeMatching { it.elementType == KtNodeTypes.PACKAGE_DIRECTIVE && it.text.isNotBlank() }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
