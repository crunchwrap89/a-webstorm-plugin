package com.github.crunchwrap89.featureorchestrator.featureorchestrator.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OrchestratorSettingsTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val settings = project.service<OrchestratorSettings>()
        settings.loadState(OrchestratorSettingsState()) // Reset to defaults
    }

    fun `test default settings`() {
        val settings = project.service<OrchestratorSettings>()

        println("DEBUG: settings.completionBehavior = ${settings.completionBehavior}")
        assertEquals("Completion behavior should be MOVE_TO_COMPLETED by default", CompletionBehavior.MOVE_TO_COMPLETED, settings.completionBehavior)
        assertEquals(PromptHandoffBehavior.AUTO_AI_ASSISTANT, settings.promptHandoffBehavior)
        assertTrue(settings.featureTemplate.contains("## Feature name"))
    }

    fun `test persistence`() {
        val settings = project.service<OrchestratorSettings>()

        settings.completionBehavior = CompletionBehavior.CHECK_OFF
        settings.promptHandoffBehavior = PromptHandoffBehavior.AUTO_AI_ASSISTANT
        settings.featureTemplate = "New Template"

        val state = settings.state
        assertEquals(CompletionBehavior.CHECK_OFF, state.completionBehavior)
        assertEquals(PromptHandoffBehavior.AUTO_AI_ASSISTANT, state.promptHandoffBehavior)
        assertEquals("New Template", state.featureTemplate)

        // Simulate reload
        val newSettings = OrchestratorSettings(project)
        newSettings.loadState(state)

        assertEquals(CompletionBehavior.CHECK_OFF, newSettings.completionBehavior)
        assertEquals(PromptHandoffBehavior.AUTO_AI_ASSISTANT, newSettings.promptHandoffBehavior)
        assertEquals("New Template", newSettings.featureTemplate)
    }
}
