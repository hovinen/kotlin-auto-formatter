package org.kotlin.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.max

class KotlinFormatterTest {
    @Test
    fun `format breaks line at assignment operator in local variable`() {
        val result = KotlinFormatter(maxLineLength = 55).format("""
            fun main() {
                val aValue = ALongMethodCall(aParameter, anotherParameter)
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun main() {
                val aValue =
                    ALongMethodCall(aParameter, anotherParameter)
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks line at assignment operator in global property`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            val aValue = ALongMethodCall(aParameter, anotherParameter)
        """.trimIndent())

        assertThat(result).isEqualTo("""
            val aValue =
                ALongMethodCall(aParameter, anotherParameter)
        """.trimIndent())
    }

    @Test
    fun `format breaks line at assignment operator when whitespace after assignment is missing`() {
        val result = KotlinFormatter(maxLineLength = 55).format("""
            fun main() {
                val aValue =ALongMethodCall(aParameter, anotherParameter)
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun main() {
                val aValue =
                    ALongMethodCall(aParameter, anotherParameter)
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks line at parameters in parameter list`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun main() {
                ALongMethodCall(aParameter, anotherParameter, aThirdParameter)
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun main() {
                ALongMethodCall(
                    aParameter,
                    anotherParameter,
                    aThirdParameter
                )
            }
        """.trimIndent())
    }

    @Test
    fun `format removes excess whitespace before closing parentheses`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun main() {
                ALongMethodCall(aParameter, anotherParameter, aThirdParameter )
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun main() {
                ALongMethodCall(
                    aParameter,
                    anotherParameter,
                    aThirdParameter
                )
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks line at class constructor parameter list`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            class ALongClass(aParameter: String, anotherParameter: String, aThirdParameter: String)
        """.trimIndent())

        assertThat(result).isEqualTo("""
            class ALongClass(
                aParameter: String,
                anotherParameter: String,
                aThirdParameter: String
            )
        """.trimIndent())
    }

    @Test
    fun `format does not break before a class declaration based on length of body`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            internal class ALongClass(aParameter: String, anotherParameter: String, aThirdParameter: String) {
                fun aFunction() {
                    aFunctionCall()
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            internal class ALongClass(
                aParameter: String,
                anotherParameter: String,
                aThirdParameter: String
            ) {
                fun aFunction() {
                    aFunctionCall()
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format does not indent a class after a class after a package declaration`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            package apackage

            class AClass

            class AnotherClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            package apackage

            class AClass

            class AnotherClass
        """.trimIndent())
    }

    @Test
    fun `format breaks at parameters of function declarations`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun aFunction(aParameter: String, anotherParameter: String, aThirdParameter: String) {
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun aFunction(
                aParameter: String,
                anotherParameter: String,
                aThirdParameter: String
            ) {
            }
        """.trimIndent())
    }

    @Test
    fun `does not break around parameters based on the length of the function initializer`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun aFunction(aParameter: String) = aFunctionCall().anotherFunctionCall()
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun aFunction(aParameter: String) =
                aFunctionCall().anotherFunctionCall()
        """.trimIndent())
    }

    @Test
    fun `does not break around parameters based on the length of the KDoc`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            /** Some long KDoc which should not wrap */
            fun aFunction(aParameter: String) {
                aFunctionCall().anotherFunctionCall()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some long KDoc which should not wrap */
            fun aFunction(aParameter: String) {
                aFunctionCall().anotherFunctionCall()
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks a property before its getter`() {
        val result = KotlinFormatter(maxLineLength = 55).format("""
            class AClass {
                val aProperty: String get() = "This value is too long for one line"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            class AClass {
                val aProperty: String
                    get() = "This value is too long for one line"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks a property before its setter`() {
        val result = KotlinFormatter(maxLineLength = 80).format("""
            class AClass {
                var aProperty: AType = aValue
                    private set
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            class AClass {
                var aProperty: AType = aValue
                    private set
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks a parameter list preceded by a blank line`() {
        val result = KotlinFormatter(maxLineLength = 55).format("""
            class MyClass {

                fun aFunction(aParameter: String, anotherParameter: String, aThirdParameter: String) {
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            class MyClass {

                fun aFunction(
                    aParameter: String,
                    anotherParameter: String,
                    aThirdParameter: String
                ) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun `breaks between annotations and multiline declarations`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            @AnAnnotation
            fun myFunction() {
                aFunctionCall()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            @AnAnnotation
            fun myFunction() {
                aFunctionCall()
            }
        """.trimIndent())
    }

    @Test
    fun `breaks between different annotations`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            @AnAnnotation
            @AnotherAnnotation
            val aProperty: String
        """.trimIndent())

        assertThat(result).isEqualTo("""
            @AnAnnotation
            @AnotherAnnotation
            val aProperty: String
        """.trimIndent())
    }

    @Test
    fun `does not break before property name in extension property`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            val AClass.aProperty: String
                get() =
                    aFunction(aParameter, anotherParameter)
        """.trimIndent())

        assertThat(result).isEqualTo("""
            val AClass.aProperty: String
                get() =
                    aFunction(aParameter, anotherParameter)
        """.trimIndent())
    }

    @Test
    fun `includes space between modifiers and property`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            private val aProperty: String = "A long string which should wrap"
        """.trimIndent())

        assertThat(result).isEqualTo("""
            private val aProperty: String =
                "A long string which should wrap"
        """.trimIndent())
    }

    @Test
    fun `includes space between modifiers and function`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            private fun aFunction(): String = "A long string which should wrap"
        """.trimIndent())

        assertThat(result).isEqualTo("""
            private fun aFunction(): String =
                "A long string which should wrap"
        """.trimIndent())
    }

    @Test
    fun `indents property initializer for function when inside a class`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            class MyClass {
                private fun aFunction(): String = "A long string which should wrap"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            class MyClass {
                private fun aFunction(): String =
                    "A long string which should wrap"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks at logical operator in an if statement`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                if (aLongCondition && anotherLongCondition && yetAnotherLongCondition) {
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                if (aLongCondition && anotherLongCondition &&
                    yetAnotherLongCondition
                ) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks at logical operator in a while statement`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                while (aLongCondition && anotherLongCondition && yetAnotherLongCondition) {
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                while (aLongCondition && anotherLongCondition &&
                    yetAnotherLongCondition
                ) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks at an operator within a when statement`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                when (aLongExression + anotherLongExpression + yetAnotherLongExpression) {
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                when (aLongExression + anotherLongExpression +
                    yetAnotherLongExpression
                ) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks at in operator in for statement`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                for (aLongVariableName in aCollectionWithALongName) {
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                for (aLongVariableName in
                    aCollectionWithALongName
                ) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks before chained calls`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                aVariable.aMethod().anotherMethod().aThirdMethod()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                aVariable.aMethod()
                    .anotherMethod()
                    .aThirdMethod()
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks before chained calls with null check`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                aVariable.aMethod()?.anotherMethod()?.aThirdMethod()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                aVariable.aMethod()
                    ?.anotherMethod()
                    ?.aThirdMethod()
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                aVariable = aValue + anotherValue + yetAnotherValue + andYetAnotherValue
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                aVariable =
                    aValue + anotherValue + yetAnotherValue +
                        andYetAnotherValue
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators inside if statements`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                if (aCondition) {
                    aVariable = aValue + anotherValue + yetAnotherValue
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                if (aCondition) {
                    aVariable =
                        aValue + anotherValue +
                            yetAnotherValue
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators inside while statements`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                while (aCondition) {
                    aVariable = aValue + anotherValue + yetAnotherValue
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                while (aCondition) {
                    aVariable =
                        aValue + anotherValue +
                            yetAnotherValue
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators inside for loops`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                for (anEntry in aCollection) {
                    aVariable = aValue + anotherValue + yetAnotherValue
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                for (anEntry in aCollection) {
                    aVariable =
                        aValue + anotherValue +
                            yetAnotherValue
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators inside first when entry`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                when (aCondition) {
                    1 -> {
                        aVariable = aValue + anotherValue + yetAnotherValue
                    }
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                when (aCondition) {
                    1 -> {
                        aVariable =
                            aValue + anotherValue +
                                yetAnotherValue
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators inside a second when entry`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                when (aCondition) {
                    1 -> {
                        aVariable = aValue
                    }
                    2 -> {
                        aVariable = aValue + anotherValue + yetAnotherValue
                    }
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                when (aCondition) {
                    1 -> {
                        aVariable = aValue
                    }
                    2 -> {
                        aVariable =
                            aValue + anotherValue +
                                yetAnotherValue
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions at operators leaving operations at the end`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                aVariable = aValue + anotherValue + yetAnotherValue + andYetAnotherValue + moreStuff
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                aVariable =
                    aValue + anotherValue + yetAnotherValue +
                        andYetAnotherValue + moreStuff
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks function literal after arguments`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            val aFunction = { aParameter, anotherParameter ->
                anotherFunction()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            val aFunction =
                { aParameter, anotherParameter ->
                    anotherFunction()
                }
        """.trimIndent())
    }

    @Test
    fun `format breaks strings at a single word boundary when possible`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "ALongStringInitializerWhichShould wrapToANewLine"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "ALongStringInitializerWhichShould " +
                        "wrapToANewLine"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks strings at multiple word boundaries when possible`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "ALongStringInitializerWhichShould wrapToANewLineAndWrappedAgain whereNecessary"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "ALongStringInitializerWhichShould " +
                        "wrapToANewLineAndWrappedAgain " +
                        "whereNecessary"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks strings only at a suitable word boundary`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "A long string initializer which should wrap to a new line"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "A long string initializer which should" +
                        " wrap to a new line"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks strings at multiple lines only at a suitable word boundary`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "A long string initializer which should wrap to a new line and wrap again"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "A long string initializer which should" +
                        " wrap to a new line and wrap again"
            }
        """.trimIndent())
    }

    @Test
    fun `format does not break a string template inside an expression`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "ALongStringInitializerWhichShouldWrap${'$'}{someExpression}"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "ALongStringInitializerWhichShouldWrap" +
                        "${'$'}{someExpression}"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks strings which include template variables`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = "A long string initializer with ${'$'}variable should wrap"
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    "A long string initializer with " +
                        "${'$'}variable should wrap"
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks expressions recursively when required`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                val aNewVariable = aVariable.aMethod()?.anotherMethod()?.aThirdMethod()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                val aNewVariable =
                    aVariable.aMethod()
                        ?.anotherMethod()
                        ?.aThirdMethod()
            }
        """.trimIndent())
    }

    @Test
    fun `does not break between return and return value`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                return aVariable.aMethod().anotherMethod().aThirdMethod()
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                return aVariable.aMethod()
                    .anotherMethod()
                    .aThirdMethod()
            }
        """.trimIndent())
    }

    @Test
    fun `does not include trailing whitespace on naked return`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                return
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                return
            }
        """.trimIndent())
    }

    @Test
    fun `does not break between else and opening brace of block`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                if (aCondition) {
                    doSomething()
                } else {
                    doSomethingElse().andThenSomethingElse().andThenSomethingElse()
                }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                if (aCondition) {
                    doSomething()
                } else {
                    doSomethingElse().andThenSomethingElse()
                        .andThenSomethingElse()
                }
            }
        """.trimIndent())
    }

    @Test
    fun `does not break between function and closure parameter`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                aMethodWithLambda { doSomething().doSomethingElse() }
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                aMethodWithLambda {
                    doSomething().doSomethingElse()
                }
            }
        """.trimIndent())
    }

    @Test
    fun `does not insert a newline between throw and exception`() {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                throw AnException("A long exception message which wraps")
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                throw AnException(
                    "A long exception message which wraps"
                )
            }
        """.trimIndent())
    }

    @Test
    fun `does not insert whitespace into an empty block`() {
        val result = KotlinFormatter().format("""
            val object = MyInterface {}
        """.trimIndent())

        assertThat(result).isEqualTo("""
            val object = MyInterface {}
        """.trimIndent())
    }

    @ParameterizedTest
    @ValueSource(strings = [".", "?."])
    fun `does not break on a single dot expression`(operator: String) {
        val result = KotlinFormatter(maxLineLength = 50).format("""
            fun myFunction() {
                anObject${operator}aMethod(aParameter, anotherParameter, aThirdParameter)
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            fun myFunction() {
                anObject${operator}aMethod(
                    aParameter,
                    anotherParameter,
                    aThirdParameter
                )
            }
        """.trimIndent())
    }

    @Test
    fun `format breaks KDoc comment text`() {
        val result = KotlinFormatter(maxLineLength = 60).format("""
            /**
             * An extra long summary fragment which should wrap to a new line.
             */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * An extra long summary fragment which should wrap to a new
             * line.
             */
        """.trimIndent())
    }

    @Test
    fun `format breaks the short form of the summary fragment`() {
        val result = KotlinFormatter(maxLineLength = 60).format("""
            /** An extra long summary fragment which should wrap to a new line. */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * An extra long summary fragment which should wrap to a new
             * line.
             */
        """.trimIndent())
    }

    @Test
    fun `format does not break if there is no word boundary`() {
        val result = KotlinFormatter(maxLineLength = 60).format("""
            /**
             * http://www.example.com/an-extra-long-url-which-should-not-break
             */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * http://www.example.com/an-extra-long-url-which-should-not-break
             */
        """.trimIndent())
    }

    @Test
    fun `format indents with a continuation indent when breaking at a tag`() {
        val result = KotlinFormatter(maxLineLength = 60).format("""
            /**
             * @param parameter an input parameter with a particularly long description
             */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * @param parameter an input parameter with a particularly
             *     long description
             */
        """.trimIndent())
    }

    @Test
    fun `format consolidates continuation indentation in KDoc tags`() {
        val result = KotlinFormatter(maxLineLength = 75).format("""
            /**
             * @param parameter an input parameter with a particularly
             *     long description
             */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * @param parameter an input parameter with a particularly long description
             */
        """.trimIndent())
    }

    @Test
    fun `format preserves newlines between directives`() {
        val result = KotlinFormatter(maxLineLength = 60).format("""
            /**
             * @param parameter an input parameter with a particularly long description
             * @param anotherParameter another input parameter
             */
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * @param parameter an input parameter with a particularly
             *     long description
             * @param anotherParameter another input parameter
             */
        """.trimIndent())
    }

    @Test
    fun `does not insert line breaks in the package statement`() {
        val result = KotlinFormatter(maxLineLength = 20).format("""
            package org.kotlin.a.very.long.package.name.which.should.not.wrap
        """.trimIndent())

        assertThat(result).isEqualTo("""
            package org.kotlin.a.very.long.package.name.which.should.not.wrap
        """.trimIndent())
    }

    @Test
    fun `does not insert line breaks in an import statement`() {
        val subject = KotlinFormatter(maxLineLength = 20)

        val result = subject.format("""
            import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass
        """.trimIndent())
    }

    @Test
    fun `does not insert line breaks in an import statement with alias`() {
        val subject = KotlinFormatter(maxLineLength = 20)

        val result = subject.format("""
            import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass as AnAlias
        """.trimIndent())

        assertThat(result).isEqualTo("""
            import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass as AnAlias
        """.trimIndent())
    }

    @Test
    fun `preserves single line breaks between import statements`() {
        val subject = KotlinFormatter()

        val result = subject.format("""
            import org.kotlin.formatter.AClass
            import org.kotlin.formatter.AnotherClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            import org.kotlin.formatter.AClass
            import org.kotlin.formatter.AnotherClass
        """.trimIndent())
    }
    
    @Test
    fun `does not indent import statements after a package statement`() {
        val subject = KotlinFormatter()

        val result = subject.format("""
            package org.kotlin.formatter
            
            import org.kotlin.formatter.package.AClass
        """.trimIndent())
        
        assertThat(result).isEqualTo("""
            package org.kotlin.formatter
            
            import org.kotlin.formatter.package.AClass
        """.trimIndent())
    }

    @Test
    fun `does not indent top level class declaration`() {
        val subject = KotlinFormatter()

        val result = subject.format("""
            package org.kotlin.formatter
            
            class MyClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            package org.kotlin.formatter
            
            class MyClass
        """.trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and class declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            /** Some KDoc. */
            class MyClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some KDoc. */
            class MyClass
        """.trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and object declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            /** Some KDoc. */
            object MyObject
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some KDoc. */
            object MyObject
        """.trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and function declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            /** Some KDoc. */
            fun aFunction()
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some KDoc. */
            fun aFunction()
        """.trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and property declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            /** Some KDoc. */
            val aProperty
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some KDoc. */
            val aProperty
        """.trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and enum value declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            enum class AnEnum {
                /** Some KDoc. */
                A_VALUE
            }
        """.trimIndent())

        assertThat(result).isEqualTo("""
            enum class AnEnum {
                /** Some KDoc. */
                A_VALUE
            }
        """.trimIndent())
    }

    @Test
    fun `places an initial asterix on blank KDoc lines`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""
            /**
             * Some KDoc.
             *
             * Some further explanation.
             */
            class AClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /**
             * Some KDoc.
             *
             * Some further explanation.
             */
            class AClass
        """.trimIndent())
    }

    @Test
    fun `maintains spacing between KDoc elements`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result = subject.format("""
            /** Some KDoc with an [element] and more text. */
            class AClass
        """.trimIndent())

        assertThat(result).isEqualTo("""
            /** Some KDoc with an [element] and more text. */
            class AClass
        """.trimIndent())
    }

    @Test
    fun `does not strip trailing whitespace in multiline string literals`() {
        val subject = KotlinFormatter()

        val result = subject.format("""
            ""${'"'}Before whitespace  
            After whitespace""${'"'}
        """.trimIndent())

        assertThat(result).isEqualTo("""
            ""${'"'}Before whitespace  
            After whitespace""${'"'}
        """.trimIndent())
    }

    @Test
    fun `resets state after every file`() {
        val subject = KotlinFormatter()
        subject.format("package org.kotlin.formatter")

        val result = subject.format("package org.kotlin.formatter")

        assertThat(result).isEqualTo("package org.kotlin.formatter")
    }
}
