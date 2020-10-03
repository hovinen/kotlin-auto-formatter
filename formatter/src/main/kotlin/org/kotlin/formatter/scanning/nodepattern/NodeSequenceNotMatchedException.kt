package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.kotlin.formatter.scanning.KotlinScannerException

/**
 * Exception thrown when a sequence of [ASTNode] is expected to be matched by a [NodePattern] but is
 * not matched.
 *
 * @property nodes the sequence of [ASTNode] which failed to match
 */
class NodeSequenceNotMatchedException(nodes: Iterable<ASTNode>) :
    KotlinScannerException(nodes, "Could not match node sequence ${nodes.toList()}")
