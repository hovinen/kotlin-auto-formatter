package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/**
 * A [NodeScanner] for the list of annotations and modifiers on a type or function declaration.
 *
 * The output [Token] list sorts the modifiers in the list to put annotations first in original
 * order, then modifier keywords in the order specified by the
 * [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers).
 *
 * @property markerCount how many [MarkerToken] should be inserted after each annotation
 * @property breakMode strategy on placing breaks after annotations
 */
internal class ModifierListScanner(
    private val kotlinScanner: KotlinScanner,
    private val markerCount: Int = 1,
    private val breakMode: BreakMode = BreakMode.PARAMETER
) : NodeScanner {
    /** Governs the [Token] which should appear immediately after each annotation. */
    internal enum class BreakMode(internal val breakToken: Token) {
        TYPE(ForcedBreakToken(count = 1)),
        FUNCTION_PROPERTY(ForcedBreakToken(count = 1)),
        PARAMETER(SynchronizedBreakToken(whitespaceLength = 1)),
        CONSTRUCTOR(WhitespaceToken(" "))
    }

    private val nodePattern =
        nodePattern {
            zeroOrMore { whitespace() }
            zeroOrMore {
                nodeOfType(KtNodeTypes.ANNOTATION_ENTRY) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                }
                zeroOrOne {
                    possibleWhitespace() thenMapToTokens { nodes ->
                        if (nodes.firstOrNull()?.textContains('\n') == true) {
                            listOf(ForcedBreakToken(count = 1))
                        } else {
                            listOf(WhitespaceToken(" "))
                        }
                    }
                    comment()
                } thenMapTokens { tokens -> tokens.plus(breakMode.breakToken) }
                possibleWhitespace()
            }
            zeroOrMore {
                nodeOfOneOfTypes(*KtTokens.MODIFIER_KEYWORDS_ARRAY) thenMapToTokens { nodes ->
                    kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                        .plus(WhitespaceToken(" "))
                }
            } thenMapTokens { tokens -> List(markerCount) { MarkerToken }.plus(tokens) }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(sortModifierNodes(node.children().asIterable()))

    private fun sortModifierNodes(nodes: Iterable<ASTNode>): Iterable<ASTNode> {
        val nonKeywordNodes = nodes.filter { !KtTokens.MODIFIER_KEYWORDS.contains(it.elementType) }
        val keywordNodes =
            sortModifiers(nodes.filter { KtTokens.MODIFIER_KEYWORDS.contains(it.elementType) })
        return nonKeywordNodes.plus(keywordNodes)
    }

    private fun sortModifiers(modifiers: List<ASTNode>): List<ASTNode> =
        modifiers.sortedWith(
            Comparator { node1, node2 ->
                modifierSortOrder[node1.text]!! - modifierSortOrder[node2.text]!!
            }
        )

    companion object {
        // From https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers
        private val modifierSortOrder =
            mapOf(
                "public" to 0,
                "protected" to 0,
                "private" to 0,
                "internal" to 0,
                "expect" to 1,
                "actual" to 1,
                "final" to 2,
                "open" to 2,
                "abstract" to 2,
                "sealed" to 2,
                "const" to 2,
                "external" to 3,
                "override" to 4,
                "lateinit" to 5,
                "tailrec" to 6,
                "vararg" to 7,
                "suspend" to 8,
                "inner" to 9,
                "enum" to 10,
                "annotation" to 10,
                "companion" to 11,
                "inline" to 12,
                "infix" to 13,
                "operator" to 14,
                "data" to 15
            )
    }
}
