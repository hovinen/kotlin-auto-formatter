package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Base class for exceptions thrown when an error is found while scanning the input AST.
 *
 * @property nodes the sequence of [ASTNode] which triggered the error for determining the location
 *     of the error in the original source code
 */
open class KotlinScannerException(internal val nodes: Iterable<ASTNode>, message: String) :
    Exception(message)
