package com.github.crunchwrap89.featureorchestrator.featureorchestrator.model

import com.intellij.openapi.vfs.VirtualFile

data class Backlog(
    val features: List<BacklogFeature>,
    val warnings: List<String> = emptyList(),
)

data class BacklogFeature(
    val name: String,
    val checked: Boolean,
    val description: String,
    val optionalSections: Map<Section, String> = emptyMap(),
    val acceptanceCriteria: List<AcceptanceCriterion> = emptyList(),
    val rawBlock: String,
    val blockStartOffset: Int,
    val blockEndOffset: Int,
)

data class Skill(
    val name: String,
    val description: String,
    val path: String
)

enum class Section {
    REQUIREMENTS, OUT_OF_SCOPE, ACCEPTANCE_CRITERIA, NOTES, CONTEXT
}

sealed interface AcceptanceCriterion {
    data class FileExists(val relativePath: String) : AcceptanceCriterion
    data class CommandSucceeds(val command: String) : AcceptanceCriterion
    object NoTestsFail : AcceptanceCriterion
    data class ManualVerification(val description: String) : AcceptanceCriterion
}

enum class OrchestratorState {
    IDLE, HANDOFF, AWAITING_AI, VERIFYING, COMPLETED, FAILED
}

enum class BacklogStatus {
    OK, MISSING, NO_FEATURES
}

data class ExecutionSession(
    val feature: BacklogFeature,
    val changedFiles: MutableSet<VirtualFile> = linkedSetOf(),
    var statusMessage: String = "",
)
