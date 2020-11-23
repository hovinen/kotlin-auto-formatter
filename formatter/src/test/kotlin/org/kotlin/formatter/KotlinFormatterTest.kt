package org.kotlin.formatter

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kotlin.formatter.imports.ImportPolicy

class KotlinFormatterTest {
    @Test
    fun `format breaks line at assignment operator in local variable`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        fun main() {
                            val aValue = ALongMethodCall(aParameter, anotherParameter)
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun main() {
                        val aValue =
                            ALongMethodCall(aParameter, anotherParameter)
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks line at assignment operator in global property`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """val aValue = ALongMethodCall(aParameter, anotherParameter)""".trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val aValue =
                        ALongMethodCall(aParameter, anotherParameter)
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks line at assignment operator when whitespace after assignment is missing`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        fun main() {
                            val aValue =ALongMethodCall(aParameter, anotherParameter)
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun main() {
                        val aValue =
                            ALongMethodCall(aParameter, anotherParameter)
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks line at parameters in parameter list`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun main() {
                            ALongMethodCall(aParameter, anotherParameter, aThirdParameter)
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun main() {
                        ALongMethodCall(
                            aParameter,
                            anotherParameter,
                            aThirdParameter
                        )
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format removes excess whitespace before closing parentheses`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun main() {
                            ALongMethodCall(aParameter, anotherParameter, aThirdParameter )
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun main() {
                        ALongMethodCall(
                            aParameter,
                            anotherParameter,
                            aThirdParameter
                        )
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks line at class constructor parameter list`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class ALongClass(aParameter: String, anotherParameter: String, aThirdParameter: String)
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class ALongClass(
                        aParameter: String,
                        anotherParameter: String,
                        aThirdParameter: String
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `format does not break before a class declaration based on length of body`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        internal class ALongClass(aParameter: String, anotherParameter: String, aThirdParameter: String) {
                            fun aFunction() {
                                aFunctionCall()
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    internal class ALongClass(
                        aParameter: String,
                        anotherParameter: String,
                        aThirdParameter: String
                    ) {
                        fun aFunction() {
                            aFunctionCall()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break on super constructor parameter list based on full length`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class ALongClass(aParameter: String) : ASuperclass(aParameter, anotherParameter)
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class ALongClass(aParameter: String) :
                        ASuperclass(aParameter, anotherParameter)
                """.trimIndent()
            )
    }

    @Test
    fun `breaks on multiple super class spec correctly`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class ALongClass(aParameter: String) : ASuperclass(aParameter, anotherParameter), AnInterface, AnotherInterface
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class ALongClass(aParameter: String) :
                        ASuperclass(aParameter, anotherParameter),
                        AnInterface, AnotherInterface
                """.trimIndent()
            )
    }

    @Test
    fun `does not break immediately before opening brace of class body`() {
        val result =
            KotlinFormatter(maxLineLength = 33)
                .format(
                    """
                        class AClass(aParameter: String) {
                        }
                    """.trimIndent()
                )

        assertThat(result).isEqualTo(
            """
                class AClass(
                    aParameter: String
                ) {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `maintains exactly one space between companion object and opening brace`() {
        val result =
            KotlinFormatter().format(
                """
                    class AClass {
                        companion object {
                        }
                    }
                """.trimIndent()
            )

        assertThat(result).isEqualTo(
            """
                class AClass {
                    companion object {
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun `prefers to break in parameter list to before constructor`() {
        val result =
            KotlinFormatter(maxLineLength = 54)
                .format(
                    """
                        class AClass internal constructor(aParameter: String) {
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass internal constructor(
                        aParameter: String
                    ) {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks constructor parameters consistently`() {
        val result =
            KotlinFormatter(maxLineLength = 60)
                .format(
                    """
                        class AClass(
                            private val aParameter: String,
                            private val anotherParameter: String
                        ) {
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass(
                        private val aParameter: String,
                        private val anotherParameter: String
                    ) {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks constructor parameters with a trailing comma`() {
        val result =
            KotlinFormatter(maxLineLength = 60)
                .format(
                    """
                        class AClass(
                            private val aParameter: String,
                            private val anotherParameter: String,
                        )
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass(
                        private val aParameter: String,
                        private val anotherParameter: String,
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `supports comments after a trailing comma`() {
        val result =
            KotlinFormatter(maxLineLength = 60)
                .format(
                    """
                        fun aFunction(
                            aParameter,
                            anotherParameter, // A comment
                        )
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(
                        aParameter,
                        anotherParameter, // A comment
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `breaks before a return type exceeds the column limit`() {
        val result =
            KotlinFormatter(maxLineLength = 45)
                .format(
                    """
                        fun aFunction(aParameter: String): AClass<Type> =
                            AClass()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(aParameter: String):
                        AClass<Type> = AClass()
                """.trimIndent()
            )
    }

    @Test
    fun `prefers to break before beginning of function literal`() {
        val result =
            KotlinFormatter(maxLineLength = 37)
                .format(
                    """
                        fun aFunction(aParameter: String) = { aValue -> doSomething() }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(aParameter: String) =
                        { aValue -> doSomething() }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains spacing in an empty function literal with parameters`() {
        val result = KotlinFormatter(maxLineLength = 37).format("{ aValue -> }")

        assertThat(result).isEqualTo("{ aValue -> }")
    }

    @Test
    fun `puts arrow on its own line if the parameter list does not fit on a line`() {
        val result =
            KotlinFormatter(maxLineLength = 38)
                .format("{ aValue: String, anotherValue: String -> doSomething() }")

        assertThat(result)
            .isEqualTo(
                """
                    {
                        aValue: String,
                        anotherValue: String
                        ->
                        doSomething()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `strips spacing around label of function literal`() {
        val result = KotlinFormatter(maxLineLength = 37).format("map label@ { doSomething() }")

        assertThat(result).isEqualTo("map label@{ doSomething() }")
    }

    @Test
    fun `breaks before a dot when next block does not fit on line`() {
        val result =
            KotlinFormatter(maxLineLength = 45)
                .format(
                    """
                        anObject.aMethod(aParameter, anotherParameter)
                            .anotherMethod()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    anObject
                        .aMethod(aParameter, anotherParameter)
                        .anotherMethod()
                """.trimIndent()
            )
    }

    @Test
    fun `removes spaces inside parenthesis`() {
        val result =
            KotlinFormatter(maxLineLength = 81)
                .format(
                    """
                        (
                            anObject as AnotherClass
                        )
                    """.trimIndent()
                )

        assertThat(result).isEqualTo("""(anObject as AnotherClass)""".trimIndent())
    }

    @Test
    fun `allows line break between parenthesized expression and dot`() {
        val result =
            KotlinFormatter(maxLineLength = 28)
                .format("""(anObject as AnotherClass).aMethod()""".trimIndent())

        assertThat(result).isEqualTo(
            """
                (anObject as AnotherClass)
                    .aMethod()
            """.trimIndent()
        )
    }

    @Test
    fun `allows line break between array expression and dot`() {
        val result =
            KotlinFormatter(maxLineLength = 19)
                .format("""anArray[anIndex].aMethod()""".trimIndent())

        assertThat(result).isEqualTo(
            """
                anArray[anIndex]
                    .aMethod()
            """.trimIndent()
        )
    }

    @Test
    fun `allows comments inside dot-qualified expressions`() {
        val result =
            KotlinFormatter(maxLineLength = 100)
                .format(
                    """
                        anObject.aMethod()
                            // A comment
                            // Another comment
                            .anotherMethod()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    anObject.aMethod()
                        // A comment
                        // Another comment
                        .anotherMethod()
                """.trimIndent()
            )
    }

    @Test
    fun `allows comments inside single dot-qualified expression`() {
        val result =
            KotlinFormatter(maxLineLength = 100)
                .format(
                    """
                        anObject
                            // A comment
                            // Another comment
                            .aMethod()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    anObject
                        // A comment
                        // Another comment
                        .aMethod()
                """.trimIndent()
            )
    }

    @Test
    fun `allows mixture of block and EOL comments inside single dot-qualified expression`() {
        val result =
            KotlinFormatter(maxLineLength = 100)
                .format(
                    """
                        anObject
                            // A comment
                            /* Another comment */
                            .aMethod()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    anObject
                        // A comment
                        /* Another comment */
                        .aMethod()
                """.trimIndent()
            )
    }

    @Test
    fun `format does not indent a class after a class after a package declaration`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        package apackage
                        
                        class AClass
                        
                        class AnotherClass
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    package apackage
                    
                    class AClass
                    
                    class AnotherClass
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks at parameters of function declarations`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun aFunction(aParameter: String, anotherParameter: String, aThirdParameter: String) {
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(
                        aParameter: String,
                        anotherParameter: String,
                        aThirdParameter: String
                    ) {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `correctly handles functional interfaces`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        interface fun AFunctionalInterface {
                            fun doSomething()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    interface fun AFunctionalInterface {
                        fun doSomething()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break around parameters based on the length of the function initializer`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun aFunction(aParameter: String) = aFunctionCall().anotherFunctionCall()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(aParameter: String) =
                        aFunctionCall().anotherFunctionCall()
                """.trimIndent()
            )
    }

    @Test
    fun `does not break around parameters based on the length of the KDoc`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        /** Some long KDoc which should not wrap */
                        fun aFunction(aParameter: String) {
                            aFunctionCall().anotherFunctionCall()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some long KDoc which should not wrap */
                    fun aFunction(aParameter: String) {
                        aFunctionCall().anotherFunctionCall()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks a property before its getter`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        class AClass {
                            val aProperty: String get() = "This value is too long for one line"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        val aProperty: String
                            get() = "This value is too long for one line"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks a property before its getter when setter is present`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        class AClass {
                            val aProperty: String get() = "Something"
                                set(value) { aFunction() }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        val aProperty: String
                            get() = "Something"
                            set(value) { aFunction() }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format handles a comment between property initializers`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        class AClass {
                            val aProperty: String
                                get() = "Something"
                                // A comment
                                set(value) { aFunction() }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        val aProperty: String
                            get() = "Something"
                            // A comment
                            set(value) { aFunction() }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `corrects the spacing around an initializer style property accessor`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        class AClass {
                            val aProperty: String get()="Value"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        val aProperty: String
                            get() = "Value"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks a property before its setter`() {
        val result =
            KotlinFormatter(maxLineLength = 80)
                .format(
                    """
                        class AClass {
                            var aProperty: AType = aValue
                                private set
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        var aProperty: AType = aValue
                            private set
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks a parameter list preceded by a blank line`() {
        val result =
            KotlinFormatter(maxLineLength = 55)
                .format(
                    """
                        class MyClass {
                        
                            fun aFunction(aParameter: String, anotherParameter: String, aThirdParameter: String) {
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                    
                        fun aFunction(
                            aParameter: String,
                            anotherParameter: String,
                            aThirdParameter: String
                        ) {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `correctly treats annotations with arguments`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation("An argument")
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation("An argument")
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `treats array initializers in annotations as blocks`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation(["An argument", "Another argument", "A third argument"])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation([
                            "An argument",
                            "Another argument",
                            "A third argument"
                        ])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comment formatting in collection literal expressions`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        @AnAnnotation([
                            "An argument", // A comment
                            "Another argument", // Another comment
                            "A third argument" // A third comment
                        ])
                        fun myFunction()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation([
                        "An argument", // A comment
                        "Another argument", // Another comment
                        "A third argument" // A third comment
                    ])
                    fun myFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `strips trailing newline whitespace from array initializers in annotations`() {
        val result =
            KotlinFormatter(maxLineLength = 80)
                .format(
                    """
                        @AnAnnotation(value = [
                            "An argument", "Another argument", "A third argument"
                        ])
                        fun myFunction()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation(value = ["An argument", "Another argument", "A third argument"])
                    fun myFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `adds spaces between array elements in an annotation`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation(["1","2","3"])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation(["1", "2", "3"])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `removes extraneous spaces in array initializer in an annotation`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation(["1" ,"2" ,"3"])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation(["1", "2", "3"])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert a space before a sole element in an array in an annotation`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation(["1"])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation(["1"])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `strips extraneous spacing from array initializer in annotation`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation([ "1" ])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation(["1"])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `accepts empty array initializers`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation([])
                            fun myFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation([])
                        fun myFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `properly formats annotated type parameters`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val value: Map<@AnAnnotation("A value") Key, @AnotherAnnotation("Another value") Value>
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val value:
                        Map<
                            @AnAnnotation("A value") Key,
                            @AnotherAnnotation("Another value") Value
                        >
                """.trimIndent()
            )
    }

    @Test
    fun `properly formats annotated type parameters with trailing comma`() {
        val result = KotlinFormatter(maxLineLength = 50).format("val value: Map<Key, Value,>")

        assertThat(result).isEqualTo("val value: Map<Key, Value,>")
    }

    @Test
    fun `properly formats annotated type parameters with trailing comma and EOL comment`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val value: Map<
                            Key,
                            Value, // Comment
                        >
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val value: Map<
                        Key,
                        Value, // Comment
                    >
                """.trimIndent()
            )
    }

    @Test
    fun `splits annotations on constructor parameters by newlines when they do not fit on line`() {
        val result =
            KotlinFormatter(maxLineLength = 53)
                .format(
                    """
                        class AClass(@AnAnnotation(["Some value", "Some other value"]) @AnotherAnnotation private val aProperty: String)
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass(
                        @AnAnnotation(["Some value", "Some other value"])
                        @AnotherAnnotation
                        private val aProperty: String
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `handles trailing commas in array initializers`() {
        val result =
            KotlinFormatter(maxLineLength = 53)
                .format(
                    """
                        @AnAnnotation(["Some value", "Some other value",]) private val aProperty: String
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation(["Some value", "Some other value",])
                    private val aProperty: String
                """.trimIndent()
            )
    }

    @Test
    fun `preserves vertical whitespace between constructor parameters`() {
        val result =
            KotlinFormatter(maxLineLength = 53)
                .format(
                    """
                        class AClass(
                            private val aProperty: String,
                        
                            private val anotherProperty: String
                        )
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass(
                        private val aProperty: String,
                    
                        private val anotherProperty: String
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `maintains space between comma-separated type arguments`() {
        val result = KotlinFormatter(maxLineLength = 50).format("val value: Map<Key, Value>")

        assertThat(result).isEqualTo("val value: Map<Key, Value>")
    }

    @Test
    fun `fixes space between comma-separated type arguments`() {
        val result =
            KotlinFormatter(maxLineLength = 50).format("val value: Map< Key ,Something , Value >")

        assertThat(result).isEqualTo("val value: Map<Key, Something, Value>")
    }

    @Test
    fun `preserves comments between type arguments`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val value: Map<
                            /* A comment */ Key,
                            Something, /* Another comment */
                            SomethingElse, // Another comment
                            Value, // Another comment
                        >
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val value:
                        Map<
                            /* A comment */
                            Key,
                            Something, /* Another comment */
                            SomethingElse, // Another comment
                            Value, // Another comment
                        >
                """.trimIndent()
            )
    }

    @Test
    fun `breaks between annotations and multiline declarations`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        @AnAnnotation
                        fun myFunction() {
                            aFunctionCall()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation
                    fun myFunction() {
                        aFunctionCall()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks between annotations and class declaration`() {
        val result = KotlinFormatter().format(
            """
                @AnAnnotation
                class MyClass {
                }
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                @AnAnnotation
                class MyClass {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `breaks between different annotations`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        @AnAnnotation
                        @AnotherAnnotation
                        val aProperty: String
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation
                    @AnotherAnnotation
                    val aProperty: String
                """.trimIndent()
            )
    }

    @Test
    fun `does not break before property name in extension property`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val AClass.aProperty: String
                            get() =
                                aFunction(aParameter, anotherParameter)
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val AClass.aProperty: String
                        get() =
                            aFunction(aParameter, anotherParameter)
                """.trimIndent()
            )
    }

    @Test
    fun `allows block comments between annotations`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        @AnAnnotation
                        /* A comment */
                        @AnotherAnnotation
                        val aProperty: String
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation
                    /* A comment */
                    @AnotherAnnotation
                    val aProperty: String
                """.trimIndent()
            )
    }

    @Test
    fun `allows EOL comments after annotations`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        @AnAnnotation // A comment
                        @AnotherAnnotation
                        val aProperty: String
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation // A comment
                    @AnotherAnnotation
                    val aProperty: String
                """.trimIndent()
            )
    }

    @Test
    fun `includes space between modifiers and property`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        private val aProperty: String = "A long string which should wrap"
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    private val aProperty: String =
                        "A long string which should wrap"
                """.trimIndent()
            )
    }

    @Test
    fun `includes space between modifiers and function`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        private fun aFunction(): String = "A long string which should wrap"
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    private fun aFunction(): String =
                        "A long string which should wrap"
                """.trimIndent()
            )
    }

    @Test
    fun `indents property initializer for public function when inside a class`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            fun aFunction(): String = "A long string which should wrap"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        fun aFunction(): String =
                            "A long string which should wrap"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents property initializer for private function when inside a class`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            private fun aFunction(): String = "A long string which should wrap"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        private fun aFunction(): String =
                            "A long string which should wrap"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents property initializer for annotated function when inside a class`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation
                            fun aFunction(): String = "A long string which should wrap"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation
                        fun aFunction(): String =
                            "A long string which should wrap"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `properly handles spacing around secondary constructor with modifiers`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            @AnAnnotation
                            constructor()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        @AnAnnotation
                        constructor()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `properly handles delegated constructor call`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        class MyClass {
                            constructor() : this("Something")
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        constructor() : this("Something")
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `treats delegated constructor call as a block`() {
        val result =
            KotlinFormatter(maxLineLength = 25)
                .format(
                    """
                        class MyClass {
                            constructor() : this("Something")
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        constructor() :
                            this("Something")
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks after colon in delegated constructor call`() {
        val result =
            KotlinFormatter(maxLineLength = 35)
                .format(
                    """
                        class MyClass {
                            constructor(aParameter: String) : this("Something")
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        constructor(
                            aParameter: String
                        ) : this("Something")
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks at logical operator in an if statement`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            if (aLongCondition && anotherLongCondition && yetAnotherLongCondition) {
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        if (aLongCondition && anotherLongCondition &&
                            yetAnotherLongCondition
                        ) {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks if condition even if condition would fit on the line`() {
        val result =
            KotlinFormatter(maxLineLength = 35)
                .format(
                    """
                        if (aCondition || anotherCondition) {
                        }
                    """.trimIndent()
                )

        assertThat(result).isEqualTo(
            """
                if (aCondition || anotherCondition
                ) {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `breaks if with negated condition at the correct location`() {
        val result =
            KotlinFormatter(maxLineLength = 37)
                .format(
                    """
                        if (aCondition || !(aNegatedCondition)) {
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    if (aCondition ||
                        !(aNegatedCondition)
                    ) {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks a too long if-else block`() {
        val result =
            KotlinFormatter(maxLineLength = 51)
                .format("""if (aCondition) "Some value" else "Some other value"""".trimIndent())

        assertThat(result)
            .isEqualTo(
                """
                    if (aCondition)
                        "Some value"
                    else
                        "Some other value"
                """.trimIndent()
            )
    }

    @Test
    fun `does not break a before else if`() {
        val result =
            KotlinFormatter(maxLineLength = 51)
                .format(
                    """
                        if (aCondition) {
                            something()
                        } else if (anotherCondition) {
                            somethingElse()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    if (aCondition) {
                        something()
                    } else if (anotherCondition) {
                        somethingElse()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not consolidate EOL comment with following operator in a binary expression`() {
        val result =
            KotlinFormatter(maxLineLength = 52)
                .format(
                    """
                        condition1 // A comment which doesn't fit on the line
                        && condition2
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    condition1
                        // A comment which doesn't fit on the line
                        && condition2
                """.trimIndent()
            )
    }

    @Test
    fun `preserves newline before EOL comment in a binary expression`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        condition1
                            // A comment which fits on the line
                            && condition2
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    condition1
                        // A comment which fits on the line
                        && condition2
                """.trimIndent()
            )
    }

    @Test
    fun `prefers not to break before as operator`() {
        val result =
            KotlinFormatter(maxLineLength = 26).format("val something = variable as MyClass")

        assertThat(result).isEqualTo(
            """
                val something =
                    variable as MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `format breaks at logical operator in a while statement`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            while (aLongCondition && anotherLongCondition && yetAnotherLongCondition) {
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        while (aLongCondition && anotherLongCondition &&
                            yetAnotherLongCondition
                        ) {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks at an operator within a when statement`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            when (aLongExression + anotherLongExpression + yetAnotherLongExpression) {
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        when (aLongExression + anotherLongExpression +
                            yetAnotherLongExpression
                        ) {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks at in operator in for statement`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            for (aLongVariableName in aCollectionWithALongName) {
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        for (aLongVariableName in
                            aCollectionWithALongName
                        ) {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `handles for with label correctly`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        loop@for (aVariable in aCollection) {
                            break@loop
                            continue@loop
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    loop@for (aVariable in aCollection) {
                        break@loop
                        continue@loop
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks before chained calls`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aVariable.aMethod().anotherMethod().aThirdMethod()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aVariable.aMethod()
                            .anotherMethod()
                            .aThirdMethod()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks before chained calls with null check`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aVariable.aMethod()?.anotherMethod()?.aThirdMethod()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aVariable.aMethod()
                            ?.anotherMethod()
                            ?.aThirdMethod()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aVariable = aValue + anotherValue + yetAnotherValue + andYetAnotherValue
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aVariable =
                            aValue + anotherValue + yetAnotherValue +
                                andYetAnotherValue
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `prefers to break outside grouping operators`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aVariable = aValue + anotherValue + (yetAnotherValue + andYetAnotherValue)
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aVariable =
                            aValue + anotherValue +
                                (yetAnotherValue + andYetAnotherValue)
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators inside if statements`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            if (aCondition) {
                                aVariable = aValue + anotherValue + yetAnotherValue
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        if (aCondition) {
                            aVariable =
                                aValue + anotherValue +
                                    yetAnotherValue
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators inside while statements`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            while (aCondition) {
                                aVariable = aValue + anotherValue + yetAnotherValue
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        while (aCondition) {
                            aVariable =
                                aValue + anotherValue +
                                    yetAnotherValue
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators inside for loops`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            for (anEntry in aCollection) {
                                aVariable = aValue + anotherValue + yetAnotherValue
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        for (anEntry in aCollection) {
                            aVariable =
                                aValue + anotherValue +
                                    yetAnotherValue
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators inside first when entry`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            when (aCondition) {
                                1 -> {
                                    aVariable = aValue + anotherValue + yetAnotherValue
                                }
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        when (aCondition) {
                            1 -> {
                                aVariable =
                                    aValue + anotherValue +
                                        yetAnotherValue
                            }
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators inside a second when entry`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
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
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
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
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comment inside when expression`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        when (aCondition) {
                            // A comment
                            1 -> {
                                aVariable = aValue
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    when (aCondition) {
                        // A comment
                        1 -> {
                            aVariable = aValue
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves sequence of EOL comments inside when expression`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        when (aCondition) {
                            // A comment
                            // A second line
                            1 -> {
                                aVariable = aValue
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    when (aCondition) {
                        // A comment
                        // A second line
                        1 -> {
                            aVariable = aValue
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comment at the end of a when expression`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        when (aCondition) {
                            1 -> {
                                aVariable = aValue
                            }
                            // A comment
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    when (aCondition) {
                        1 -> {
                            aVariable = aValue
                        }
                        // A comment
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions at operators leaving operations at the end`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aVariable = aValue + anotherValue + yetAnotherValue + andYetAnotherValue + moreStuff
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aVariable =
                            aValue + anotherValue + yetAnotherValue +
                                andYetAnotherValue + moreStuff
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks before the elvis operator`() {
        val result =
            KotlinFormatter(maxLineLength = 34)
                .format(
                    """
                        aVariable =
                            aNullableValue ?: anAlternative
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    aVariable =
                        aNullableValue
                            ?: anAlternative
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks function literal after arguments`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val aFunction = { aParameter, anotherParameter ->
                            anotherFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val aFunction = { aParameter, anotherParameter ->
                        anotherFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `prefers to break before function literal following argument list`() {
        val result =
            KotlinFormatter(maxLineLength = 57)
                .format(
                    """
                        aFunction("Something", "Something else") { aParameter, anotherParameter -> anotherFunction() }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    aFunction("Something", "Something else")
                        { aParameter, anotherParameter -> anotherFunction() }
                """.trimIndent()
            )
    }

    @Test
    fun `prefers not to break before a function name`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun `an extremely long function name which does not fit`() {
                            aFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun `an extremely long function name which does not fit`() {
                        aFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `prefers not to break before a function name after modifier`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        internal fun `an extremely long function name which does not fit`() {
                            aFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    internal fun `an extremely long function name which does not fit`() {
                        aFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks strings at a single word boundary when possible`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "ALongStringInitializerWhichShould wrapToANewLine"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "ALongStringInitializerWhichShould " +
                                "wrapToANewLine"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents the right hand side of an initializer one step beyond the type`() {
        val result =
            KotlinFormatter(maxLineLength = 66)
                .format(
                    """
                        class AClass {
                            fun myFunction(aParameter: String, anotherParameter: String): String =
                                "A string initializer which should move to the next"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass {
                        fun myFunction(aParameter: String, anotherParameter: String):
                            String =
                                "A string initializer which should move to the next"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains a newline before an EOL comment at the beginning of an initializer`() {
        val result =
            KotlinFormatter(maxLineLength = 66)
                .format(
                    """
                        fun aFunction(): String =
                            // A comment
                            "A value"
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(): String =
                        // A comment
                        "A value"
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks strings at multiple word boundaries when possible`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "ALongStringInitializerWhichShould wrapToANewLineAndWrappedAgain whereNecessary"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "ALongStringInitializerWhichShould " +
                                "wrapToANewLineAndWrappedAgain " +
                                "whereNecessary"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks strings only at a suitable word boundary`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "A long string initializer which should wrap to a new line"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A long string initializer which should" +
                                " wrap to a new line"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks strings at multiple lines only at a suitable word boundary`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "A long string initializer which should wrap to a new line and wrap again"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A long string initializer which should" +
                                " wrap to a new line and wrap again"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `treats breaking string literals as idempotent when first part exactly hits end`() {
        val result =
            KotlinFormatter(maxLineLength = 51)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable =
                                "A long string initializer which should " +
                                    "wrap to a new line and wrap again"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A long string initializer which should " +
                                "wrap to a new line and wrap again"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format does not break a string template inside an expression`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "ALongStringInitializerWhichShouldWrap${'$'}{someExpression}"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "ALongStringInitializerWhichShouldWrap" +
                                "${'$'}{someExpression}"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks strings which include template variables`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "A long string initializer with ${'$'}variable should wrap"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A long string initializer with " +
                                "${'$'}variable should wrap"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format correctly calculates line break position for string templates with expressions`() {
        val result =
            KotlinFormatter(maxLineLength = 61)
                .format(
                    """
                        aFunction(
                        "a a a ${'$'}{object.method()} a a ${'$'}{object.method()} a a a " +
                            "a"
                        )
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    aFunction(
                        "a a a ${'$'}{object.method()} a a ${'$'}{object.method()} a a a" +
                            " " + "a"
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `does not break a string if the rest fits on the line`() {
        val result =
            KotlinFormatter(maxLineLength = 52)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "A string initializer which should not wrap"
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A string initializer which should not wrap"
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains formatting of a function call with a string literal argument`() {
        val result =
            KotlinFormatter(maxLineLength = 37)
                .format(
                    """
                        AClass(
                            content = "A\n    more content"
                        )
                    """.trimIndent()
                )

        assertThat(result).isEqualTo(
            """
                AClass(
                    content = "A\n    more content"
                )
            """.trimIndent()
        )
    }

    @Test
    fun `does not break a string if a method is called on it`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = "A long string initializer which should not wrap to a new line".aMethod()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            "A long string initializer which should not wrap to a new line"
                                .aMethod()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks expressions recursively when required`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            val aNewVariable = aVariable.aMethod()?.anotherMethod()?.aThirdMethod()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        val aNewVariable =
                            aVariable.aMethod()
                                ?.anotherMethod()
                                ?.aThirdMethod()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break between return and return value`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            return aVariable.aMethod().anotherMethod().aThirdMethod()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        return aVariable.aMethod()
                            .anotherMethod()
                            .aThirdMethod()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert whitespace between return and label`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            return@label
                        }
                    """.trimIndent()
                )

        assertThat(result).isEqualTo(
            """
                fun myFunction() {
                    return@label
                }
            """.trimIndent()
        )
    }

    @Test
    fun `does not output whitespace around the range operator`() {
        val result = KotlinFormatter().format("0..100".trimIndent())

        assertThat(result).isEqualTo("0..100")
    }

    @Test
    fun `does not include trailing whitespace on naked return`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            return
                        }
                    """.trimIndent()
                )

        assertThat(result).isEqualTo(
            """
                fun myFunction() {
                    return
                }
            """.trimIndent()
        )
    }

    @Test
    fun `indents multiline string literal with trimIndent call`() {
        val result =
            KotlinFormatter(maxLineLength = 30)
                .format(
                    """
                        val aString = aFunction(${'"'}""
                            Some content
                        ${'"'}"".trimIndent())
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val aString =
                        aFunction(
                            ${'"'}""
                                Some content
                            ${'"'}"".trimIndent()
                        )
                """.trimIndent()
            )
    }

    @Test
    fun `does not force breaks on method parameters on multiline string literals`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        val aString = ${'"'}""
                            Some content
                        ${'"'}"".aFunction(aParameter, anotherParameter)
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    val aString =
                        ${'"'}""
                        Some content
                    ${'"'}"".aFunction(aParameter, anotherParameter)
                """.trimIndent()
            )
    }

    @Test
    fun `does not break between else and opening brace of block`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            if (aCondition) {
                                doSomething()
                            } else {
                                doSomethingElse().andThenSomethingElse().andThenSomethingElse()
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        if (aCondition) {
                            doSomething()
                        } else {
                            doSomethingElse().andThenSomethingElse()
                                .andThenSomethingElse()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break before else when preceded by a block`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        if (aCondition) {
                            doSomething()
                        } else
                            doSomethingElse()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    if (aCondition) {
                        doSomething()
                    } else
                        doSomethingElse()
                """.trimIndent()
            )
    }

    @Test
    fun `does not break between function and closure parameter`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            aMethodWithLambda { doSomething().doSomethingElse() }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aMethodWithLambda {
                            doSomething().doSomethingElse()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `respects column limit when assignment operator would break it`() {
        val result =
            KotlinFormatter(maxLineLength = 48)
                .format(
                    """
                        fun myFunction(aParameter: String): AReturnType = anotherFunction()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction(aParameter: String):
                        AReturnType = anotherFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert a newline between throw and exception`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            throw AnException("A long exception message which wraps")
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        throw AnException(
                            "A long exception message which wraps"
                        )
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents inside when entry`() {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            when (anExpression) {
                                else ->
                                    throw AnException("A long exception message")
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        when (anExpression) {
                            else ->
                                throw AnException(
                                    "A long exception message"
                                )
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert whitespace into an empty block`() {
        val result = KotlinFormatter().format("""val myObject = MyInterface {}""".trimIndent())

        assertThat(result).isEqualTo("""val myObject = MyInterface {}""".trimIndent())
    }

    @ParameterizedTest
    @ValueSource(strings = [".", "?."])
    fun `does not break on a single dot expression`(operator: String) {
        val result =
            KotlinFormatter(maxLineLength = 50)
                .format(
                    """
                        fun myFunction() {
                            anObject${operator}aMethod(aParameter, anotherParameter, aThirdParameter)
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        anObject${operator}aMethod(
                            aParameter,
                            anotherParameter,
                            aThirdParameter
                        )
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks at first dot operator in dot expression when necessary`() {
        val result =
            KotlinFormatter(maxLineLength = 30)
                .format(
                    """
                        fun myFunction() {
                            aMethod(aParameter).aProperty
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun myFunction() {
                        aMethod(aParameter)
                            .aProperty
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents a try-catch expression correctly`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        fun aFunction() { 
                            try {
                                aFunction()
                            } catch (e: Exception) {
                                anotherFunction()
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction() {
                        try {
                            aFunction()
                        } catch (e: Exception) {
                            anotherFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents a try-catch expression on a single line correctly`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try { aFunction() } catch (e: Exception) { anotherFunction() }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    } catch (e: Exception) {
                        anotherFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `indents a multi-try-catch expression correctly`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try {
                            aFunction()
                        } catch (e: Exception) {
                            anotherFunction()
                        } catch (e: AnotherExpression) {
                            aDifferentFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    } catch (e: Exception) {
                        anotherFunction()
                    } catch (e: AnotherExpression) {
                        aDifferentFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `handles empty catch clause`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try {
                            aFunction()
                        }
                        catch (e: Exception) {}
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    } catch (e: Exception) {}
                """.trimIndent()
            )
    }

    @Test
    fun `handles a finally block`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try {
                            aFunction()
                        } catch (e: Exception) {
                            anotherFunction()
                        } finally {
                            aDifferentFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    } catch (e: Exception) {
                        anotherFunction()
                    } finally {
                        aDifferentFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `handles comments before a catch clause`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try {
                            aFunction()
                        }
                        // A comment
                        catch (e: Exception) {
                            anotherFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    }
                    // A comment
                    catch (e: Exception) {
                        anotherFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `handles comments before a finally clause`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """
                        try {
                            aFunction()
                        }
                        // A comment
                        finally {
                            anotherFunction()
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    try {
                        aFunction()
                    }
                    // A comment
                    finally {
                        anotherFunction()
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks before try expression inside function literal`() {
        val result =
            KotlinFormatter()
                .format(
                    """
                        aFunctionAcceptingALambda {
                            try {
                                aFunction()
                            } catch (e: Exception) {
                                anotherFunction()
                            }
                        }
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    aFunctionAcceptingALambda {
                        try {
                            aFunction()
                        } catch (e: Exception) {
                            anotherFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `format breaks the short form of the summary fragment`() {
        val result =
            KotlinFormatter(maxLineLength = 69)
                .format(
                    """
                        /** An extra long summary fragment which should wrap to a new line. */
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * An extra long summary fragment which should wrap to a new line.
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `format does not break if there is no word boundary`() {
        val result =
            KotlinFormatter(maxLineLength = 60)
                .format(
                    """
                        /**
                         * http://www.example.com/an-extra-long-url-which-should-not-break
                         */
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * http://www.example.com/an-extra-long-url-which-should-not-break
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `format preserves newlines between tags`() {
        val result =
            KotlinFormatter(maxLineLength = 60)
                .format(
                    """
                        /**
                         * @param parameter an input parameter with a particularly long description
                         * @param anotherParameter another input parameter
                         */
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * @param parameter an input parameter with a particularly
                     *     long description
                     * @param anotherParameter another input parameter
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert line breaks in the package statement`() {
        val result =
            KotlinFormatter(maxLineLength = 20)
                .format(
                    """
                        package org.kotlin.a.very.long.package.name.which.should.not.wrap
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """package org.kotlin.a.very.long.package.name.which.should.not.wrap""".trimIndent()
            )
    }

    @Test
    fun `does not insert line breaks in an import statement`() {
        val subject =
            KotlinFormatter(maxLineLength = 20, importPolicySupplier = { { _, _ -> true } })

        val result =
            subject.format(
                """
                    import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert line breaks in an import statement with alias`() {
        val subject =
            KotlinFormatter(maxLineLength = 20, importPolicySupplier = { { _, _ -> true } })

        val result =
            subject.format(
                """
                    import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass as AnAlias
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    import org.kotlin.a.very.long.package.name.which.should.not.wrap.AClass as AnAlias
                """.trimIndent()
            )
    }

    @Test
    fun `preserves single line breaks between import statements`() {
        val subject = KotlinFormatter(importPolicySupplier = { { _, _ -> true } })

        val result =
            subject.format(
                """
                    import org.kotlin.formatter.AClass
                    import org.kotlin.formatter.AnotherClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    import org.kotlin.formatter.AClass
                    import org.kotlin.formatter.AnotherClass
                """.trimIndent()
            )
    }

    @Test
    fun `does not indent import statements after a package statement`() {
        val subject = KotlinFormatter(importPolicySupplier = { { _, _ -> true } })

        val result =
            subject.format(
                """
                    package org.kotlin.formatter
                    
                    import org.kotlin.formatter.package.AClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    package org.kotlin.formatter
                    
                    import org.kotlin.formatter.package.AClass
                """.trimIndent()
            )
    }

    @Test
    fun `sorts import statements alphabetically`() {
        val subject = KotlinFormatter(importPolicySupplier = { { _, _ -> true } })

        val result =
            subject.format(
                """
                    import java.time.LocalDate
                    import org.kotlin.formatter.BClass
                    import org.kotlin.formatter.AClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    import java.time.LocalDate
                    import org.kotlin.formatter.AClass
                    import org.kotlin.formatter.BClass
                """.trimIndent()
            )
    }

    @Test
    fun `does not indent top level class declaration`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                package org.kotlin.formatter
                
                class MyClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                package org.kotlin.formatter
                
                class MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `breaks before inheritance spec when necessary`() {
        val subject = KotlinFormatter(maxLineLength = 46)

        val result =
            subject.format(
                """
                    class MyClass(aParameter: String) : AnInterface {
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass(aParameter: String) :
                        AnInterface {
                    
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `breaks parameters list when existence of supertype spec would break column limit`() {
        val subject = KotlinFormatter(maxLineLength = 33)

        val result =
            subject.format(
                """
                    class MyClass(aParameter: String) : AnInterface {
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass(
                        aParameter: String
                    ) : AnInterface {
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break before colon before inheritance spec`() {
        val subject = KotlinFormatter(maxLineLength = 33)

        val result =
            subject.format("""class MyClass(aParameter: String) : AnInterface""".trimIndent())

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass(
                        aParameter: String
                    ) : AnInterface
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert an extra blank line at the beginning of a class`() {
        val subject = KotlinFormatter(maxLineLength = 46)

        val result =
            subject.format(
                """
                    class MyClass(aParameter: String) :
                        AnInterface {
                    
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass(aParameter: String) :
                        AnInterface {
                    
                        val aProperty: String = ""
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not have trailing whitespace when inserting a blank line`() {
        val subject = KotlinFormatter(maxLineLength = 46)

        val result =
            subject.format(
                """
                    class OuterClass {
                        class MyClass(aParameter: String) :
                            AnInterface {
                    
                            val aProperty: String = ""
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class OuterClass {
                        class MyClass(aParameter: String) :
                            AnInterface {
                    
                            val aProperty: String = ""
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not break before object expression when unnecessary`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    fun aFunction() {
                        val pomModel: PomModel = object : UserDataHolderBase(), PomModel {
                            val aProperty: String = ""
                            val anotherProperty: String = ""
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction() {
                        val pomModel: PomModel = object : UserDataHolderBase(), PomModel {
                            val aProperty: String = ""
                            val anotherProperty: String = ""
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `inserts whitespace before and after colon before super type list`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                class MyClass():AnInterface {
                }
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                class MyClass() : AnInterface {
                }
            """.trimIndent()
        )
    }

    @Test
    fun `does not insert break between annotation and constructor keyword`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                class MyClass @AnAnnotation constructor() {
                }
            """.trimIndent()
        )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass @AnAnnotation constructor() {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not insert break between annotation and constructor keyword if there are params`() {
        val subject = KotlinFormatter(maxLineLength = 50)

        val result =
            subject.format(
                """
                    class MyClass @AnAnnotation private constructor(
                        aParameter: String,
                        anotherParameter: String,
                        aThirdParameter: String
                    ) {
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass @AnAnnotation private constructor(
                        aParameter: String,
                        anotherParameter: String,
                        aThirdParameter: String
                    ) {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves whitespace between annotations in primary constructor`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass @AnAnnotation @AnotherAnnotation constructor() {
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass @AnAnnotation @AnotherAnnotation constructor() {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `removes whitespace between constructor and arguments`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass @AnAnnotation constructor () {
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass @AnAnnotation constructor() {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comments after primary constructor annotation list`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass @AnAnnotation /* A comment */ constructor () {
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass @AnAnnotation /* A comment */ constructor() {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comments before primary constructor annotation list`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass /* A comment */ @AnAnnotation constructor () {
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass /* A comment */ @AnAnnotation constructor() {
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves sequences of EOL comments among annotations`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    @AnAnnotation
                    // A comment
                    // Another comment
                    @AnotherAnnotation
                    class MyClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation
                    // A comment
                    // Another comment
                    @AnotherAnnotation
                    class MyClass
                """.trimIndent()
            )
    }

    @Test
    fun `preserves EOL comments between annotations and declarations`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                @AnAnnotation
                // A comment
                class MyClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                @AnAnnotation
                // A comment
                class MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `preserves EOL comments between annotations and declarations with modifiers`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    @AnAnnotation
                    // A comment
                    internal class MyClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @AnAnnotation
                    // A comment
                    internal class MyClass
                """.trimIndent()
            )
    }

    @Test
    fun `preserves newline after Suppress annotation`() {
        val subject = KotlinFormatter(maxLineLength = 70)

        val result =
            subject.format(
                """
                    @Suppress("UNCHECKED_CAST")
                    aVariable as AGenericType<ATypeParameter>
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    @Suppress("UNCHECKED_CAST")
                    aVariable as AGenericType<ATypeParameter>
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting within EOL comments`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                // a b  c   d    e
                //   f    g      h
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                // a b  c   d    e
                //   f    g      h
            """.trimIndent()
        )
    }

    @Test
    fun `preserves formatting within block comments`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                /* a b  c   d    e
                 *   f    g      h */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /* a b  c   d    e
                 *   f    g      h */
            """.trimIndent()
        )
    }

    @Test
    fun `preserves empty lines on block comments`() {
        val subject = KotlinFormatter()

        val result = subject.format(
            """
                /* A comment
                 *
                 * After blank line */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /* A comment
                 *
                 * After blank line */
            """.trimIndent()
        )
    }

    @Test
    fun `does not insert a line break in an EOL comment when column limit impossible to keep`() {
        val subject = KotlinFormatter(maxLineLength = 20)

        val result = subject.format("""// aaaa_aaaa_aaaa_aaaa_aaaa""".trimIndent())

        assertThat(result).isEqualTo("""// aaaa_aaaa_aaaa_aaaa_aaaa""".trimIndent())
    }

    @Test
    fun `does not insert a line break in an block comment when column limit impossible to keep`() {
        val subject = KotlinFormatter(maxLineLength = 20)

        val result = subject.format("""/* aaaa_aaaa_aaaa_aaaa_aaaa */""".trimIndent())

        assertThat(result).isEqualTo("""/* aaaa_aaaa_aaaa_aaaa_aaaa */""".trimIndent())
    }

    @Test
    fun `does not insert a line break in string literal when column limit impossible to keep`() {
        val subject = KotlinFormatter(maxLineLength = 20)

        val result = subject.format("""val str = "aaaa_aaaa_aaaa_aaaa_aaaa"""".trimIndent())

        assertThat(result).isEqualTo(
            """
                val str =
                    "aaaa_aaaa_aaaa_aaaa_aaaa"
            """.trimIndent()
        )
    }

    @Test
    fun `does not indent init block`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        private val aField: String
                        
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        private val aField: String
                    
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `does not indent init block after constructor`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        constructor(aParameter: String): super(aParameter)
                        
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        constructor(aParameter: String) : super(aParameter)
                    
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `enforces whitespace between init and {`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        init{
                            aFunction()
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `removes extraneous whitespace between init and {`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        init
                        {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `preserves comments before an init block`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        // A comment
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        // A comment
                        init {
                            aFunction()
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains a line break between KDoc and class declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** Some KDoc. */
                class MyClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                class MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `accepts empty KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** */
                class MyClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /**  */
                class MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `maintains a line break between KDoc and object declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** Some KDoc. */
                object MyObject
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                object MyObject
            """.trimIndent()
        )
    }

    @Test
    fun `maintains a line break between KDoc and typealias declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** Some KDoc. */
                typealias AType = Int
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                typealias AType = Int
            """.trimIndent()
        )
    }

    @Test
    fun `handles a typealias with a type parameter`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""typealias AType<ATypeParameter> = Int""".trimIndent())

        assertThat(result).isEqualTo("""typealias AType<ATypeParameter> = Int""".trimIndent())
    }

    @Test
    fun `handles comments between KDoc and typealias`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    /** Some KDoc. */
                    // A comment
                    typealias AType = Int
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some KDoc. */
                    // A comment
                    typealias AType = Int
                """.trimIndent()
            )
    }

    @Test
    fun `handles comments before rhs of typealias`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                typealias AType = // A comment
                    Int
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                typealias AType = // A comment
                    Int
            """.trimIndent()
        )
    }

    @Test
    fun `moves modifiers preceding KDoc to after the KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                @AnAnnotation
                /** Some KDoc. */
                class MyClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                @AnAnnotation
                class MyClass
            """.trimIndent()
        )
    }

    @Test
    fun `moves modifiers preceding KDoc to after the KDoc in the presence of another modifier`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    @AnAnnotation
                    /** Some KDoc. */
                    data class MyClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some KDoc. */
                    @AnAnnotation
                    data class MyClass
                """.trimIndent()
            )
    }

    @Test
    fun `supports modifiers on typealias declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format("""internal typealias AType = Int""".trimIndent())

        assertThat(result).isEqualTo("""internal typealias AType = Int""".trimIndent())
    }

    @Test
    fun `maintains a line break between KDoc and function declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** Some KDoc. */
                fun aFunction()
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                fun aFunction()
            """.trimIndent()
        )
    }

    @Test
    fun `maintains same indent between KDoc and function declaration inside class`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        /** Some KDoc. */
                        fun aFunction() {
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        /** Some KDoc. */
                        fun aFunction() {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains line break between KDoc and a class constructor`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    class MyClass {
                        /** Some KDoc. */
                        constructor() {
                        }
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    class MyClass {
                        /** Some KDoc. */
                        constructor() {
                        }
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains a line break between KDoc and property declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /** Some KDoc. */
                val aProperty
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                val aProperty
            """.trimIndent()
        )
    }

    @Test
    fun `maintains a line break between KDoc and enum value declaration`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    enum class AnEnum {
                        /** Some KDoc. */
                        A_VALUE
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    enum class AnEnum {
                        /** Some KDoc. */
                        A_VALUE
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `maintains same level of indent for enum as its KDoc`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    /** Some KDoc. */
                    enum class AnEnum {
                        A_VALUE
                    }
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some KDoc. */
                    enum class AnEnum {
                        A_VALUE
                    }
                """.trimIndent()
            )
    }

    @Test
    fun `places an initial asterisk on blank KDoc lines`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    /**
                     * Some KDoc.
                     *
                     * Some further explanation.
                     */
                    class AClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * Some KDoc.
                     *
                     * Some further explanation.
                     */
                    class AClass
                """.trimIndent()
            )
    }

    @Test
    fun `maintains original formatting in code blocks in KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    /**
                     * ```
                     * val aVariable
                     * val anotherVariable
                     * ```
                     */
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * ```
                     * val aVariable
                     * val anotherVariable
                     * ```
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `does not collapse KDoc with line break into short form`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /**
                 * Some KDoc.
                 *
                 * Some further text.
                 */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /**
                 * Some KDoc.
                 *
                 * Some further text.
                 */
            """.trimIndent()
        )
    }

    @Test
    fun `inserts whitespace before closing KDoc marker when converting to one-line form`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /**
                 * Some KDoc.
                 */
            """.trimIndent()
        )

        assertThat(result).isEqualTo("""/** Some KDoc. */""".trimIndent())
    }

    @Test
    fun `formats paragraphs in KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /**
                 * Some
                 * KDoc.
                 */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /**
                 * Some KDoc.
                 */
            """.trimIndent()
        )
    }

    @Test
    fun `handles continuation lines of tags in KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    /**
                     * @property aProperty A property
                     *     with some KDoc
                     */
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * @property aProperty A property with
                     *     some KDoc
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `maintains indentation of KDoc tags`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result =
            subject.format(
                """
                    /**
                     * @property aProperty Some KDoc
                     * @property anotherProperty Some other KDoc
                     */
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * @property aProperty Some KDoc
                     * @property anotherProperty Some other
                     *     KDoc
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `preserves newlines in bullet lists in KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /**
                 *  * An item
                 *  * Another item
                 */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /**
                 *  * An item
                 *  * Another item
                 */
            """.trimIndent()
        )
    }

    @Test
    fun `preserves newlines in numbered lists in KDoc`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                /**
                 *  1. An item
                 *  2. Another item
                 */
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /**
                 *  1. An item
                 *  2. Another item
                 */
            """.trimIndent()
        )
    }

    @Test
    fun `maintains spacing between KDoc elements`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    /** Some KDoc with an [element] and more text. */
                    class AClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some KDoc with an [element] and more text. */
                    class AClass
                """.trimIndent()
            )
    }

    @Test
    fun `maintains comment between KDoc and class declaration`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result = subject.format(
            """
                /** Some KDoc. */
                // A comment
                class AClass
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                // A comment
                class AClass
            """.trimIndent()
        )
    }

    @Test
    fun `maintains comment between KDoc and property declaration`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result = subject.format(
            """
                /** Some KDoc. */
                // A comment
                val aProperty
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                /** Some KDoc. */
                // A comment
                val aProperty
            """.trimIndent()
        )
    }

    @Test
    fun `maintains comment between KDoc and function declaration`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result = subject.format(
            """
                /** Some KDoc. */
                // A comment
                fun aFunction()
            """.trimIndent()
        )

        assertThat(result)
            .isEqualTo(
                """
                    /** Some KDoc. */
                    // A comment
                    fun aFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `maintains line break between comment and function after annotation`() {
        val subject = KotlinFormatter(maxLineLength = 40)

        val result = subject.format(
            """
                @AnAnnotation
                // A comment
                fun aFunction()
            """.trimIndent()
        )

        assertThat(result).isEqualTo(
            """
                @AnAnnotation
                // A comment
                fun aFunction()
            """.trimIndent()
        )
    }

    @Test
    fun `maintains spacing in follow-up lines of TODO comment`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    // TODO(ticket): Some item
                    //  with some more information
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    // TODO(ticket): Some item
                    //  with some more information
                """.trimIndent()
            )
    }

    @Test
    fun `maintains line break after TODO comment`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    // TODO(ticket): Some item
                    //  with something
                    fun aFunction()
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    // TODO(ticket): Some item
                    //  with something
                    fun aFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `maintains spacing before KDoc tag`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    /**
                     * A summary fragment with some content.
                     *
                     * @property property A property
                     */
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /**
                     * A summary fragment with some content.
                     *
                     * @property property A property
                     */
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of one-line comments inside statements`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    aVariable =
                    
                        // Some comment text
                        // Some further text
                        anotherVariable + aThirdVariable
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    aVariable =
                    
                        // Some comment text
                        // Some further text
                        anotherVariable + aThirdVariable
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of multi-line comments inside statements`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    aVariable =
                    
                        /* Some comment text which is too long to fit on line
                         * Some further text */
                        anotherVariable + aThirdVariable
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    aVariable =
                        /* Some comment text which is too long to fit on line
                         * Some further text */
                        anotherVariable + aThirdVariable
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of multi-line comments with initial blank line`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    /*
                     * Some comment text which is too long to fit on line
                     * Some further text
                     */
                    aFunction()
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    /* Some comment text which is too long to fit on line
                     * Some further text */
                    aFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of one-line comments inside function calls`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    aFunctionCall(
                        aParameter,
                    
                        // Some comment text
                        // Some further text
                        anotherParameter
                    )
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    aFunctionCall(
                        aParameter,
                    
                        // Some comment text
                        // Some further text
                        anotherParameter
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of one-line comments inside blocks`() {
        val subject = KotlinFormatter(maxLineLength = 60)

        val result =
            subject.format(
                """
                    val aProperty
                    
                    // Some comment text
                    // Some further text
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    val aProperty
                    
                    // Some comment text
                    // Some further text
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of block comments in parameter lists`() {
        val subject = KotlinFormatter(maxLineLength = 25)

        val result =
            subject.format(
                """
                    aFunction(
                        0,
                        /* A comment */ 1,
                        2 /* A comment */,
                        3, /* A comment */
                        4
                    )
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    aFunction(
                        0,
                        /* A comment */ 1,
                        2 /* A comment */,
                        3, /* A comment */
                        4
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `preserves formatting of block and EOL comments in parameter lists`() {
        val subject = KotlinFormatter(maxLineLength = 25)

        val result =
            subject.format(
                """
                    aFunction(
                        0,
                        // An EOL comment
                        /* A comment */ 1
                    )
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    aFunction(
                        0,
                        // An EOL comment
                        /* A comment */ 1
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `preserves whitespace preceding an EOL comment on a statement`() {
        val subject = KotlinFormatter(maxLineLength = 80)

        val result = subject.format("""val aProperty: String // An EOL comment""".trimIndent())

        assertThat(result).isEqualTo("""val aProperty: String // An EOL comment""".trimIndent())
    }

    @Test
    fun `puts simple annotations on the same line as function parameters`() {
        val result = KotlinFormatter().format("fun aFunction(@AnAnnotation aParameter: String)")

        assertThat(result).isEqualTo("fun aFunction(@AnAnnotation aParameter: String)")
    }

    @Test
    fun `breaks at correct location in function calls`() {
        val result =
            KotlinFormatter(maxLineLength = 49)
                .format(
                    """
                        aFunction(
                            anotherFunction(aParameter, anotherParameter),
                            aThirdFunction()
                        )
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    aFunction(
                        anotherFunction(
                            aParameter,
                            anotherParameter
                        ),
                        aThirdFunction()
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `indents parameters at the same level as their annotations`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format("""fun aFunction(@AnAnnotation("Some value") aParameter: String)""")

        assertThat(result)
            .isEqualTo(
                """
                    fun aFunction(
                        @AnAnnotation("Some value")
                        aParameter: String
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `does not break after modifier list when an annotation is present`() {
        val result =
            KotlinFormatter(maxLineLength = 40)
                .format(
                    """class AClass(@AnAnnotation("Some value") private val aParameter: String)"""
                )

        assertThat(result)
            .isEqualTo(
                """
                    class AClass(
                        @AnAnnotation("Some value")
                        private val aParameter: String
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `indents named parameters with continuation indent`() {
        val result =
            KotlinFormatter(maxLineLength = 42)
                .format("""aFunction(aParameter = "A long value which should wrap")""")

        assertThat(result)
            .isEqualTo(
                """
                    aFunction(
                        aParameter =
                            "A long value which should wrap"
                    )
                """.trimIndent()
            )
    }

    @Test
    fun `sorts annotations before modifiers on declarations`() {
        val result =
            KotlinFormatter().format(
                """
                    private
                    @AnAnnotation fun myFunction()
                """.trimIndent()
            )

        assertThat(result).isEqualTo(
            """
                @AnAnnotation
                private fun myFunction()
            """.trimIndent()
        )
    }

    @Test
    fun `sorts modifier keywords on declarations`() {
        val result =
            KotlinFormatter(maxLineLength = 88)
                .format(
                    """
                        data operator infix inline companion enum annotation inner suspend vararg tailrec
                            lateinit override external final open abstract sealed const expect actual public
                            protected private internal fun myFunction()
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    public protected private internal expect actual final open abstract sealed const
                        external override lateinit tailrec vararg suspend inner enum annotation companion
                        inline infix operator data fun myFunction()
                """.trimIndent()
            )
    }

    @Test
    fun `correctly handles sorting of functional interfaces`() {
        val result =
            KotlinFormatter(maxLineLength = 88)
                .format("""public fun interface AnInterface""".trimIndent())

        assertThat(result).isEqualTo("""public fun interface AnInterface""".trimIndent())
    }

    @Test
    fun `removes whitespace between imports`() {
        val result =
            KotlinFormatter(importPolicySupplier = { { _, _ -> true } })
                .format(
                    """
                        import apackage.AClass
                        
                        import apackage.BClass
                    """.trimIndent()
                )

        assertThat(result)
            .isEqualTo(
                """
                    import apackage.AClass
                    import apackage.BClass
                """.trimIndent()
            )
    }

    @Test
    fun `removes imports when directed`() {
        val importPolicy: ImportPolicy = { importName, importPath ->
            !(importName == "AClass" && importPath == "apackage.AClass")
        }
        val subject = KotlinFormatter(importPolicySupplier = { importPolicy })

        val result = subject.format("import apackage.AClass")

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `does not remove the wildcard import for java util`() {
        val subject = KotlinFormatter()

        val result = subject.format("""import java.util.*""".trimIndent())

        assertThat(result).isEqualTo("""import java.util.*""".trimIndent())
    }

    @Test
    fun `does not remove the wildcard import for a custom package`() {
        val subject = KotlinFormatter()

        val result = subject.format("""import apackage.*""".trimIndent())

        assertThat(result).isEqualTo("""import apackage.*""".trimIndent())
    }

    @Test
    fun `retains imports whose alias is used`() {
        val importPolicy: ImportPolicy = { importName, importPath ->
            importName == "AClassAlias" && importPath == "apackage.AClass"
        }
        val subject = KotlinFormatter(importPolicySupplier = { importPolicy })

        val result = subject.format("import apackage.AClass as AClassAlias")

        assertThat(result).isEqualTo("import apackage.AClass as AClassAlias")
    }

    @Test
    fun `removes imports by default according to the standard policy`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    import apackage.AnUnusedClass
                    import apackage.AUsedClass
                    
                    val aVariable: AUsedClass
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    import apackage.AUsedClass
                    
                    val aVariable: AUsedClass
                """.trimIndent()
            )
    }

    @Test
    fun `does not strip trailing whitespace in multiline string literals`() {
        val subject = KotlinFormatter()

        val result =
            subject.format(
                """
                    ""${'"'}Before whitespace  
                    After whitespace""${'"'}
                """.trimIndent()
            )

        assertThat(result)
            .isEqualTo(
                """
                    ""${'"'}Before whitespace  
                    After whitespace""${'"'}
                """.trimIndent()
            )
    }

    @Test
    fun `resets state after every file`() {
        val subject = KotlinFormatter()
        subject.format("package org.kotlin.formatter")

        val result = subject.format("package org.kotlin.formatter")

        assertThat(result).isEqualTo("package org.kotlin.formatter")
    }

    @Nested
    inner class FormatFile {
        private val originalOut = System.out

        @AfterEach
        fun resetOut() {
            System.setOut(originalOut)
        }

        @Test
        fun `formatFile formats the given file`(@TempDir directory: Path) {
            val filePath = directory.resolve("file.kt")
            filePath.toFile().writeText("class MyClass {\nval aProperty: String\n}", Charsets.UTF_8)
            val subject = KotlinFormatter()

            subject.formatFile(filePath)

            assertThat(filePath.toFile().readText(Charsets.UTF_8))
                .isEqualTo(
                    """
                        class MyClass {
                            val aProperty: String
                        }
                    """.trimIndent()
                )
        }

        @Test
        fun `formatFile reports an error with line number information`(@TempDir directory: Path) {
            val filePath = directory.resolve("file.kt")
            filePath.toFile().writeText("/* Some comment */\n\nif INVALID\n", Charsets.UTF_8)
            val stream = redirectOutput()
            val subject = KotlinFormatter()

            subject.formatFile(filePath)

            assertThat(String(stream.toByteArray(), Charsets.UTF_8)).contains("(line 3)")
        }

        @Test
        fun `formatFile reports an error on the last line`(@TempDir directory: Path) {
            val filePath = directory.resolve("file.kt")
            filePath.toFile().writeText("/* Some comment */\n\nif INVALID", Charsets.UTF_8)
            val stream = redirectOutput()
            val subject = KotlinFormatter()

            subject.formatFile(filePath)

            assertThat(String(stream.toByteArray(), Charsets.UTF_8)).contains("(line 3)")
        }

        @Test
        fun `formatFile reports an error on the first line`(@TempDir directory: Path) {
            val filePath = directory.resolve("file.kt")
            filePath.toFile().writeText("if INVALID\n", Charsets.UTF_8)
            val stream = redirectOutput()
            val subject = KotlinFormatter()

            subject.formatFile(filePath)

            assertThat(String(stream.toByteArray(), Charsets.UTF_8)).contains("(line 1)")
        }

        private fun redirectOutput(): ByteArrayOutputStream {
            val stream = ByteArrayOutputStream()
            System.setOut(PrintStream(stream))
            return stream
        }
    }
}
