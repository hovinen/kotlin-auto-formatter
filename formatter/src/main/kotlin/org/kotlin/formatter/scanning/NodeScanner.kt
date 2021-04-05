package org.kotlin.formatter.scanning

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtScriptElementType
import org.kotlin.formatter.State
import org.kotlin.formatter.Token

/** An abstract scanner for a particular type of [ASTNode]. */
internal interface NodeScanner {
    /**
     * Scans an [ASTNode] of a particular type in the state [ScannerState], returning the resulting
     * list of [Token].
     */
    fun scan(node: ASTNode, scannerState: ScannerState): List<Token>
}

/** Provides the correct [NodeScanner] to scan a given [ASTNode] while in a given [ScannerState]. */
internal class NodeScannerProvider(
    private val kotlinScanner: KotlinScanner,
    private val importPolicy: (String, String) -> Boolean
) {
    private val kotlinFileScanner = lazy { KotlinFileScanner(kotlinScanner) }
    private val blockScanner = lazy { BlockScanner(kotlinScanner) }
    private val whenExpressionScanner = lazy { WhenExpressionScanner(kotlinScanner) }
    private val ifExpressionScanner = lazy { IfExpressionScanner(kotlinScanner) }
    private val whileExpressionScanner = lazy { WhileExpressionScanner(kotlinScanner) }
    private val kDocScanner = lazy { KDocScanner(kotlinScanner) }
    private val stringLiteralScanner = lazy { StringLiteralScanner(kotlinScanner) }
    private val parameterListScanner = lazy { ParameterListScanner(kotlinScanner) }
    private val typeArgumentListScanner = lazy { TypeArgumentListScanner(kotlinScanner) }
    private val collectionLiteralExpressionScanner =
        lazy { CollectionLiteralExpressionScanner(kotlinScanner) }
    private val classScanner = lazy { ClassScanner(kotlinScanner) }
    private val functionDeclarationScanner = lazy { FunctionDeclarationScanner(kotlinScanner) }
    private val classInitializerScanner = lazy { ClassInitializerScanner(kotlinScanner) }
    private val typealiasScanner = lazy { TypealiasScanner(kotlinScanner) }
    private val typeParameterListScanner = lazy { TypeParameterListScanner(kotlinScanner) }
    private val typeParameterScanner = lazy { TypeParameterScanner(kotlinScanner) }
    private val enumEntryScanner = lazy { EnumEntryScanner(kotlinScanner) }
    private val dotQualifiedExpressionScanner =
        lazy { DotQualifiedExpressionScanner(kotlinScanner) }
    private val functionLiteralScanner = lazy { FunctionLiteralScanner(kotlinScanner) }
    private val conditionScanner = lazy { ConditionScanner(kotlinScanner) }
    private val forExpressionScanner = lazy { ForExpressionScanner(kotlinScanner) }
    private val doWhileExpressionScanner = lazy { DoWhileExpressionScanner(kotlinScanner) }
    private val binaryExpressionScanner = lazy { BinaryExpressionScanner(kotlinScanner) }
    private val callExpressionScanner = lazy { CallExpressionScanner(kotlinScanner) }
    private val propertyScanner = lazy { PropertyScanner(kotlinScanner) }
    private val importListScanner = lazy { ImportListScanner(kotlinScanner, importPolicy) }
    private val simpleBlockScannerForPackageImport =
        lazy {
            SimpleBlockScanner(kotlinScanner, ScannerState.PACKAGE_IMPORT, State.PACKAGE_IMPORT)
        }
    private val modifierListScanner = lazy { ModifierListScanner(kotlinScanner) }
    private val fileAnnotationListScanner = lazy { FileAnnotationListScanner(kotlinScanner) }
    private val returnScanner = lazy { ReturnScanner(kotlinScanner) }
    private val throwScanner = lazy { ThrowScanner(kotlinScanner) }
    private val tryScanner = lazy { TryScanner(kotlinScanner) }
    private val primaryConstructorScanner = lazy { PrimaryConstructorScanner(kotlinScanner) }
    private val propertyAccessorScanner = lazy { PropertyAccessorScanner(kotlinScanner) }
    private val parenthesizedScanner = lazy { ParenthesizedScanner(kotlinScanner) }
    private val labeledExpressionScanner = lazy { LabeledExpressionScanner(kotlinScanner) }
    private val annotatedExpressionScanner = lazy { AnnotatedExpressionScanner(kotlinScanner) }
    private val typeReferenceScanner = lazy { TypeReferenceScanner(kotlinScanner) }
    private val simpleScannerForBlock = lazy { SimpleScanner(kotlinScanner, ScannerState.BLOCK) }
    private val simpleBlockScanner =
        lazy { SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.CODE) }
    private val stringTemplateEntryScanner = lazy { StringTemplateEntryScanner() }
    private val simpleScanner = lazy { SimpleScanner(kotlinScanner, ScannerState.STATEMENT) }

    /**
     * Returns a [NodeScanner] to scan [ASTNode] of the given [elementType].
     *
     * The parameter [kotlinScanner] is a top-level scanner for all Kotlin constructions for use
     * when scanning [ASTNode] recursively.
     *
     * The parameter [importPolicy] is a function determining whether an import should be included
     * in the output; see [ImportListScanner].
     */
    internal fun nodeScannerForElementType(elementType: IElementType): NodeScanner =
        when (elementType) {
            KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY -> blockScanner.value
            KtNodeTypes.WHEN -> whenExpressionScanner.value
            KtNodeTypes.IF -> ifExpressionScanner.value
            KtNodeTypes.WHILE -> whileExpressionScanner.value
            KDocTokens.KDOC -> kDocScanner.value
            KtNodeTypes.STRING_TEMPLATE -> stringLiteralScanner.value
            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST ->
                parameterListScanner.value
            KtNodeTypes.TYPE_ARGUMENT_LIST -> typeArgumentListScanner.value
            KtNodeTypes.COLLECTION_LITERAL_EXPRESSION -> collectionLiteralExpressionScanner.value
            KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION -> classScanner.value
            KtNodeTypes.FUN, KtNodeTypes.SECONDARY_CONSTRUCTOR -> functionDeclarationScanner.value
            KtNodeTypes.CLASS_INITIALIZER -> classInitializerScanner.value
            KtNodeTypes.TYPEALIAS -> typealiasScanner.value
            KtNodeTypes.TYPE_PARAMETER_LIST -> typeParameterListScanner.value
            KtNodeTypes.TYPE_PARAMETER -> typeParameterScanner.value
            KtNodeTypes.ENUM_ENTRY -> enumEntryScanner.value
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION ->
                dotQualifiedExpressionScanner.value
            KtNodeTypes.FUNCTION_LITERAL -> functionLiteralScanner.value
            KtNodeTypes.CONDITION -> conditionScanner.value
            KtNodeTypes.FOR -> forExpressionScanner.value
            KtNodeTypes.DO_WHILE -> doWhileExpressionScanner.value
            KtNodeTypes.BINARY_EXPRESSION -> binaryExpressionScanner.value
            KtNodeTypes.CALL_EXPRESSION -> callExpressionScanner.value
            KtNodeTypes.PROPERTY -> propertyScanner.value
            KtNodeTypes.IMPORT_LIST -> importListScanner.value
            KtNodeTypes.PACKAGE_DIRECTIVE, KtNodeTypes.IMPORT_DIRECTIVE, KtNodeTypes.IMPORT_ALIAS ->
                simpleBlockScannerForPackageImport.value
            KtNodeTypes.MODIFIER_LIST -> modifierListScanner.value
            KtNodeTypes.RETURN -> returnScanner.value
            KtNodeTypes.THROW -> throwScanner.value
            KtNodeTypes.TRY -> tryScanner.value
            KtNodeTypes.PRIMARY_CONSTRUCTOR -> primaryConstructorScanner.value
            KtNodeTypes.PROPERTY_ACCESSOR -> propertyAccessorScanner.value
            KtNodeTypes.PARENTHESIZED -> parenthesizedScanner.value
            KtNodeTypes.LABELED_EXPRESSION -> labeledExpressionScanner.value
            KtNodeTypes.ANNOTATED_EXPRESSION -> annotatedExpressionScanner.value
            KtFileElementType.INSTANCE -> kotlinFileScanner.value
            is KtScriptElementType, KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY ->
                simpleScannerForBlock.value
            KtNodeTypes.FILE_ANNOTATION_LIST -> fileAnnotationListScanner.value
            KtNodeTypes.TYPE_REFERENCE -> typeReferenceScanner.value
            KtNodeTypes.WHEN_ENTRY, KtNodeTypes.ANNOTATION_ENTRY, KtNodeTypes.PREFIX_EXPRESSION,
                KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.SUPER_TYPE_ENTRY,
                KtNodeTypes.SUPER_TYPE_CALL_ENTRY, KtNodeTypes.USER_TYPE,
                KtNodeTypes.BINARY_WITH_TYPE, KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY,
                KtNodeTypes.IS_EXPRESSION, KtNodeTypes.ARRAY_ACCESS_EXPRESSION ->
                simpleBlockScanner.value
            KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY, KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY ->
                stringTemplateEntryScanner.value
            else -> simpleScanner.value
        }
}
