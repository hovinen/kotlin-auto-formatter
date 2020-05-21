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
import org.kotlin.formatter.LeafNodeToken
import org.kotlin.formatter.Token

class NodePatternTest {
    @Test
    fun `accumulates and consumes single matching node`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfType(KtTokens.CLASS_KEYWORD) thenMapToTokens {
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
            nodeOfOneOfTypes(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD) thenMapToTokens {
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
    fun `accepts nodes not of a given type`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeNotOfType(KtTokens.CLASS_KEYWORD) thenMapToTokens {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes = listOf(LeafPsiElement(KtTokens.FUN_KEYWORD, "fun"))

        subject.matchSequence(nodes)

        assertThat(accumulatedNodes).isEqualTo(nodes)
    }

    @Test
    fun `rejects nodes of a type not being accepted`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeNotOfType(KtTokens.CLASS_KEYWORD) thenMapToTokens {
                accumulatedNodes.addAll(it)
                listOf()
            }
            end()
        }
        val nodes = listOf(LeafPsiElement(KtTokens.CLASS_KEYWORD, "class"))

        assertThrows<Exception> { subject.matchSequence(nodes) }
    }

    @Test
    fun `throws when sequence is not accepted`() {
        val accumulatedNodes = mutableListOf<ASTNode>()
        val subject = nodePattern {
            nodeOfType(KtTokens.CLASS_KEYWORD) thenMapToTokens {
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
            nodeOfType(KtTokens.IDENTIFIER) thenMapToTokens {
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
            anyNode() thenMapToTokens { listOf() }
            anyNode() thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            anyNode() thenMapToTokens {
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
            anyNode() thenMapToTokens {
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
                zeroOrMore { anyNode() } thenMapToTokens { nodes ->
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
    fun `accepts two matching tokens with Kleene star (frugal mode)`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMoreFrugal {
                nodeOfType(KtTokens.CLASS_KEYWORD)
            } thenMapToTokens {
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
    fun `invokes inner action of zeroOrMore only if tokens matched`() {
        var actionInvoked = false
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.IDENTIFIER) thenMapToTokens {
                    actionInvoked = true
                    listOf()
                }
            }
            end()
        }

        subject.matchSequence(listOf())

        assertThat(actionInvoked).isFalse()
    }

    @Test
    fun `invokes inner action only if tokens matched on frugal matcher`() {
        var actionInvoked = false
        val subject = nodePattern {
            zeroOrMoreFrugal {
                nodeOfType(KtTokens.IDENTIFIER) thenMapToTokens {
                    actionInvoked = true
                    listOf()
                }
            }
            end()
        }

        subject.matchSequence(listOf())

        assertThat(actionInvoked).isFalse()
    }

    @Test
    fun `accepts as many matching nodes as possible on Kleene star`() {
        var accumulatedNodes = listOf<ASTNode>()
        val subject = nodePattern {
            zeroOrMore {
                nodeOfType(KtTokens.IDENTIFIER)
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
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
                anyNode() thenMapToTokens {
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
            } thenMapToTokens {
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
            } thenMapToTokens {
                accumulatedNodes = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.VAR_KEYWORD, "var")

        subject.matchSequence(listOf(element))

        assertThat(accumulatedNodes).isEqualTo(listOf(element))
    }

    @Test
    fun `allows additional processing of nodes on a given group`() {
        var tokens = listOf<Token>()
        val subject = nodePattern {
            exactlyOne {
                oneOrMore {
                    anyNode() thenMapToTokens { listOf(LeafNodeToken("a")) }
                }
            } thenMapTokens {
                tokens = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        subject.matchSequence(listOf(element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a")))
    }

    @Test
    fun `executes action on node before mapping tokens`() {
        var tokens = listOf<Token>()
        val subject = nodePattern {
            exactlyOne {
                oneOrMore {
                    anyNode()
                } thenMapToTokens { listOf(LeafNodeToken("a")) }
            } thenMapTokens {
                tokens = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        subject.matchSequence(listOf(element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a")))
    }

    @Test
    fun `does not include tokens from previous group in input to token mapper`() {
        var tokens = listOf<Token>()
        val subject = nodePattern {
            anyNode() thenMapToTokens { listOf(LeafNodeToken("omitted")) }
            exactlyOne {
                oneOrMore {
                    anyNode() thenMapToTokens { listOf(LeafNodeToken("a")) }
                }
            } thenMapTokens {
                tokens = it
                listOf()
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        subject.matchSequence(listOf(element, element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a")))
    }

    @Test
    fun `does not repeatedly push new empty token lists to the token stack`() {
        val subject = nodePattern {
            anyNode() thenMapToTokens { listOf(LeafNodeToken("a")) }
            exactlyOne {
                oneOrMore {
                    anyNode() thenMapToTokens { listOf(LeafNodeToken("b")) }
                }
            } thenMapTokens {
                listOf(it.first())
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        val tokens = subject.matchSequence(listOf(element, element, element, element, element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a"), LeafNodeToken("b")))
    }

    @Test
    fun `passes tokens processed in previous steps to the output`() {
        val subject = nodePattern {
            anyNode() thenMapToTokens { listOf(LeafNodeToken("a")) }
            exactlyOne {
                oneOrMore {
                    anyNode() thenMapToTokens { listOf(LeafNodeToken("b")) }
                }
            } thenMapTokens {
                it
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        val tokens = subject.matchSequence(listOf(element, element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a"), LeafNodeToken("b")))
    }

    @Test
    fun `passes tokens processed in previous steps to the output in more complicated case`() {
        val subject = nodePattern {
            anyNode() thenMapToTokens { listOf(LeafNodeToken("a")) }
            possibleWhitespace()
            exactlyOne {
                oneOrMore {
                    anyNode() thenMapToTokens { listOf(LeafNodeToken("b")) }
                }
            } thenMapTokens {
                it
            }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        val tokens = subject.matchSequence(listOf(element, element))

        assertThat(tokens).isEqualTo(listOf(LeafNodeToken("a"), LeafNodeToken("b")))
    }

    @Test
    fun `handles long input without running out of memory`() {
        val subject = nodePattern {
            zeroOrMore { anyNode() }
            zeroOrMore { anyNode() }
            zeroOrMore { anyNode() }
            end()
        }
        val element = LeafPsiElement(KtTokens.IDENTIFIER, "anyElement")

        subject.matchSequence((0..1000).map { element })

        // Assert that nothing is thrown.
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
