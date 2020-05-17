package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for function declarations. */
internal class FunctionDeclarationScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val nodePattern = nodePattern {
        either {
            declarationWithOptionalModifierList(kotlinScanner)
            possibleWhitespace()
            nodeOfType(KtNodeTypes.BLOCK) andThen { nodes ->
                listOf(
                    nonBreakingSpaceToken(),
                    *kotlinScanner.scanNodes(nodes, ScannerState.BLOCK).toTypedArray()
                )
            }
        } or {
            exactlyOne {
                declarationWithOptionalModifierList(kotlinScanner)
                zeroOrOne {
                    propertyInitializer(kotlinScanner)
                }
            } thenMapTokens { inBeginEndBlock(it, State.CODE) }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        return nodePattern.matchSequence(node.children().asIterable())
    }
}
