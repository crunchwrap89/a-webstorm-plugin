package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
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
            acceptanceCriteria = emptyList(),
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
            acceptanceCriteria = emptyList(),
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
            acceptanceCriteria = emptyList(),
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

    @Test
    fun `test generate failure prompt`() {
        val feature = BacklogFeature(
            name = "Test Feature",
            checked = false,
            description = "Desc",
            acceptanceCriteria = emptyList(),
            optionalSections = emptyMap(),
            rawBlock = "",
            blockStartOffset = 0,
            blockEndOffset = 0
        )

        val failures = listOf(
            FailureDetail(AcceptanceCriterion.FileExists("test.txt"), "File missing: test.txt"),
            FailureDetail(AcceptanceCriterion.CommandSucceeds("npm test"), "Command failed")
        )

        val prompt = PromptGenerator.generateFailurePrompt(feature, failures)

        assertTrue(prompt.contains("The implementation of feature 'Test Feature' failed verification."))
        assertTrue(prompt.contains("Criterion: File exists: test.txt"))
        assertTrue(prompt.contains("Error Details:"))
        assertTrue(prompt.contains("File missing: test.txt"))
        assertTrue(prompt.contains("Criterion: Command succeeds: npm test"))
    }
}

