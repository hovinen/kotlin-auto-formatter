package org.kotlin.formatter.scanning.nodepattern

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NodePatternTest {
    @Test
    fun `accumulates and consumes single matching node`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfType(KtTokens.CLASS_KEYWORD) andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes = listOf(LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"))

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @ParameterizedTest
    @MethodSource("valVarNodeCases")
    fun `accepts nodes of given types`(node: ASTNode) {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfOneOfTypes(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD) andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes = listOf(node)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `throws when sequence is not accepted`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfType(KtTokens.CLASS_KEYWORD) andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes = listOf(LeafPsiElement(KtTokens.FUN_KEYWORD, "fun"))

        assertThrows<Exception> {
            subject.matchSequence(nodes)
        }
    }

    @Test
    fun `throws away non-consumed nodes`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfType(KtTokens.CLASS_KEYWORD)
            nodeOfType(KtTokens.IDENTIFIER) andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val identifierElement = LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                identifierElement
            )
        val nodesExpected = listOf(identifierElement)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodesExpected)
    }

    @Test
    fun `does not spill node from one action into the next`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            anyNode() andThen { listOf() }
            anyNode() andThen {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val identifierElement = LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                identifierElement
            )
        val nodesExpected = listOf(identifierElement)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodesExpected)
    }

    @Test
    fun `accumulates a group of tokens to appear exactly once`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            exactlyOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                nodeOfType(KtTokens.IDENTIFIER)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
            )

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `executes actions after nested groups (exactlyOne)`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            exactlyOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                exactlyOne {
                    nodeOfType(KtTokens.IDENTIFIER)
                }
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
            )

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `executes actions after nested groups (zeroOrOne)`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            exactlyOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                zeroOrOne {
                    nodeOfType(KtTokens.IDENTIFIER)
                }
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
            )

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `executes an action on a single node before a Kleene star element`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            anyNode() andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            zeroOrMore { anyNode() }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")
        val nodes = listOf(classElement, LeafPsiElement(KtTokens.IDENTIFIER, "MyClass"))
        val nodesExpected = listOf(classElement)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodesExpected)
    }

    @Test
    fun `executes an action on a single node after a Kleene star element`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore { anyNode() }
            anyNode() andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val identifierElement = LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
        val nodes = listOf(LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"), identifierElement)
        val nodesExpected = listOf(identifierElement)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodesExpected)
    }

    @Test
    fun `accumulates nodes on nested zeroOrMore`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrOne {
                zeroOrMore { anyNode() } andThen { nodes ->
                    accumulatedNodes.addAll(nodes)
                    listOf()
                }
            }
            end()
        }
        val identifierElement = LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
        val nodes = listOf(identifierElement)
        val nodesExpected = listOf(identifierElement)

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodesExpected)
    }

    @Test
    fun `accepts and accumulates a group of tokens to appear zero or one times`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                nodeOfType(KtTokens.IDENTIFIER)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes =
            listOf(
                LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"),
                LeafPsiElement(KtTokens.IDENTIFIER, "MyClass")
            )

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `accepts absence of a group of tokens to appear zero or one times`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                nodeOfType(KtTokens.IDENTIFIER)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }

        subject.matchSequence(listOf())

        assertThat(accumulatedNodes).isEmpty()
    }

    @Test
    fun `executes action on group of nodes when optional subgroup is missing`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            exactlyOne {
                nodeOfType(KtTokens.CLASS_KEYWORD)
                zeroOrOne {
                    nodeOfType(KtTokens.IDENTIFIER)
                }
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(classElement))
    }

    @Test
    fun `accepts zero matching tokens with Kleene star`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }

        subject.matchSequence(listOf())

        assertThat(accumulatedNodes).isEqualTo(listOf<ASTNode>())
    }

    @Test
    fun `accepts one matching token with Kleene star`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(classElement))
    }

    @Test
    fun `accepts two matching tokens with Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement, classElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(classElement, classElement))
    }

    @Test
    fun `accepts as many matching nodes as possible on Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.IDENTIFIER)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            zeroOrMore { anyNode() }
            end()
        }
        val variableElement = LeafPsiElement(KtTokens.IDENTIFIER, "aVariable")

        subject.matchSequence(listOf(variableElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(variableElement))
    }

    @Test
    fun `accepts as few matching nodes as possible on frugal Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMoreFrugal {
                anyNode()
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            zeroOrMore { nodeOfType(KtTokens.IDENTIFIER) }
            end()
        }
        val variableElement = LeafPsiElement(KtTokens.IDENTIFIER, "aVariable")

        subject.matchSequence(listOf(variableElement))

        assertThat(accumulatedNodes).isEqualTo(listOf<ASTNode>())
    }

    @Test
    fun `accepts one matching token with one plus Kleene star`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            oneOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(classElement))
    }

    @Test
    fun `accepts two matching tokens with one plus Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            oneOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement, classElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(classElement, classElement))
    }

    @Test
    fun `accepts as many matching nodes as possible on one plus Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            oneOrMore {
                nodeOfType(KtTokens.IDENTIFIER)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            zeroOrMore { anyNode() }
            end()
        }
        val variableElement = LeafPsiElement(KtTokens.IDENTIFIER, "aVariable")

        subject.matchSequence(listOf(variableElement, variableElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(variableElement, variableElement))
    }

    @Test
    fun `accepts as few matching nodes as possible on frugal one plus Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            oneOrMoreFrugal {
                anyNode()
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            zeroOrMore { nodeOfType(KtTokens.IDENTIFIER) }
            end()
        }
        val variableElement = LeafPsiElement(KtTokens.IDENTIFIER, "aVariable")

        subject.matchSequence(listOf(variableElement, variableElement))

        assertThat(accumulatedNodes).isEqualTo(listOf(variableElement))
    }

    @Test
    fun `invokes callback only once on one plus Kleene star`() {
        var calledCount = 0
        val subject = nodePattern {
            oneOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                calledCount++
                listOf()
            }
            end()
        }
        val classElement = LeafPsiElement(KtTokens.CLASS_KEYWORD, "class")

        subject.matchSequence(listOf(classElement, classElement))

        assertThat(calledCount).isEqualTo(1)
    }

    @Test
    fun `rejects zero matching tokens with one plus Kleene star`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            oneOrMore {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } andThen {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }

        assertThrows<Exception> { subject.matchSequence(listOf()) }
    }

    @Test
    fun `does not invoke command on branch which was not invoked`() {
        var wasCalled = false
        val subject = nodePattern {
            zeroOrMore { anyNode() }
            zeroOrOne {
                nodeOfType(KtTokens.EQ)
                anyNode() andThen {
                    wasCalled = true
                    listOf()
                }
            }
            end()
        }
        val variableElement = LeafPsiElement(KtTokens.IDENTIFIER, "aVariable")

        subject.matchSequence(listOf(variableElement))

        assertThat(wasCalled).isFalse()
    }

    @Test
    fun `accepts first branch of an either-or block`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            either {
                nodeOfType(KtTokens.VAL_KEYWORD)
            } or {
                nodeOfType(KtTokens.VAR_KEYWORD)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.VAL_KEYWORD, "val")

        subject.matchSequence(listOf(element))

        assertThat(accumulatedNodes).isEqualTo(listOf(element))
    }

    @Test
    fun `accepts second branch of an either-or block`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            either {
                nodeOfType(KtTokens.VAL_KEYWORD)
            } or {
                nodeOfType(KtTokens.VAR_KEYWORD)
            } andThen {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.VAR_KEYWORD, "var")

        subject.matchSequence(listOf(element))

        assertThat(accumulatedNodes).isEqualTo(listOf(element))
    }

    companion object {
        @JvmStatic
        fun valVarNodeCases(): List<Arguments> =
            listOf(
                Arguments.of(LeafPsiElement(KtTokens.VAL_KEYWORD, "val")),
                Arguments.of(LeafPsiElement(KtTokens.VAR_KEYWORD, "var"))
            )
    }
}
