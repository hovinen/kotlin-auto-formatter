package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for KDoc. */
internal class KDocScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KDocTokens.START) andThen {
            listOf(
                BeginToken(State.LONG_COMMENT),
                LeafNodeToken("/**"),
                SynchronizedBreakToken(whitespaceLength = 0)
            )
        }
        zeroOrMore { anyNode() } andThen { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.KDOC)
        }
        nodeOfType(KDocTokens.END) andThen {
            listOf(
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("*/"),
                EndToken
            )
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
