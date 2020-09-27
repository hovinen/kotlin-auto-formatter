package org.kotlin.formatter.imports

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kotlin.formatter.loading.KotlinFileLoader

class KotlinImportPolicyTest {
    private val kotlinLoader = KotlinFileLoader()

    @Test
    fun `returns false on a name not present in the input`() {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "AClass")).isFalse()
    }

    @Test
    fun `returns true on a name present in the input as reference expression`() {
        val node = kotlinLoader.parseKotlin("val aVariable: AClass")

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.AClass")).isTrue()
    }

    @Test
    fun `returns true on a name which uses backticks`() {
        val node = kotlinLoader.parseKotlin("val aVariable = a `a function` b")

        val subject = importPolicyForNode(node)

        assertThat(subject("a function", "apackage.`a function`")).isTrue()
    }

    @Test
    fun `returns false on a name present in the input but not as reference expression`() {
        val node = kotlinLoader.parseKotlin("val aVariable: AClass")

        val subject = importPolicyForNode(node)

        assertThat(subject("aVariable", "")).isFalse()
    }

    @Test
    fun `returns false on a name present in the same package`() {
        val node =
            kotlinLoader.parseKotlin(
                """
                    package apackage
                    
                    val aVariable: AClass
                """.trimIndent()
            )

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.AClass")).isFalse()
    }

    @Test
    fun `returns false on a name present in the same package with subpackage`() {
        val node =
            kotlinLoader.parseKotlin(
                """
                    package apackage.asubpackage
                    
                    val aVariable: AClass
                """.trimIndent()
            )

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.asubpackage.AClass")).isFalse()
    }

    @Test
    fun `returns true on a name present in a subpackage of the file package`() {
        val node =
            kotlinLoader.parseKotlin(
                """
                    package apackage
                    
                    val aVariable: AClass
                """.trimIndent()
            )

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.asubpackage.AClass")).isTrue()
    }

    @Test
    fun `returns true on a name present in the same package if there is an alias`() {
        val node =
            kotlinLoader.parseKotlin(
                """
                    package apackage
                    
                    val aVariable: AClassAlias
                """.trimIndent()
            )

        val subject = importPolicyForNode(node)

        assertThat(subject("AClassAlias", "apackage.AClass")).isTrue()
    }

    @Test
    fun `returns false on a name present in the default package if in default package`() {
        val node = kotlinLoader.parseKotlin("""val aVariable: AClass""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "AClass")).isFalse()
    }

    @Test
    fun `returns false on a name present only in an import directive`() {
        val node = kotlinLoader.parseKotlin("""import apackage.asubpackage.AClass""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.asubpackage.AClass")).isFalse()
    }

    @Test
    fun `returns true on link present in KDoc`() {
        val node = kotlinLoader.parseKotlin("""/** [AClass] */""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.AClass")).isTrue()
    }

    @Test
    fun `returns true on wildcard import`() {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject("*", "apackage.*")).isTrue()
    }

    @Test
    fun `returns false on nested class referenced only with qualifier in KDoc`() {
        val node = kotlinLoader.parseKotlin("""/** [AClass.AnInnerClass] */""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AnInnerClass", "apackage.AClass.AnInnerClass")).isFalse()
    }

    @Test
    fun `returns true on outer class referencing inner class in KDoc`() {
        val node = kotlinLoader.parseKotlin("""/** [AClass.AnInnerClass] */""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.AClass")).isTrue()
    }

    @Test
    fun `returns true on a wildcard import`() {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject("*", "apackage.*")).isTrue()
    }

    @Test
    fun `returns false on class appearing only in KDoc link text`() {
        val node = kotlinLoader.parseKotlin("""/** [AClass][AnotherClass] */""".trimIndent())

        val subject = importPolicyForNode(node)

        assertThat(subject("AClass", "apackage.AClass")).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = [
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
    ])
    fun `returns true on operators`(operator: String) {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject(operator, "apackage.AClass.$operator")).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["component1", "component2", "component10"])
    fun `returns true on components`(component: String) {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject(component, "apackage.AClass.$component")).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["componentINVALID", "component"])
    fun `returns false on unreferenced non-components`(component: String) {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject(component, "apackage.AClass.$component")).isFalse()
    }

    @Test
    fun `returns true on provideDelegate if there is a by keyword in the file`() {
        val node = kotlinLoader.parseKotlin("val something: AClass by b")

        val subject = importPolicyForNode(node)

        assertThat(subject("provideDelegate", "org.gradle.kotlin.dsl.provideDelegate")).isTrue()
    }

    @Test
    fun `returns false on provideDelegate if there is no by keyword in the file`() {
        val node = kotlinLoader.parseKotlin("")

        val subject = importPolicyForNode(node)

        assertThat(subject("provideDelegate", "org.gradle.kotlin.dsl.provideDelegate")).isFalse()
    }
}
