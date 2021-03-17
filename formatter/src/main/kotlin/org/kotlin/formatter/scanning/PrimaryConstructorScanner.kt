package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.inBeginEndBlock
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for the primary constructor of a class. */
internal class PrimaryConstructorScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(
            kotlinScanner = kotlinScanner,
            markerCount = 0,
            breakMode = ModifierListScanner.BreakMode.CONSTRUCTOR
        )
    private val nodePattern =
        nodePattern {
            zeroOrOne {
                nodeOfType(KtNodeTypes.MODIFIER_LIST) thenMapToTokens { nodes ->
                    modifierListScanner.scan(nodes.first(), ScannerState.STATEMENT)
                }
            }
            // Swallow any trailing whitespace after the last modifier
            possibleWhitespace()
            possibleWhitespaceWithComment()
            zeroOrOne { nodeOfType(KtTokens.CONSTRUCTOR_KEYWORD) } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            possibleWhitespace()
            zeroOrOne {
                nodeOfType(KtNodeTypes.VALUE_PARAMETER_LIST) thenMapToTokens { nodes ->
                    inBeginEndBlock(
                        kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT),
                        State.CODE
                    )
                }
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
