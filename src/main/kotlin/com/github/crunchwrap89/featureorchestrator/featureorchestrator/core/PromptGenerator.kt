package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Section
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill

object PromptGenerator {
    fun generate(feature: BacklogFeature, skills: List<Skill> = emptyList()): String {
        val sb = StringBuilder()

        if (skills.isNotEmpty()) {
            sb.appendLine("<available_skills>")
            skills.forEach { skill ->
                sb.appendLine("  <skill>")
                sb.appendLine("    <name>${skill.name}</name>")
                sb.appendLine("    <description>${skill.description}</description>")
                sb.appendLine("    <location>${skill.path}</location>")
                sb.appendLine("  </skill>")
            }
            sb.appendLine("</available_skills>")
            sb.appendLine()
        }

        sb.appendLine("Implement Feature: ${feature.name}")
        sb.appendLine()
        sb.appendLine("Description:")
        sb.appendLine(feature.description.trim())
        sb.appendLine()

        fun appendIfPresent(section: Section, title: String) {
            val body = feature.optionalSections[section]?.trim().orEmpty()
            if (body.isNotBlank()) {
                sb.appendLine("$title:")
                sb.appendLine(body)
                sb.appendLine()
            }
        }

        appendIfPresent(Section.REQUIREMENTS, "Requirements")
        appendIfPresent(Section.OUT_OF_SCOPE, "Out of Scope")
        appendIfPresent(Section.NOTES, "Notes")
        appendIfPresent(Section.CONTEXT, "Context")

        return sb.toString().trimEnd()
    }
}
