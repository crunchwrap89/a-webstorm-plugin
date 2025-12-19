package com.github.crunchwrap89.featureorchestrator.featureorchestrator.core

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.AcceptanceCriterion
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AcceptanceVerifierTest : BasePlatformTestCase() {

    fun `test verify file exists`() {
        val criteria = listOf(AcceptanceCriterion.FileExists("test.txt"))

        // File does not exist
        var result = AcceptanceVerifier.verify(project, criteria)
        assertFalse(result.success)
        assertEquals(1, result.failures.size)
        assertTrue(result.failures[0].message.contains("File missing"))

        // Create file
        myFixture.addFileToProject("test.txt", "content")

        // File exists
        result = AcceptanceVerifier.verify(project, criteria)
        assertTrue(result.success)
        assertTrue(result.failures.isEmpty())
    }

    fun `test verify manual verification`() {
        val criteria = listOf(AcceptanceCriterion.ManualVerification("Check UI"))

        val result = AcceptanceVerifier.verify(project, criteria)

        // Manual verification is considered success in auto-check phase, but added to manualVerifications list
        assertTrue(result.success)
        assertEquals(1, result.manualVerifications.size)
        assertEquals("Check UI", result.manualVerifications[0].description)
    }

    fun `test verify mixed criteria`() {
        val criteria = listOf(
            AcceptanceCriterion.FileExists("test.txt"),
            AcceptanceCriterion.ManualVerification("Check UI")
        )

        // File missing
        var result = AcceptanceVerifier.verify(project, criteria)
        assertFalse(result.success)
        assertEquals(1, result.failures.size)
        assertEquals(1, result.manualVerifications.size)

        // Create file
        myFixture.addFileToProject("test.txt", "content")

        // File exists
        result = AcceptanceVerifier.verify(project, criteria)
        assertTrue(result.success)
        assertTrue(result.failures.isEmpty())
        assertEquals(1, result.manualVerifications.size)
    }
}

