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
            val fullContent = nodes.joinToString("") { scanIntoString(it.children().asIterable()) }
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

    private fun scanIntoString(nodes: Iterable<ASTNode>): String =
        sectionNodePattern.matchSequence(nodes).joinToString("") { (it as KDocContentToken).content }

    private val sectionNodePattern = nodePattern {
        zeroOrMore {
            nodeOfType(KDocElementTypes.KDOC_TAG) andThen { nodes ->
                listOf(
                    KDocContentToken(
                        content = scanIntoString(nodes.first().children().asIterable())
                    )
                )
            }
            possibleWhitespace() andThen { nodes ->
                listOf(KDocContentToken(content = "\n".repeat(nodes.map { it.text.countNewlines() }.sum())))
            }
        }
        zeroOrOne {
            oneOrMoreFrugal { nodeNotOfType(KDocTokens.LEADING_ASTERISK) } andThen { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { it.text }))
            }
            zeroOrOne {
                whitespaceWithNewline() andThen { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().text.countNewlines())))
                }
            }
        }
        zeroOrMore {
            nodeOfType(KDocTokens.LEADING_ASTERISK)
            zeroOrMoreFrugal { anyNode() } andThen { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { it.text }.trimFirstWhitespace()))
            }
            zeroOrOne {
                whitespaceWithNewline() andThen { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().text.countNewlines())))
                }
            }
        }
        end()
    }

    private fun String.countNewlines(): Int = count { it == '\n' }

    private fun String.trimFirstWhitespace(): String =
        if (startsWith(' ')) {
            substring(1, length)
        } else {
            this
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
