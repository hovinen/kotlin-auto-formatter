package org.kotlin.formatter.imports

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPackageDirective

/**
 * A function determining whether an import should be retained or removed.
 *
 * The parameters are:
 *
 *  * the imported name, i.e., how it is referred in the source code, and
 *  * the name of the path to the import, i.e., what appears right after `import` in the directive.
 */
typealias ImportPolicy = (String, String) -> Boolean

/**
 * An import policy which checks for the use of symbols in the source code.
 *
 * The return value is a function used in
 * [ImportListScanner][org.kotlin.formatter.scanning.ImportListScanner] to decide whether an import
 * should be retained or discarded.
 *
 * The parameter [node] represents a Kotlin source file to be scanned for existing references.
 *
 * This policy allows retention of the following imports:
 *
 *  * Those directly referenced via a [KtNodeTypes.REFERENCE_EXPRESSION] or a
 *    [KtNodeTypes.OPERATION_REFERENCE] in the source file, excluding `import` statements,
 *  * Wildcard imports,
 *  * Imports of symbols representing overloaded operators (see
 *    [operator overloading](https://kotlinlang.org/docs/reference/operator-overloading.html) and
 *    [operators]),
 *  * Imports of symbols representing numbered components (see
 *    [destructuring declarations](https://kotlinlang.org/docs/reference/multi-declarations.html)),
 *  * Imports of the Kotlin function required to provide delegates with the `by` keyword (see
 * [delegated properties](https://kotlinlang.org/docs/reference/delegated-properties.html)).
 *
 * All other imports are identified for removal.
 */
fun importPolicyForNode(node: ASTNode): ImportPolicy {
    val foundNames = mutableSetOf("*")
    var filePackage = ""
    var hasByKeyword = false

    fun isProvideDelegate(importPath: String): Boolean =
        importPath == "org.gradle.kotlin.dsl.provideDelegate"

    fun isUsedImport(importName: String, importPath: String) =
        foundNames.contains(importName) || operators.contains(importName) ||
            componentRegex.matches(importName) || (isProvideDelegate(importPath) && hasByKeyword)

    fun isAliasedImport(importPath: String, importName: String) = !importPath.endsWith(importName)

    fun isInDifferentPackage(importPath: String) = importPath.toImportPackage() != filePackage

    fun isNecessaryImport(importName: String, importPath: String) =
        isAliasedImport(importPath, importName) || isInDifferentPackage(importPath)

    node.visitIgnoringImportList {
        when (it.elementType) {
            KtNodeTypes.PACKAGE_DIRECTIVE -> {
                filePackage = (it.psi as KtPackageDirective).qualifiedName
            }
            KtNodeTypes.REFERENCE_EXPRESSION, KtNodeTypes.OPERATION_REFERENCE -> {
                foundNames.add(it.text.trim('`'))
            }
            KDocTokens.MARKDOWN_LINK -> {
                foundNames.add((it.psi as KDocLink).getLinkText().split('.').first().trim('`'))
            }
            KtTokens.BY_KEYWORD -> {
                hasByKeyword = true
            }
        }
    }
    return { importName, importPath ->
        isUsedImport(importName, importPath) && isNecessaryImport(importName, importPath)
    }
}

private fun String.toImportPackage(): String {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex == -1) "" else substring(0, dotIndex)
}

private fun ASTNode.visitIgnoringImportList(visitor: (ASTNode) -> Unit) {
    if (elementType == KtNodeTypes.IMPORT_LIST) {
        return
    }
    visitor(this)
    getChildren(null).forEach { it.visitIgnoringImportList(visitor) }
}

private val operators =
    setOf(
        "compareTo",
        "contains",
        "dec",
        "div",
        "divAssign",
        "equals",
        "get",
        "getValue",
        "inc",
        "invoke",
        "iterator",
        "minus",
        "minusAssign",
        "mod",
        "modAssign",
        "not",
        "plus",
        "plusAssign",
        "rangeTo",
        "rem",
        "timesAssign",
        "set",
        "setValue",
        "times",
        "unaryMinus",
        "unaryPlus"
    )

private val componentRegex = Regex("component\\d+")
