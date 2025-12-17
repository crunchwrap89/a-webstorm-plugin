package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings.OrchestratorSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.EnvironmentUtil
import java.io.File

data class VerificationResult(
    val success: Boolean,
    val details: List<String>,
    val failures: List<FailureDetail> = emptyList()
)

data class FailureDetail(
    val criterion: AcceptanceCriterion,
    val message: String
)

object AcceptanceVerifier {
    fun verify(project: Project, criteria: List<AcceptanceCriterion>): VerificationResult {
        if (criteria.isEmpty()) return VerificationResult(false, listOf("No acceptance criteria present."))
        val settings = project.service<OrchestratorSettings>()
        val timeoutMs = settings.commandTimeoutSeconds * 1000
        val details = mutableListOf<String>()
        val failures = mutableListOf<FailureDetail>()
        var allOk = true
        criteria.forEach { c ->
            when (c) {
                is AcceptanceCriterion.FileExists -> {
                    var exists = false
                    runReadAction {
                        val roots = ProjectRootManager.getInstance(project).contentRoots
                        for (root in roots) {
                            if (root.findFileByRelativePath(c.relativePath) != null) {
                                exists = true
                                break
                            }
                        }

                        if (!exists) {
                            val path = project.basePath?.let { File(it, c.relativePath) } ?: File(c.relativePath)
                            exists = path.exists()
                            if (exists) LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path)
                        }
                    }

                    if (exists) {
                        details += "✔ File exists: ${c.relativePath}"
                    } else {
                        val msg = "✘ File missing: ${c.relativePath}"
                        details += msg
                        failures += FailureDetail(c, msg)
                    }
                    allOk = allOk && exists
                }
                is AcceptanceCriterion.CommandSucceeds -> {
                    val cmd = c.command
                    val commandLine = if (isWindows()) {
                        GeneralCommandLine("cmd", "/c", cmd)
                    } else {
                        val shell = System.getenv("SHELL") ?: "/bin/sh"
                        // Use -c instead of -lc to rely on EnvironmentUtil's captured environment
                        // and avoid potential issues with double-sourcing shell profiles.
                        GeneralCommandLine(shell, "-c", cmd)
                    }
                    project.basePath?.let { commandLine.withWorkDirectory(it) }

                    if (!isWindows()) {
                        try {
                            val env = EnvironmentUtil.getEnvironmentMap()
                            commandLine.withEnvironment(env)
                        } catch (e: Exception) {
                            // Ignore if we can't get the environment
                        }
                    }

                    val handler = CapturingProcessHandler(commandLine)
                    val output = handler.runProcess(timeoutMs)
                    val ok = output.exitCode == 0

                    val sb = StringBuilder()
                    if (ok) {
                        sb.append("✔ Command succeeded: $cmd")
                    } else {
                        sb.append("✘ Command failed ($cmd): exit=${output.exitCode}")
                        sb.append("\nWorking Dir: ${commandLine.workDirectory}")
                        sb.append("\nSTDOUT:\n${output.stdout}")
                        sb.append("\nSTDERR:\n${output.stderr}")

                        // Heuristic for missing dependencies
                        if (output.stderr.contains("not installed", ignoreCase = true) ||
                            output.stderr.contains("command not found", ignoreCase = true)) {
                            sb.append("\n\nHINT: You may need to run 'npm install' or 'yarn install' in your project.")
                        }
                        failures += FailureDetail(c, sb.toString())
                    }
                    details += sb.toString()
                    allOk = allOk && ok
                }
            }
        }
        return VerificationResult(allOk, details, failures)
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
