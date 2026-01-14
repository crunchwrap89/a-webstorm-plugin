package com.github.crunchwrap89.featureorchestrator.startup

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.skills.SkillService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }
}