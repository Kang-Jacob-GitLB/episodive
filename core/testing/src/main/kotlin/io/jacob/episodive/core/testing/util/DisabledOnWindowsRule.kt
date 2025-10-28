package io.jacob.episodive.core.testing.util

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisabledOnWindows

class DisabledOnWindowsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (description.getAnnotation(DisabledOnWindows::class.java) != null) {
                    val osName = System.getProperty("os.name")
                    if (osName.contains("Windows", ignoreCase = true)) {
                        println("\u001B[33m[!] Test skipped on Windows: ${description.methodName}\u001B[0m")
                        return
                    }
                }
                base.evaluate()
            }
        }
    }
}