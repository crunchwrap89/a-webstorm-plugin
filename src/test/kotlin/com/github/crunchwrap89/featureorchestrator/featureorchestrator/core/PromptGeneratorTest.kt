package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Section
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptGeneratorTest {

    @Test
    fun `test generate prompt with skills`() {
        val feature = BacklogFeature(
            name = "Test Feature",
            checked = false,
            description = "Test Description",
            optionalSections = emptyMap(),
            rawBlock = "",
            blockStartOffset = 0,
            blockEndOffset = 0
        )
        val skills = listOf(
            Skill("pdf-processing", "Extracts text from PDF", "/path/to/pdf/SKILL.md"),
            Skill("data-analysis", "Analyzes datasets", "/path/to/data/SKILL.md")
        )

        val prompt = PromptGenerator.generate(feature, skills)

        assertTrue(prompt.contains("<available_skills>"))
        assertTrue(prompt.contains("<skill>"))
        assertTrue(prompt.contains("<name>pdf-processing</name>"))
        assertTrue(prompt.contains("<description>Extracts text from PDF</description>"))
        assertTrue(prompt.contains("<location>/path/to/pdf/SKILL.md</location>"))
        assertTrue(prompt.contains("</available_skills>"))
    }

    @Test
    fun `test generate prompt basic`() {
        val feature = BacklogFeature(
            name = "Test Feature",
            checked = false,
            description = "Test Description",
            optionalSections = emptyMap(),
            rawBlock = "",
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        val prompt = PromptGenerator.generate(feature)

        assertTrue(prompt.contains("Implement Feature: Test Feature"))
        assertTrue(prompt.contains("Description:"))
        assertTrue(prompt.contains("Test Description"))
    }

    @Test
    fun `test generate prompt with sections`() {
        val feature = BacklogFeature(
            name = "Test Feature",
            checked = false,
            description = "Test Description",
            optionalSections = mapOf(
                Section.REQUIREMENTS to "- Req 1\n- Req 2",
                Section.OUT_OF_SCOPE to "Nothing"
            ),
            rawBlock = "",
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        val prompt = PromptGenerator.generate(feature)

        assertTrue(prompt.contains("Requirements:"))
        assertTrue(prompt.contains("- Req 1"))
        assertTrue(prompt.contains("Out of Scope:"))
        assertTrue(prompt.contains("Nothing"))
    }
}

