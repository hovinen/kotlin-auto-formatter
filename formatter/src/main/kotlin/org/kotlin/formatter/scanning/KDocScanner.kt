package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.KDocContentToken
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
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1)
            )
        }
        possibleWhitespace()
        zeroOrMoreFrugal { nodeOfType(KDocElementTypes.KDOC_SECTION) } andThen { nodes ->
            val fullContent = nodes.joinToString("") { scanIntoString(it.children().asIterable()) }
            listOf(KDocContentToken(content = fullContent.trim()))
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

    private fun scanIntoString(nodes: Iterable<ASTNode>): String =
        sectionNodePattern.matchSequence(nodes).joinToString("") { (it as KDocContentToken).content }

    private val sectionNodePattern = nodePattern {
        zeroOrOne {
            nodeNotOfType(KDocTokens.LEADING_ASTERISK) andThen { nodes ->
                listOf(KDocContentToken(content = nodes.first().text.trimStart()))
            }
            zeroOrMoreFrugal { anyNode() } andThen { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { it.text }))
            }
            zeroOrOne { whitespaceWithNewline() andThen { listOf(KDocContentToken(content = "\n")) } }
        }
        zeroOrMore {
            nodeOfType(KDocTokens.LEADING_ASTERISK)
            zeroOrOne {
                nodeNotOfType(KtTokens.WHITE_SPACE) andThen { nodes ->
                    listOf(KDocContentToken(content = nodes.first().text.trimFirstWhitespace()))
                }
                zeroOrMoreFrugal { anyNode() } andThen { nodes ->
                    listOf(KDocContentToken(content = nodes.joinToString("") { it.text }))
                }
            }
            zeroOrOne {
                whitespaceWithNewline() andThen { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().text.count { it == '\n' })))
                }
            }
        }
        zeroOrMore {
            nodeOfType(KDocElementTypes.KDOC_TAG) andThen { nodes ->
                listOf(KDocContentToken(content = scanIntoString(nodes.first().children().asIterable())))
            }
        }
        end()
    }

    private fun String.trimFirstWhitespace(): String =
        if (startsWith(' ')) {
            substring(1, length)
        } else {
            this
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
