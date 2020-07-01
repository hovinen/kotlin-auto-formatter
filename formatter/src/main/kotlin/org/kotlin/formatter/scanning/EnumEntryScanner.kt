package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.children
import org.kotlin.formatter.Token
import org.kotlin.formatter.scanning.nodepattern.nodePattern

/** A [NodeScanner] for individual entries of an `enum`. */
internal class EnumEntryScanner(private val kotlinScanner: KotlinScanner) : NodeScanner {
    private val modifierListScanner =
        ModifierListScanner(kotlinScanner, breakMode = ModifierListScanner.BreakMode.TYPE)
    private val nodePattern =
        nodePattern {
            optionalKDoc(kotlinScanner, modifierListScanner)
            oneOrMore { anyNode() } thenMapToTokens { nodes ->
                kotlinScanner.scanNodes(nodes, ScannerState.STATEMENT)
            }
            end()
        }

    override fun scan(node: ASTNode, scannerState: ScannerState): List<Token> =
        nodePattern.matchSequence(node.children().asIterable())
}
