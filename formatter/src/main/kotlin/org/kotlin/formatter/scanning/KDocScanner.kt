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
        nodeOfType(KDocTokens.START) thenMapToTokens {
            listOf(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1)
            )
        }
        possibleWhitespace()
        zeroOrMore { nodeOfType(KDocElementTypes.KDOC_SECTION) } thenMapToTokens { nodes ->
            val fullContent = nodes.joinToString("") { scanChildrenToString(it) }
            listOf(KDocContentToken(content = fullContent.trimOneWhitespace()))
        }
        possibleWhitespace()
        nodeOfType(KDocTokens.END) thenMapToTokens {
            listOf(
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
        }
        end()
    }

    private fun scanChildrenToString(node: ASTNode): String =
        sectionNodePattern.matchSequence(node.children().asIterable())
            .joinToString("") { (it as KDocContentToken).content }

    private fun String.trimOneWhitespace(): String =
        if (startsWith(' ') && endsWith(' ')) {
            substring(1, length - 1)
        } else {
            this
        }

    private val sectionNodePattern = nodePattern {
        zeroOrOne {
            oneOrMoreFrugal { nodeNotOfType(KDocTokens.LEADING_ASTERISK) } thenMapToTokens { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { scanNodeToString(it) }))
            }
            zeroOrOne {
                whitespaceWithNewline() thenMapToTokens { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().countNewlines())))
                }
            }
        }
        zeroOrMore {
            nodeOfType(KDocTokens.LEADING_ASTERISK)
            zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
                listOf(KDocContentToken(content = nodes.joinToString("") { scanNodeToString(it) }.trimFirstWhitespace()))
            }
            zeroOrOne {
                whitespaceWithNewline() thenMapToTokens { nodes ->
                    listOf(KDocContentToken(content = "\n".repeat(nodes.first().countNewlines())))
                }
            }
        }
        end()
    }

    private fun scanNodeToString(node: ASTNode): String =
        if (node.elementType == KDocElementTypes.KDOC_TAG) {
            scanChildrenToString(node)
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
