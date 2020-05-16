package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for individual sections of KDoc. */
internal class KDocSectionScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        possibleWhitespace()
        zeroOrMore {
            either {
                nodeOfType(KDocElementTypes.KDOC_TAG) andThen { nodes ->
                    listOf(
                        *kotlinScanner.scanNodes(nodes, ScannerState.KDOC).toTypedArray(),
                        ForcedBreakToken(count = 1)
                    )
                }
            } or {
                anyNode() andThen { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.KDOC)
                }
            }
        }
        anyNode() andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.KDOC)
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        inBeginEndBlock(
            trimTrailingWhitespace(nodePattern.matchSequence(node.children().toList())),
            State.LONG_COMMENT
        )

    private fun trimTrailingWhitespace(tokens: List<Token>): List<Token> =
        if (tokens.lastOrNull() is WhitespaceToken) {
            tokens.subList(0, tokens.size - 1)
        } else {
            tokens
        }
}
