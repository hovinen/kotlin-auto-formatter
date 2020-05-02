package org.kotlin.formatter.scanning

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ElementType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.junit.jupiter.api.Test
import org.kotlin.formatter.BeginToken
import org.kotlin.formatter.ClosingForcedBreakToken
import org.kotlin.formatter.ClosingSynchronizedBreakToken
import org.kotlin.formatter.EndToken
import org.kotlin.formatter.ForcedBreakToken
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.State
import org.kotlin.formatter.SynchronizedBreakToken
import org.kotlin.formatter.WhitespaceToken
import org.kotlin.formatter.loading.KotlinFileLoader

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

        assertThat(result).contains(WhitespaceToken(length = 9, content = "  "))
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
                    BeginToken(length = 5, state = State.CODE),
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
                listOf(
                    LeafNodeToken("+"),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("b")
                )
            )
    }

    @Test
    fun `outputs a non-breaking space token before binary operator`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    LeafNodeToken(" "),
                    LeafNodeToken("+")
                )
            )
    }

    @Test
    fun `does not output a WhitespaceToken before binary operator`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("a + b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("+")
                )
            )
    }

    @Test
    fun `outputs a WhitespaceToken after property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a =b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("="),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("b")
                )
            )
    }

    @Test
    fun `outputs a non-breaking space token before property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a = b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    LeafNodeToken(" "),
                    LeafNodeToken("=")
                )
            )
    }

    @Test
    fun `does not output a WhitespaceToken before property assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("val a = b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("=")
                )
            )
    }

    @Test
    fun `outputs a WhitespaceToken after function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() =b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("="),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("b")
                )
            )
    }

    @Test
    fun `outputs a non-breaking space token before function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() = b")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    LeafNodeToken(" "),
                    LeafNodeToken("=")
                )
            )
    }

    @Test
    fun `does not output a WhitespaceToken before function assignment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("fun a() = b")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                listOf(
                    LeafNodeToken("a"),
                    WhitespaceToken(length = 2, content = " "),
                    LeafNodeToken("=")
                )
            )
    }

    @Test
    fun `outputs a BeginToken EndToken pair on token with children`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("function(arg1, arg2, arg3)")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(listOf(BeginToken(length = 26, state = State.CODE), EndToken))
    }

    @Test
    fun `replaces whitespace with single spaces when calculating length for BeginToken`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("(arg1  +  arg2  +  arg3)")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(length = 20, state = State.CODE))
    }

    @Test
    fun `replaces synchronized break with single spaces when calculating length for BeginToken`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("function(arg1,  arg2,  arg3)")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(length = 26, state = State.CODE))
    }

    @Test
    fun `does not replace whitespace in strings with single spaces for length calculation`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"A  string\"")

        val result = subject.scan(node)

        assertThat(result).contains(BeginToken(length = 11, state = State.STRING_LITERAL))
    }

    @Test
    fun `outputs a ForcedBreakToken at statement boundary`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            function1(arg1, arg2, arg3)
            function2(arg1, arg2)
        """)

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
        val node = kotlinLoader.parseKotlin("""
            class AClass {
                fun function1() = 1
                fun function2() = 2
            }
        """)

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
        val node = kotlinLoader.parseKotlin("""
            function1(arg1,
                arg2, arg3)
        """)

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
        val node = kotlinLoader.parseKotlin("""
            function1(arg1, arg2, arg3)

            function2(arg1, arg2)
        """)

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
        val node = kotlinLoader.parseKotlin("""
            function1(arg1, arg2, arg3)


            function2(arg1, arg2)
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("function1"),
                ForcedBreakToken(count = 2),
                LeafNodeToken("function2")
            )
    }

    @Test
    fun `outputs a whitespace with length based on first token`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            class MyClass {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("MyClass"),
                WhitespaceToken(length = 1, content = " "),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken around { at beginning of class body`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            class MyClass {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("MyClass"),
                BeginToken(length = 2, state = State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs ClosingForcedBreakToken before closing brace of a class`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            class MyClass {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("{"),
                ClosingForcedBreakToken,
                LeafNodeToken("}")
            )
    }

    @Test
    fun `does not output a BeginToken, EndToken around { at beginning of function body`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("myFunction"),
                LeafNodeToken("("),
                LeafNodeToken(")"),
                BeginToken(length = 2, state = State.CODE),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs a BeginBlock, EndBlock pair immediately inside a block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            class MyClass {
                val property: Int = 0
            }
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    LeafNodeToken("{"),
                    BeginToken(length = 21, state = State.CODE),
                    ForcedBreakToken(count = 1),
                    LeafNodeToken("val"),
                    EndToken,
                    LeafNodeToken("}")
                )
            )
    }

    @Test
    fun `outputs ClosingForcedBreakToken before closing brace of a block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("{"),
                ClosingForcedBreakToken,
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
                BeginToken(length = 2, state = State.CODE),
                LeafNodeToken("{")
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
                BeginToken(length = 2, state = State.CODE),
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
                BeginToken(length = 10, state = State.CODE),
                LeafNodeToken("aCondition"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(") ")
            )
    }

    @Test
    fun `outputs a ClosingSynchronizedBreakToken before ) on for expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("for (anEntry in aCollection) {}")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(length = 22, state = State.CODE),
                LeafNodeToken("aCollection"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around when`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            when (variable) {
            }
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(length = 18, state = State.CODE),
                LeafNodeToken("when"),
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
                BeginToken(length = 8, state = State.CODE),
                LeafNodeToken("variable"),
                ClosingSynchronizedBreakToken(whitespaceLength = 0),
                EndToken,
                LeafNodeToken(")")
            )
    }

    @Test
    fun `outputs ClosingForcedBreakToken before closing brace of when`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            when (variable) {
                1 -> {
                }
            }
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("}"),
                ClosingForcedBreakToken,
                LeafNodeToken("}")
            )
    }

    @Test
    fun `does not output ForcedBreakToken before closing brace of a block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("{"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("}")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(length = 16, state = State.CODE),
                LeafNodeToken("fun"),
                LeafNodeToken("myFunction"),
                LeafNodeToken("("),
                LeafNodeToken(")"),
                EndToken,
                LeafNodeToken(" "),
                LeafNodeToken("{")
            )
    }

    @Test
    fun `outputs a ForcedBreakToken between KDoc and function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /** Some KDoc */
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("*/"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("fun")
            )
    }

    @Test
    fun `suppresses whitespace between KDoc and function declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /** Some KDoc */
            fun myFunction() {
            }
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("*/"),
                WhitespaceToken(length = 4, content = "\n"),
                LeafNodeToken("fun")
            )
    }

    @Test
    fun `outputs BeginToken, EndToken pair around class declaration`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            class MyClass(val aParameter: Int) : AnInterface {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                BeginToken(length = 49, state = State.CODE),
                LeafNodeToken("class"),
                LeafNodeToken("MyClass"),
                LeafNodeToken("AnInterface"),
                WhitespaceToken(length = 1, content = " "),
                EndToken,
                LeafNodeToken("{")
            )
    }

    @Test
    fun `does not include terminating whitespace in function declaration block`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            fun myFunction() {
            }
        """)

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken(")"),
                WhitespaceToken(length = 2, content = " "),
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
    fun `does not output WhitespaceToken between parameters of a class constructor`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("class MyClass(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("param1"),
                WhitespaceToken(length = 12, content = " "),
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
    fun `does not output BeginToken, EndToken pair for class constructor`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("class MyClass(param1: Int, param2: Int)")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("MyClass"),
                BeginToken(length = 26, state = State.CODE),
                LeafNodeToken("("),
                LeafNodeToken(")"),
                EndToken
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
                LeafNodeToken("myObject"),
                SynchronizedBreakToken(whitespaceLength = 0),
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
                BeginToken(length = 35, state = State.CODE),
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
                BeginToken(length = 37, state = State.CODE),
                LeafNodeToken("myObject"),
                LeafNodeToken("myProperty"),
                LeafNodeToken("myOtherProperty"),
                EndToken
            )
    }

    @Test
    fun `does not output BeginToken, EndToken pair within chained dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myObject.myProperty.myOtherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                BeginToken(length = 19, state = State.CODE),
                LeafNodeToken("myObject")
            )
    }

    @Test
    fun `does not output BeginToken, EndToken pair within chained safe access expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myObject?.myProperty?.myOtherProperty")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                BeginToken(length = 20, state = State.CODE),
                LeafNodeToken("myObject")
            )
    }

    @Test
    fun `outputs SynchronizedBreakToken for chained null-safe dot expression`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("myNullableObject?.myProperty")

        val result = subject.scan(node)

        assertThat(result)
                .containsSubsequence(
                LeafNodeToken("myNullableObject"),
                SynchronizedBreakToken(whitespaceLength = 0),
                LeafNodeToken("myProperty")
            )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with state STRING_LITERAL on string literal`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string"
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(BeginToken(length = 10, state = State.STRING_LITERAL), EndToken)
            )
    }

    @Test
    fun `outputs no BeginToken, EndToken pair for literal string template entries`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string"
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContain(BeginToken(length = 8, state = State.CODE))
    }

    @Test
    fun `tokenizes strings in literal string template entries`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string"
        """)

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            listOf(
                LeafNodeToken("A"),
                WhitespaceToken(length = 7, content = " "),
                LeafNodeToken("string")
            )
        )
    }

    @Test
    fun `outputs an empty Whitespace between parts of a string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string${'$'}aVariable"
        """)

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            listOf(
                LeafNodeToken("string"),
                WhitespaceToken(length = 10, content = ""),
                LeafNodeToken("${'$'}"),
                LeafNodeToken("aVariable")
            )
        )
    }

    @Test
    fun `does not output an empty Whitespace at beginning of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string${'$'}aVariable"
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContainSubsequence(
            listOf(
                LeafNodeToken("\""),
                WhitespaceToken(length = 1, content = ""),
                LeafNodeToken("A")
            )
        )
    }

    @Test
    fun `does not output an empty Whitespace before start of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string${'$'}aVariable"
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContainSubsequence(
            listOf(
                WhitespaceToken(length = 1, content = ""),
                LeafNodeToken("\""),
                LeafNodeToken("A")
            )
        )
    }

    @Test
    fun `does not output an empty Whitespace before end of string template`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            "A string${'$'}aVariable"
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContainSubsequence(
            listOf(
                LeafNodeToken("aVariable"),
                WhitespaceToken(length = 1, content = ""),
                LeafNodeToken("\"")
            )
        )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair with state MULTILINE_STRING_LITERAL on multiline`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("\"\"\"A string\"\"\"")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(BeginToken(length = 14, state = State.MULTILINE_STRING_LITERAL), EndToken)
            )
    }

    @Test
    fun `outputs a BeginToken with state LINE_COMMENT on line comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("// A comment")

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(listOf(BeginToken(length = 12, state = State.LINE_COMMENT), EndToken))
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
                    WhitespaceToken(2, " "),
                    LeafNodeToken("A"),
                    WhitespaceToken(8, " "),
                    LeafNodeToken("comment")
                )
            )
    }

    @Test
    fun `outputs a BeginToken with state TODO_COMMENT on line comment with TODO`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("// TODO: Something to be done")

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(listOf(BeginToken(length = 29, state = State.TODO_COMMENT), EndToken))
    }

    @Test
    fun `outputs a BeginToken with state LONG_COMMENT on block comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/* A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(listOf(BeginToken(length = 15, state = State.LONG_COMMENT), EndToken))
    }

    @Test
    fun `does not output leading astrix on multi-line block comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /*
             * A comment
             */
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContain(LeafNodeToken("*"))
    }

    @Test
    fun `outputs ClosingSynchronizedBreakToken before closing of KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("comment"),
                ClosingSynchronizedBreakToken(whitespaceLength = 1),
                LeafNodeToken("*/")
            )
    }

    @Test
    fun `does not output leading whitespace on KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                WhitespaceToken(length = 2, content = " "),
                LeafNodeToken("A")
            )
    }

    @Test
    fun `outputs a BeginToken with state LONG_COMMENT on KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(listOf(BeginToken(length = 16, state = State.LONG_COMMENT), EndToken))
    }

    @Test
    fun `outputs a BeginToken with state LONG_COMMENT on KDoc section`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("/** A comment */")

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                listOf(
                    BeginToken(length = 9, state = State.LONG_COMMENT),
                    LeafNodeToken("A"),
                    LeafNodeToken("comment"),
                    EndToken
                )
            )
    }

    @Test
    fun `does not output extraneous whitespace`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * A comment
             */
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result).doesNotContain(WhitespaceToken(length = 12, content = "\n "))
    }

    @Test
    fun `does not output initial astrix on KDoc comments`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * A comment
             */
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContain(LeafNodeToken("*"))
    }

    @Test
    fun `does not output a ForcedBreakTokens single line break in KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * A comment
             * Some more text
             */
        """)

        val result = subject.scan(node)

        assertThat(result)
            .doesNotContainSubsequence(
                LeafNodeToken("comment"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("Some")
            )
    }

    @Test
    fun `outputs two ForcedBreakTokens for vertical whitespace in KDoc comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * A comment
             *
             * Some more text
             */
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsSubsequence(
                LeafNodeToken("comment"),
                ForcedBreakToken(count = 2),
                LeafNodeToken("Some")
            )
    }

    @Test
    fun `outputs a BeginToken with state KDOC_DIRECTIVE on KDoc directive inside comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * @return A return value
             */
        """)

        val result = subject.scan(node)

        assertThat(result)
            .containsAll(listOf(BeginToken(length = 22, state = State.KDOC_DIRECTIVE), EndToken))
    }

    @Test
    fun `does not output a BeginToken with state CODE on KDoc directive inside comment`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /**
             * @param parameter A parameter
             */
        """)

        val result = subject.scan(node)

        assertThat(result).doesNotContain(BeginToken(length = 9, state = State.CODE))
    }

    @Test
    fun `outputs a ForcedLineBreak after a KDoc`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /** Some KDoc. */
            class MyClass
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            listOf(
                LeafNodeToken("*/"),
                ForcedBreakToken(count = 1),
                LeafNodeToken("class")
            )
        )
    }

    @Test
    fun `outputs a BeginToken, EndToken pair around a class which has KDoc`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            /** Some KDoc. */
            class MyClass
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            listOf(
                LeafNodeToken("*/"),
                BeginToken(length = 13, state = State.CODE),
                LeafNodeToken("class"),
                LeafNodeToken("MyClass"),
                EndToken
            )
        )
    }

    @Test
    fun `outputs directives in PACKAGE_IMPORT state for package directive`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            package org.kotlin.formatter
        """)

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            BeginToken(length = 28, state = State.PACKAGE_IMPORT),
            BeginToken(length = 20, state = State.PACKAGE_IMPORT),
            LeafNodeToken("org"),
            EndToken,
            EndToken
        )
    }

    @Test
    fun `outputs directives in PACKAGE_IMPORT state for import directive`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            import org.kotlin.formatter.MyClass
        """)

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            BeginToken(length = 35, state = State.PACKAGE_IMPORT),
            BeginToken(length = 28, state = State.PACKAGE_IMPORT),
            LeafNodeToken("org"),
            EndToken,
            EndToken
        )
    }

    @Test
    fun `outputs a ForcedBreak between imports`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            import org.kotlin.formatter.MyClass
            import org.kotlin.formatter.AnotherClass
        """)

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            LeafNodeToken("MyClass"),
            ForcedBreakToken(count = 1),
            LeafNodeToken("import")
        )
    }

    @Test
    fun `outputs a ForcedBreak with count 2 between package and import`() {
        val subject = subject()
        val node = kotlinLoader.parseKotlin("""
            package org.kotlin.formatter
            
            import org.kotlin.formatter.package.AClass
        """.trimIndent())

        val result = subject.scan(node)

        assertThat(result).containsSubsequence(
            LeafNodeToken("formatter"),
            ForcedBreakToken(count = 2),
            LeafNodeToken("import")
        )
    }

    private fun subject() = KotlinScanner()
}
