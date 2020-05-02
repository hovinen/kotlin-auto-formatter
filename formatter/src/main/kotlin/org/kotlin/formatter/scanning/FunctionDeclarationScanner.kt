package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.State
import org.kotlin.formatter.Token
import org.kotlin.formatter.nonBreakingSpaceToken
import org.kotlin.formatter.scanning.nodepattern.nodePattern

internal class FunctionDeclarationScanner(private val kotlinScanner: KotlinScanner): NodeScanner {
    private val blockPattern = nodePattern {
        oneOrMoreFrugal { anyNode() }.andThen { nodes ->
            inBeginEndBlock(kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT), State.CODE)
        }
        zeroOrOne { propertyInitializer(kotlinScanner) }
        zeroOrOne {
            possibleWhitespace()
            nodeOfType(KtNodeTypes.BLOCK) andThen { nodes ->
                listOf(
                    nonBreakingSpaceToken(content = " "),
                    *kotlinScanner.scanNodes(nodes, ScannerState.BLOCK).toTypedArray()
                )
            }
        }
        end()
    }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> {
        return inBeginEndBlock(blockPattern.matchSequence(node.children().asIterable()), State.CODE)
    }
}
