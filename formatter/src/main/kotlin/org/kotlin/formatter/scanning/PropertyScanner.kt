package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.NodePatternBuilder
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for property declarations. */
internal class PropertyScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val nodePattern =
        nodePattern {
            exactlyOne {
                declarationWithOptionalModifierList(kotlinScanner)
                zeroOrOne { propertyInitializer(kotlinScanner) }
                zeroOrMore {
                    possibleWhitespace()
                    nodeOfType(KtNodeTypes.PROPERTY_ACCESSOR) thenMapToTokens { nodes ->
                        listOf(ForcedBreakToken(count = 1))
                            .plus(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT))
                    }
                }
            } thenMapTokens { it.plus(BlockFromMarkerToken) }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching a class, function, or property
 * declaration which be preceded by a modifier list.
 *
 * @param markerCount the number of [MarkerToken] which should appear before the first
 *     non-annotation modifier or the declaration keyword
 */
internal fun NodePatternBuilder.declarationWithOptionalModifierList(
    kotlinScanner: KotlinScanner,
    modifierListScanner: ModifierListScanner =
        ModifierListScanner(
            kotlinScanner,
            breakMode = ModifierListScanner.BreakMode.FUNCTION_PROPERTY
        ),
    markerCount: Int = 1
) {
    optionalKDoc(kotlinScanner, modifierListScanner)
    possibleWhitespaceWithComment()
    either {
        nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
            modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT)
        }
        possibleWhitespace()
        zeroOrOne {
            comment()
            possibleWhitespace() thenMapToTokens {
                listOf(ForcedBreakToken(count = 1)).plus(List(markerCount) { MarkerToken })
            }
        }
        declarationKeyword(kotlinScanner)
        possibleWhitespace()
        zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
    } or {
        declarationKeyword(kotlinScanner) thenMapTokens { tokens ->
            List(markerCount) { MarkerToken }.plus(tokens)
        }
        possibleWhitespace()
        zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
        }
    }
    zeroOrOne {
        possibleWhitespace()
        nodeOfType(KtTokens.COLON)
        possibleWhitespace()
        nodeOfType(KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL) thenMapToTokens { nodes ->
            listOf(LeafNodeToken(" :"), WhitespaceToken(" "))
                .plus(
                    inBeginEndBlock(
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT),
                        State.CODE
                    )
                )
        }
    }
}

private fun NodePatternBuilder.declarationKeyword(
    kotlinScanner: KotlinScanner
): NodePatternBuilder =
    either {
        nodeOfType(KtTokens.CONSTRUCTOR_KEYWORD) thenMapToTokens {
            listOf(LeafNodeToken("constructor"))
        }
    } or {
        anyNode() thenMapToTokens { nodes ->
            kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT).plus(nonBreakingSpaceToken())
        }
    }

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching optionally present KDoc.
 *
 * The KDoc may be preceded by a modifier list, in which case it is relocated to after the list.
 */
internal fun NodePatternBuilder.optionalKDoc(
    kotlinScanner: KotlinScanner,
    modifierListScanner: ModifierListScanner
) {
    zeroOrOne {
        exactlyOne {
            zeroOrOne { nodeOfType(KtNodeTypes.MODIFIER_LIST) }
            possibleWhitespace()
            nodeOfType(KDocTokens.KDOC)
        } thenMapToTokens { nodes ->
            if (nodes.first().elementType == KtNodeTypes.MODIFIER_LIST) {
                kotlinScanner.scanNodes(listOf(nodes.last()), ScannerState.STATEMENT)
                    .plus(ForcedBreakToken(count = 1))
                    .plus(modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT))
            } else {
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
                    .plus(ForcedBreakToken(count = 1))
            }
        }
        possibleWhitespace()
    }
}

/**
 * Adds to the receiver [NodePatternBuilder] a sequence matching a property initializer.
 *
 * The spacing and breaking rules around the assignment operator are then adjusted according to the
 * Kotlin coding conventions and Google Kotlin Style Guide.
 */
internal fun NodePatternBuilder.propertyInitializer(kotlinScanner: KotlinScanner) {
    possibleWhitespace() thenMapToTokens { listOf(nonBreakingSpaceToken()) }
    nodeOfType(KtTokens.EQ) thenMapToTokens { listOf(LeafNodeToken("=")) }
    possibleWhitespace() thenMapToTokens { listOf(WhitespaceToken(" ")) }
    zeroOrMoreFrugal { anyNode() } thenMapToTokens { nodes ->
        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
    }
}
