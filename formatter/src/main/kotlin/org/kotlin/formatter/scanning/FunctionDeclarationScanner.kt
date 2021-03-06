package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for function declarations. */
internal class FunctionDeclarationScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(
            kotlinScanner,
            markerCount = 2,
            breakMode = ModifierListScanner.BreakMode.FUNCTION_PROPERTY
        )
    private val nodePattern =
        nodePattern {
            either {
                declarationWithOptionalModifierList(kotlinScanner)
                possibleWhitespace()
                nodeOfType(KtNodeTypes.BLOCK) thenMapToTokens { nodes ->
                    listOf(nonBreakingSpaceToken())
                        .plus(kotlinScanner.scanNodes(nodes, ScannerState.BLOCK))
                }
            } or {
                declarationWithOptionalModifierList(
                    kotlinScanner,
                    modifierListScanner,
                    markerCount = 2
                )
                optionalFunctionInitializer(kotlinScanner)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching the initializer for a property or
 * function.
 */
private fun NodePatternBuilder.optionalFunctionInitializer(kotlinScanner: KotlinScanner) {
    zeroOrOne {
        possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) }
        nodeOfType(KtTokens.EQ) thenMapToTokens {
            listOf(
                LeafNodeToken("="),
                BlockFromMarkerToken,
                BeginToken(state = State.CODE),
                WhitespaceToken(" ")
            )
        }
        possibleWhitespaceWithComment()
        zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(EndToken)
        }
    } thenMapTokens { it.plus(BlockFromMarkerToken) }
}
