package org.kotlin.formatter.scanning

import java.util.function.Predicate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ElementType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.BlockFromMarkerToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.KDocContentToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.LiteralWhitespaceToken
import org.kotlin.formatter.MarkerToken
import org.kotlin.formatter.NonIndentingSynchronizedBreakToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.loading.KotlinFileLoader
import org.kotlin.formatter.nonBreakingSpaceToken

internal class KotlinScannerTest {
    private val kotlinLoader = KotlinFileLoader()

    @Test
    fun `outputs a LeafNodeToken on a LeafPsiElement`() {
        val node = LeafPsiElement(ElementType.IDENTIFIER, "variable")
        val subject = subject()

        val result = subject.scan(node)

        assertThat(result).isEqualTo(listOf(LeafNodeToken("variable")))
    }

    @Test
    fun `outputs a WhitespaceToken on whitespace`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("  variable")

        val result = subject.scan(node)

        assertThat(result).contains(WhitespaceToken("  "))
    }

    @Test
    fun `outputs ForcedBreakToken instead of whitespace at the end of the file`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(" ")

        val result = subject.scan(node)

        assertThat(result).contains(ForcedBreakToken(count = 1))
    }

    @Test
    fun `outputs a BeginToken, EndToken pair around a binary expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("(a + b)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("("),
                    BeginToken(State.CODE),
                    LeafNodeToken("a"),
                    LeafNodeToken("+"),
                    LeafNodeToken("b"),
                    EndToken,
                    LeafNodeToken(")")
                )
            )
    }

    @Test
    fun `outputs a WhitespaceToken after a binary operator`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a +b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("+"), WhitespaceToken(" "), LeafNodeToken("b"))
            )
    }

    @Test
    fun `outputs a non-breaking space token before binary operator`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(listOf(LeafNodeToken("a"), LeafNodeToken(" "), LeafNodeToken("+")))
    }

    @Test
    fun `does not output a WhitespaceToken before binary operator`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(LeafNodeToken("a"), WhitespaceToken(" "), LeafNodeToken("+"))
            )
    }

    @Test
    fun `outputs a WhitespaceToken after property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a =b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("="), WhitespaceToken(" "), LeafNodeToken("b"))
            )
    }

    @Test
    fun `outputs a non-breaking space token before property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a = b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(listOf(LeafNodeToken("a"), LeafNodeToken(" "), LeafNodeToken("=")))
    }

    @Test
    fun `does not output a WhitespaceToken before property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a = b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(LeafNodeToken("a"), WhitespaceToken(" "), LeafNodeToken("="))
            )
    }

    @Test
    fun `outputs a WhitespaceToken after function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() =b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("="), WhitespaceToken(" "), LeafNodeToken("b"))
            )
    }

    @Test
    fun `outputs a non-breaking space token before function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() = b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(listOf(LeafNodeToken("a"), LeafNodeToken(" "), LeafNodeToken("=")))
    }

    @Test
    fun `does not output a WhitespaceToken before function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() = b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(LeafNodeToken("a"), WhitespaceToken(" "), LeafNodeToken("="))
            )
    }

    @Test
    fun `outputs a BeginToken EndToken pair on token with children`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("function(arg1, arg2, arg3)")

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(listOf(BeginToken(State.CODE), EndToken))
    }

    @Test
    fun `replaces whitespace with single spaces when calculating length for BeginToken`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("(arg1  +  arg2  +  arg3)")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(State.CODE))
    }

    @Test
    fun `replaces synchronized break with single spaces when calculating length for BeginToken`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("function(arg1,  arg2,  arg3)")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(State.CODE))
    }

    @Test
    fun `does not replace whitespace in strings with single spaces for length calculation`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"A  string\"")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(State.STRING_LITERAL))
    }

    @Test
    fun `outputs a ForcedBreakToken at statement boundary`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    function1(arg1, arg2, arg3)
                    function2(arg1, arg2)
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("function1"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("function2")
            )
    }

    @Test
    fun `outputs a ForcedBreakToken at member boundary`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    class AClass {
                        fun function1() = 1
                        fun function2() = 2
                    }
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("function1"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("function2")
            )
    }

    @Test
    fun `does not output a ForcedBreakToken within a statement`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                function1(arg1,
                    arg2, arg3)
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("arg1"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("arg2")
            )
    }

    @Test
    fun `outputs two ForcedBreakTokens for vertical whitespace`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    function1(arg1, arg2, arg3)
                    
                    function2(arg1, arg2)
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("function1"),
                ForcedBreakToken(count = 2),
                LeafNodeToken("function2")
            )
    }

    @Test
    fun `outputs only two ForcedBreakTokens for more than one blank line in a row`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    function1(arg1, arg2, arg3)
                    
                    
                    function2(arg1, arg2)
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("function1"),
                ForcedBreakToken(count = 2),
                LeafNodeToken("function2")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken around { at beginning of class body`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                class MyClass {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("MyClass"),
                BeginToken(State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs ClosingSynchronizedBreakToken before closing brace of a class`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                class MyClass {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("{"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("}")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken around { at beginning of function body`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                fun myFunction() {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("myFunction"),
                LeafNodeToken("("),
                LeafNodeToken(")"),
                BeginToken(State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs a BeginBlock, EndBlock pair immediately inside a block`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    class MyClass {
                        val property: Int = 0
                    }
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("{"),
                    BeginToken(State.CODE),
                    ForcedBreakToken(count = 1),
                    LeafNodeToken("val"),
                    EndToken,
                    LeafNodeToken("}")
                )
            )
    }

    @Test
    fun `outputs ClosingSynchronizedBreakToken before closing brace of a block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                fun myFunction() {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("{"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("}")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken pair around body of if statement`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("if (aCondition) {}")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken(")"),
                BeginToken(State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `accepts an if statement with whitespace before the closing parenthesis`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("if (aCondition ) {}")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("if ("),
                LeafNodeToken("aCondition"),
                LeafNodeToken(")"),
                LeafNodeToken(" ")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken pair around body of while statement`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("while (aCondition) {}")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken(")"),
                BeginToken(State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `does not output a SynchronizedBreakToken at beginning of for loop expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("for (anEntry in aCollection) {}")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("("),
                SynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("anEntry")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken before ) on if condition`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("if (aCondition) {}")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.CODE),
                LeafNodeToken("aCondition"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")"),
                LeafNodeToken(" ")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken before ) on for expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("for (anEntry in aCollection) {}")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.CODE),
                LeafNodeToken("aCollection"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken before closing brace on function literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("map { aFunction() }")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken(")"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("}")
            )
    }

    @Test
    fun `does not output a WhitespaceToken before closing brace on function literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("map { aFunction() }")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(LeafNodeToken(")"), WhitespaceToken(" "), LeafNodeToken("}"))
    }

    @Test
    fun `outputs a SynchronizedBreakToken after opening brace on function literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("map {aFunction() }")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("{"),
                SynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("aFunction")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around when`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                when (variable) {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.CODE),
                LeafNodeToken("when "),
                LeafNodeToken("("),
                LeafNodeToken("variable"),
                LeafNodeToken(") "),
                LeafNodeToken("{"),
                LeafNodeToken("}"),
                EndToken
            )
    }

    @Test
    fun `handles a when without expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                when {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.CODE),
                LeafNodeToken("when "),
                LeafNodeToken("{"),
                LeafNodeToken("}"),
                EndToken
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken before ) on when expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("when (variable) {}")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.CODE),
                LeafNodeToken("when "),
                LeafNodeToken("variable"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(") "),
                EndToken
            )
    }

    @Test
    fun `outputs ClosingForcedBreakToken before closing brace of when`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                when (variable) {
                    1 -> {
                    }
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(LeafNodeToken("}"), ClosingForcedBreakToken, LeafNodeToken("}"))
    }

    @Test
    fun `outputs a ForcedBreakToken between KDoc and function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                /** Some KDoc */
                fun myFunction() {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken(" */"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("fun")
            )
    }

    @Test
    fun `suppresses whitespace between KDoc and function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                /** Some KDoc */
                fun myFunction() {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("*/"),
                WhitespaceToken("\n"),
                LeafNodeToken("fun")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around class declaration`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    class MyClass(val aParameter: Int) : AnInterface {
                        val something = "Something"
                    }
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                MarkerToken,
                LeafNodeToken("class"),
                LeafNodeToken("MyClass"),
                LeafNodeToken("AnInterface"),
                LeafNodeToken(" {"),
                NonIndentingSynchronizedBreakToken(whitespaceLength = 0),
                BlockFromMarkerToken
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around interface declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                interface AnInterface {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                MarkerToken,
                LeafNodeToken("interface"),
                LeafNodeToken("AnInterface"),
                nonBreakingSpaceToken(),
                LeafNodeToken("{"),
                BlockFromMarkerToken
            )
    }

    @Test
    fun `does not include terminating whitespace in function declaration block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                fun myFunction() {
                }
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken(")"),
                WhitespaceToken(" "),
                EndToken,
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for each parameter of a class constructor`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("class MyClass(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("param1"),
                SynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("param2")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken at end of class constructor parameters`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("class MyClass(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("param2"),
                LeafNodeToken("Int"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(")")
            )
    }

    @Test
    fun `does not output a ClosingSynchronizedBreakToken if no class constructor parameters`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("class MyClass()")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("("),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(")")
            )
    }

    @Test
    fun `does not output a ClosingSynchronizedBreakToken in an expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("(b + c) + d")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(
                    LeafNodeToken("c"),
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken("d")
                )
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for each parameter of a function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun myFunction(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("param1"),
                SynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("param2")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken at end of class function parameters`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun myFunction(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("param2"),
                LeafNodeToken("Int"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(")")
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for each parameter of a function call`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myFunction(param1, param2)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("param1"),
                SynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("param2")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken at end of call parameters`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myFunction(param1, param2)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("param2"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(")")
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for chained dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myObject.myProperty.myOtherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("myProperty"),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("myOtherProperty")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken around chained dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + myObject.myProperty.myOtherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("+"),
                BeginToken(State.CODE),
                LeafNodeToken("myObject"),
                LeafNodeToken("myProperty"),
                LeafNodeToken("myOtherProperty"),
                EndToken
            )
    }

    @Test
    fun `outputs BeginToken, EndToken around safe access dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + myObject?.myProperty?.myOtherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("+"),
                BeginToken(State.CODE),
                LeafNodeToken("myObject"),
                LeafNodeToken("myProperty"),
                LeafNodeToken("myOtherProperty"),
                EndToken
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for chained null-safe dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myNullableObject?.myProperty?.anotherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("myProperty"),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("anotherProperty")
            )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with state STRING_LITERAL on string literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(listOf(BeginToken(State.STRING_LITERAL), EndToken))
    }

    @Test
    fun `tokenizes strings in literal string template entries`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("A"), LiteralWhitespaceToken(" "), LeafNodeToken("string"))
            )
    }

    @Test
    fun `consolidates whitespace into a single LiteralWhitespaceToken in a string literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A  string"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("A"), LiteralWhitespaceToken("  "), LeafNodeToken("string"))
            )
    }

    @Test
    fun `outputs an empty LiteralWhitespaceToken between parts of a string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string${'$'}aVariable"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("string"),
                    LiteralWhitespaceToken(""),
                    LeafNodeToken("${'$'}aVariable")
                )
            )
    }

    @Test
    fun `outputs an empty LiteralWhitespaceToken between parts of a multiline string template`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin("""""${'"'}A string${'$'}aVariable""${'"'}""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("string"),
                    LiteralWhitespaceToken(""),
                    LeafNodeToken("${'$'}aVariable")
                )
            )
    }

    @Test
    fun `does not output an empty Whitespace at beginning of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string${'$'}aVariable"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(LeafNodeToken("\""), LiteralWhitespaceToken(""), LeafNodeToken("A"))
            )
    }

    @Test
    fun `does not output an empty Whitespace before start of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string${'$'}aVariable"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(WhitespaceToken(""), LeafNodeToken("\""), LeafNodeToken("A"))
            )
    }

    @Test
    fun `does not output an empty Whitespace before end of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string${'$'}aVariable"""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(LeafNodeToken("aVariable"), LiteralWhitespaceToken(""), LeafNodeToken("\""))
            )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with state MULTILINE_STRING_LITERAL on multiline`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"\"\"A string\"\"\"")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(listOf(BeginToken(State.MULTILINE_STRING_LITERAL), EndToken))
    }

    @Test
    fun `outputs a break and a LeafNodeToken on a multiline string literal with trimIndent`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"\"\"A string\"\"\".trimIndent()")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("\"\"\""),
                    BeginToken(State.CODE),
                    SynchronizedBreakToken(whitespaceLength = 0),
                    LeafNodeToken("A string"),
                    ClosingSynchronizedBreakToken(whitespaceLength = 0),
                    EndToken,
                    LeafNodeToken("\"\"\".trimIndent()")
                )
            )
    }

    @Test
    fun `outputs a forced break on newline in a multiline string template with trimIndent`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"\"\"A\nstring\"\"\".trimIndent()")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken("A"), ForcedBreakToken(count = 1), LeafNodeToken("string"))
            )
    }

    @Test
    fun `applies trimIndent to multiline string template content with trimIndent`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    ""${'"'}
                        A string
                    ""${'"'}.trimIndent()
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result).contains(LeafNodeToken("A string"))
    }

    @Test
    fun `does not output WhitespaceToken in a string literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(""""A string"""")

        val result = subject.scan(node)

        assertThat(result).areNot(Condition(Predicate { it is WhitespaceToken }, ""))
    }

    @Test
    fun `does not output WhitespaceToken in a multiline string literal`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    ""${'"'}
                        A line
                        Another line
                    ""${'"'}
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .areNot(Condition(Predicate { it is WhitespaceToken }, "A WhitespaceToken"))
    }

    @Test
    fun `outputs a BeginToken with state LINE_COMMENT on line comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("// A comment")

        val result = subject.scan(node)

        assertThat(result).containsAll(listOf(BeginToken(State.LINE_COMMENT), EndToken))
    }

    @Test
    fun `outputs individual tokens for words in a line comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("// A comment")

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(
                listOf(
                    LeafNodeToken("//"),
                    LiteralWhitespaceToken(" "),
                    LeafNodeToken("A"),
                    LiteralWhitespaceToken(" "),
                    LeafNodeToken("comment")
                )
            )
    }

    @Test
    fun `outputs a BeginToken with state TODO_COMMENT on line comment with TODO`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("// TODO: Something to be done")

        val result = subject.scan(node)

        assertThat(result).containsAll(listOf(BeginToken(State.TODO_COMMENT), EndToken))
    }

    @Test
    @Disabled("Holding off on implementation for now.")
    fun `outputs KDocContentToken in a block for a long comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/* A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/* "),
                KDocContentToken(content = "A comment"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `outputs KDocContentToken in a block for a KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** Some KDoc */")

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                KDocContentToken(content = "Some KDoc"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `supports links in KDoc comments`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** Some KDoc [AClass] */")

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                KDocContentToken(content = "Some KDoc [AClass]"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `outputs KDocContentToken without leading asterisks`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                /**
                 * Some KDoc
                 */
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                KDocContentToken(content = "Some KDoc"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `outputs KDocContentToken with intermediate newlines`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    /**
                     * Some KDoc
                     *
                     * Some more KDoc
                     */
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                KDocContentToken(content = "Some KDoc\n\nSome more KDoc"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `outputs KDocContentToken with tag content`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    /**
                     * @param parameter A parameter
                     *     with some more content
                     */
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSequence(
                BeginToken(State.CODE),
                LeafNodeToken("/**"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                KDocContentToken(
                    content = "@param parameter A parameter\n    with some more content"
                ),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken(" */"),
                EndToken
            )
    }

    @Test
    fun `outputs a ForcedLineBreak after a KDoc`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                /** Some KDoc. */
                class MyClass
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(LeafNodeToken(" */"), ForcedBreakToken(count = 1), LeafNodeToken("class"))
            )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair around a class which has KDoc`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin(
            """
                /** Some KDoc. */
                class MyClass
            """.trimIndent()
        )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken(" */"),
                    ForcedBreakToken(count = 1),
                    MarkerToken,
                    LeafNodeToken("class"),
                    LeafNodeToken("MyClass"),
                    BlockFromMarkerToken
                )
            )
    }

    @Test
    fun `outputs directives in PACKAGE_IMPORT state for package directive`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""package org.kotlin.formatter""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.PACKAGE_IMPORT),
                BeginToken(State.PACKAGE_IMPORT),
                LeafNodeToken("org"),
                EndToken,
                EndToken
            )
    }

    @Test
    fun `outputs directives in PACKAGE_IMPORT state for import directive`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""import org.kotlin.formatter.MyClass""".trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(State.PACKAGE_IMPORT),
                BeginToken(State.PACKAGE_IMPORT),
                LeafNodeToken("org"),
                EndToken,
                EndToken
            )
    }

    @Test
    fun `outputs a ForcedBreak between imports`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    import org.kotlin.formatter.AnotherClass
                    import org.kotlin.formatter.MyClass
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("AnotherClass"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("import")
            )
    }

    @Test
    fun `outputs a ForcedBreak with count 2 between package and import`() {
        val subject = subject()
        val node =
            kotlinLoader.parseKotlin(
                """
                    package org.kotlin.formatter
                    
                    import org.kotlin.formatter.package.AClass
                """.trimIndent()
            )

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("formatter"),
                ForcedBreakToken(count = 2),
                LeafNodeToken("import")
            )
    }

    private fun subject() = KotlinScanner({ _, _ -> true })
}
