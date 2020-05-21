package org.kotlin.formatter.scanning.nodepattern

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Exception thrown when a sequence of [ASTNode] is expected to be matched by a [NodePattern] but is
 * not matched.
 *
 * @property nodes the sequence of [ASTNode] which failed to match
 */
class NodeSequenceNotMatchedException(internal val nodes: Iterable<ASTNode>) :
    Exception("Could not match node sequence ${nodes.toList()}")
