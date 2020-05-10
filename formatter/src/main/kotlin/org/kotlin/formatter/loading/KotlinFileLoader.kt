package org.kotlin.formatter.loading

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import sun.reflect.ReflectionFactory

/** Parses Kotlin content into an abstract syntax tree. */
class KotlinFileLoader {
    private val psiFileFactory: PsiFileFactory

    init {
        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val environment =
            KotlinCoreEnvironment.createForProduction(
                Disposable {}, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        installPomModel(environment)
        psiFileFactory = PsiFileFactory.getInstance(environment.project)
    }

    /** Parses the given [code], returning the corresponding top-level [ASTNode]. */
    fun parseKotlin(code: String): ASTNode {
        val file = psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, code)
        return file.node
    }

    companion object {
        private fun installPomModel(environment: KotlinCoreEnvironment) {
            val pomModel: PomModel = object : UserDataHolderBase(), PomModel {
                override fun runTransaction(transaction: PomTransaction) {
                    (transaction as PomTransactionBase).run()
                }

                @Suppress("UNCHECKED_CAST")
                override fun <T : PomModelAspect> getModelAspect(aspect: Class<T>): T? {
                    if (aspect == TreeAspect::class.java) {
                        val constructor =
                            ReflectionFactory.getReflectionFactory()
                                .newConstructorForSerialization(
                                    aspect, Any::class.java.getDeclaredConstructor())
                        return constructor.newInstance() as T
                    } else {
                        return null
                    }
                }
            }
            val project = environment.project as MockProject
            project.registerService(PomModel::class.java, pomModel)
        }
    }
}