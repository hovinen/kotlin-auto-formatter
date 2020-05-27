package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/**
 * A [NodeScanner] for a list of parameters in a function or class declaration, or the arguments of
 * a function call expression.
 */
internal class ParameterListScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(kotlinScanner, breakMode = ModifierListScanner.BreakMode.PARAMETER)
    private val parameterPattern = nodePattern {
        zeroOrOne {
            nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT)
                    .plus(WhitespaceToken(" "))
            }
            possibleWhitespace()
        }
        oneOrMore { anyNode() } thenMapToTokens { nodes ->
            inBeginEndBlock(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT), State.CODE)
        }
        end()
    }
    private var isFirstEntry = false

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        isFirstEntry = true
        val children = node.children().toList()
        return kotlinScanner.scanNodes(children, ScannerState.STATEMENT) { childNode, _ ->
            scanEntry(childNode)
        }
    }

    private fun scanEntry(node: ASTNode): List<Token> {
        return when (node.elementType) {
            KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.VALUE_ARGUMENT -> {
                val childTokens = parameterPattern.matchSequence(node.children().asIterable())
                val breakToken =
                    SynchronizedBreakToken(whitespaceLength = if (isFirstEntry) 0 else 1)
                isFirstEntry = false
                listOf(breakToken).plus(childTokens)
            }
            KtTokens.WHITE_SPACE -> listOf()
            KtTokens.LPAR -> listOf(LeafNodeToken("("), BeginToken(State.CODE))
            KtTokens.RPAR ->
                if (isFirstEntry) {
                    listOf(EndToken, LeafNodeToken(")"))
                } else {
                    listOf(
                        ClosingSynchronizedBreakToken(whitespaceLength = 0),
                        EndToken,
                        LeafNodeToken(")")
                    )
                }
            else -> kotlinScanner.scanInState(node, ScannerState.STATEMENT)
        }
    }
}
