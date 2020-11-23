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

/**
 * Provides the correct [NodeScanner] to scan a given [ASTNode] while in a given [ScannerState].
 */
internal class NodeScannerProvider(
    private val kotlinScanner: KotlinScanner,
    private val importPolicy: (String, String) -> Boolean
) {
    private val blockScanner = BlockScanner(kotlinScanner)
    private val whenExpressionScanner = WhenExpressionScanner(kotlinScanner)
    private val ifExpressionScanner = IfExpressionScanner(kotlinScanner)
    private val whileExpressionScanner = WhileExpressionScanner(kotlinScanner)
    private val kDocScanner = KDocScanner(kotlinScanner)
    private val stringLiteralScanner = StringLiteralScanner(kotlinScanner)
    private val parameterListScanner = ParameterListScanner(kotlinScanner)
    private val typeArgumentListScanner = TypeArgumentListScanner(kotlinScanner)
    private val collectionLiteralExpressionScanner =
        CollectionLiteralExpressionScanner(kotlinScanner)
    private val classScanner = ClassScanner(kotlinScanner)
    private val functionDeclarationScanner = FunctionDeclarationScanner(kotlinScanner)
    private val classInitializerScanner = ClassInitializerScanner(kotlinScanner)
    private val typealiasScanner = TypealiasScanner(kotlinScanner)
    private val enumEntryScanner = EnumEntryScanner(kotlinScanner)
    private val dotQualifiedExpressionScanner = DotQualifiedExpressionScanner(kotlinScanner)
    private val functionLiteralScanner = FunctionLiteralScanner(kotlinScanner)
    private val conditionScanner = ConditionScanner(kotlinScanner)
    private val forExpressionScanner = ForExpressionScanner(kotlinScanner)
    private val binaryExpressionScanner = BinaryExpressionScanner(kotlinScanner)
    private val callExpressionScanner = CallExpressionScanner(kotlinScanner)
    private val propertyScanner = PropertyScanner(kotlinScanner)
    private val importListScanner = ImportListScanner(kotlinScanner, importPolicy)
    private val simpleBlockScannerForPackageImport =
        SimpleBlockScanner(kotlinScanner, ScannerState.PACKAGE_IMPORT, State.PACKAGE_IMPORT)
    private val modifierListScanner = ModifierListScanner(kotlinScanner)
    private val returnScanner = ReturnScanner(kotlinScanner)
    private val throwScanner = ThrowScanner(kotlinScanner)
    private val tryScanner = TryScanner(kotlinScanner)
    private val primaryConstructorScanner = PrimaryConstructorScanner(kotlinScanner)
    private val propertyAccessorScanner = PropertyAccessorScanner(kotlinScanner)
    private val parenthesizedScanner = ParenthesizedScanner(kotlinScanner)
    private val labeledExpressionScanner = LabeledExpressionScanner(kotlinScanner)
    private val annotatedExpressionScanner = AnnotatedExpressionScanner(kotlinScanner)
    private val simpleScannerForBlock = SimpleScanner(kotlinScanner, ScannerState.BLOCK)
    private val simpleBlockScanner =
        SimpleBlockScanner(kotlinScanner, ScannerState.STATEMENT, State.CODE)
    private val stringTemplateEntryScanner = StringTemplateEntryScanner()
    private val simpleScanner = SimpleScanner(kotlinScanner, ScannerState.STATEMENT)

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
            KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY -> blockScanner
            KtNodeTypes.WHEN -> whenExpressionScanner
            KtNodeTypes.IF -> ifExpressionScanner
            KtNodeTypes.WHILE -> whileExpressionScanner
            KDocTokens.KDOC -> kDocScanner
            KtNodeTypes.STRING_TEMPLATE -> stringLiteralScanner
            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST ->
                parameterListScanner
            KtNodeTypes.TYPE_ARGUMENT_LIST -> typeArgumentListScanner
            KtNodeTypes.COLLECTION_LITERAL_EXPRESSION -> collectionLiteralExpressionScanner
            KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION -> classScanner
            KtNodeTypes.FUN, KtNodeTypes.SECONDARY_CONSTRUCTOR -> functionDeclarationScanner
            KtNodeTypes.CLASS_INITIALIZER -> classInitializerScanner
            KtNodeTypes.TYPEALIAS -> typealiasScanner
            KtNodeTypes.ENUM_ENTRY -> enumEntryScanner
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION ->
                dotQualifiedExpressionScanner
            KtNodeTypes.FUNCTION_LITERAL -> functionLiteralScanner
            KtNodeTypes.CONDITION -> conditionScanner
            KtNodeTypes.FOR -> forExpressionScanner
            KtNodeTypes.BINARY_EXPRESSION -> binaryExpressionScanner
            KtNodeTypes.CALL_EXPRESSION -> callExpressionScanner
            KtNodeTypes.PROPERTY -> propertyScanner
            KtNodeTypes.IMPORT_LIST -> importListScanner
            KtNodeTypes.PACKAGE_DIRECTIVE, KtNodeTypes.IMPORT_DIRECTIVE, KtNodeTypes.IMPORT_ALIAS ->
                simpleBlockScannerForPackageImport
            KtNodeTypes.MODIFIER_LIST -> modifierListScanner
            KtNodeTypes.RETURN -> returnScanner
            KtNodeTypes.THROW -> throwScanner
            KtNodeTypes.TRY -> tryScanner
            KtNodeTypes.PRIMARY_CONSTRUCTOR -> primaryConstructorScanner
            KtNodeTypes.PROPERTY_ACCESSOR -> propertyAccessorScanner
            KtNodeTypes.PARENTHESIZED -> parenthesizedScanner
            KtNodeTypes.LABELED_EXPRESSION -> labeledExpressionScanner
            KtNodeTypes.ANNOTATED_EXPRESSION -> annotatedExpressionScanner
            KtFileElementType.INSTANCE, is KtScriptElementType,
                KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY -> simpleScannerForBlock
            KtNodeTypes.WHEN_ENTRY, KtNodeTypes.ANNOTATION_ENTRY, KtNodeTypes.PREFIX_EXPRESSION,
                KtNodeTypes.VALUE_PARAMETER, KtNodeTypes.SUPER_TYPE_ENTRY,
                KtNodeTypes.SUPER_TYPE_CALL_ENTRY, KtNodeTypes.USER_TYPE,
                KtNodeTypes.BINARY_WITH_TYPE -> simpleBlockScanner
            KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY, KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY ->
                stringTemplateEntryScanner
            else -> simpleScanner
        }
}
