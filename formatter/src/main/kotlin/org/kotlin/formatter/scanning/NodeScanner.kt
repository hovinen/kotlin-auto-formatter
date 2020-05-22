package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtScriptElementType
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

/**
 * An abstract scanner for a particular type of [ASTNode].
 */
internal interface NodeScanner {
    /**
     * Scans an [ASTNode] of a particular type in the state [ScannerState], returning the resulting
     * list of [Token].
     */
    fun scan(node: ASTNode, scannerState: ScannerState): List<Token>
}

/**
 * Returns a [NodeScanner] to scan [ASTNode] of the given [elementType].
 *
 * The parameter [kotlinScanner] is a top-level scanner for all Kotlin constructions for use when
 * scanning [ASTNode] recursively.
 */
internal fun nodeScannerForElementType(
    kotlinScanner: KotlinScanner,
    elementType: IElementType
): NodeScanner =
    when (elementType) {
        KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY -> {
            BlockScanner(kotlinScanner)
        }
        KtNodeTypes.WHEN -> {
            WhenExpressionScanner(kotlinScanner)
        }
        KtNodeTypes.IF -> {
            IfExpressionScanner(kotlinScanner)
        }
        KDocTokens.KDOC -> {
            KDocScanner(kotlinScanner)
        }
        KtNodeTypes.STRING_TEMPLATE -> {
            StringLiteralScanner(kotlinScanner)
        }
        KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST -> {
            ParameterListScanner(kotlinScanner)
        }
        KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION -> {
            ClassScanner(kotlinScanner)
        }
        KtNodeTypes.FUN -> {
            FunctionDeclarationScanner(kotlinScanner)
        }
        KtNodeTypes.TYPEALIAS -> {
            TypealiasScanner(kotlinScanner)
        }
        KtNodeTypes.ENUM_ENTRY -> {
            EnumEntryScanner(kotlinScanner)
        }
        KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
            DotQualifiedExpressionScanner(kotlinScanner)
        }
        KtNodeTypes.FUNCTION_LITERAL -> {
            FunctionLiteralScanner(kotlinScanner)
        }
        KtNodeTypes.CONDITION -> {
            ConditionScanner(kotlinScanner)
        }
        KtNodeTypes.FOR -> {
            ForExpressionScanner(kotlinScanner)
        }
        KtNodeTypes.BINARY_EXPRESSION -> {
            BinaryExpressionScanner(kotlinScanner)
        }
        KtNodeTypes.CALL_EXPRESSION -> {
            CallExpressionScanner(kotlinScanner)
        }
        KtNodeTypes.PROPERTY -> {
            PropertyScanner(kotlinScanner)
        }
        KtNodeTypes.PACKAGE_DIRECTIVE,
        KtNodeTypes.IMPORT_LIST,
        KtNodeTypes.IMPORT_DIRECTIVE,
        KtNodeTypes.IMPORT_ALIAS -> {
            SimpleBlockScanner(kotlinScanner, ScannerState.PACKAGE_IMPORT, State.PACKAGE_IMPORT)
        }
        KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.VALUE_ARGUMENT -> {
            SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.CODE)
        }
        KtNodeTypes.MODIFIER_LIST -> {
            ModifierListScanner(kotlinScanner)
        }
        KtNodeTypes.RETURN -> {
            ReturnScanner(kotlinScanner)
        }
        KtNodeTypes.THROW -> {
            ThrowScanner(kotlinScanner)
        }
        KtNodeTypes.CATCH -> {
            CatchScanner(kotlinScanner)
        }
        KtNodeTypes.PROPERTY_ACCESSOR -> {
            SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.CODE)
        }
        KtFileElementType.INSTANCE, is KtScriptElementType, KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY -> {
            SimpleScanner(kotlinScanner, ScannerState.BLOCK)
        }
        KtNodeTypes.WHEN_ENTRY, KtNodeTypes.ANNOTATION_ENTRY, KtNodeTypes.PARENTHESIZED -> {
            SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.CODE)
        }
        KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY -> {
            SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.STRING_LITERAL)
        }
        else -> {
            SimpleScanner(kotlinScanner, ScannerState.STATEMENT)
        }
    }
