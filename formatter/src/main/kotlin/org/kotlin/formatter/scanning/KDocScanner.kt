package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.KDocContentToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for KDoc. */
internal class KDocScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern = nodePattern {
        nodeOfType(KDocTokens.START) andThen {
            listOf(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1)
            )
        }
        possibleWhitespace()
        zeroOrMore { nodeOfType(KDocElementTypes.KDOC_SECTION) } andThen { nodes ->
            val fullContent = nodes.joinToString("") { scanChildrenIntoString(it) }
            listOf(KDocContentToken(content = fullContent.trimOneWhitespace()))
        }
        possibleWhitespace()
        nodeOfType(KDocTokens.END) andThen {
            listOf(
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
        }
        end()
    }

    private fun String.trimOneWhitespace(): String =
        if (startsWith(' ') && endsWith(' ')) {
            substring(1, length - 1)
        } else {
            this
        }

    private fun scanChildrenIntoString(node: ASTNode): String =
        sectionNodePattern.matchSequence(node.children().asIterable())
            .joinToString("") { (it as KDocContentToken).content }

    private val sectionNodePattern = nodePattern {
        zeroOrOne {
            oneOrMoreFrugal { nodeNotOfType(KDocTokens.LEADING_ASTERISK) } andThen { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { scanNodeToString(it) }))
            }
            zeroOrOne {
                whitespaceWithNewline() andThen { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().countNewlines())))
                }
            }
        }
        zeroOrMore {
            nodeOfType(KDocTokens.LEADING_ASTERISK)
            zeroOrMoreFrugal { anyNode() } andThen { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { scanNodeToString(it) }.trimFirstWhitespace()))
            }
            zeroOrOne {
                whitespaceWithNewline() andThen { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().countNewlines())))
                }
            }
        }
        end()
    }

    private fun scanNodeToString(node: ASTNode): String =
        if (node.elementType == KDocElementTypes.KDOC_TAG) {
            scanChildrenIntoString(node)
        } else {
            node.text
        }

    private fun ASTNode.countNewlines(): Int = text.count { it == '\n' }

    private fun String.trimFirstWhitespace(): String =
        if (startsWith(' ')) {
            substring(1, length)
        } else {
            this
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
