package com.github.crunchwrap89.featureorchestrator.featureorchestrator.ui

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.core.OrchestratorController
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogFeature
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.BacklogStatus
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.OrchestratorState
import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.Skill
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI
import com.intellij.ui.OnePixelSplitter
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.JPanel
import javax.swing.BoxLayout
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep

class FeatureOrchestratorToolWindowFactory : ToolWindowFactory {
    override suspend fun isApplicableAsync(project: Project): Boolean {
        return true
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = FeatureOrchestratorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}

private class FeatureOrchestratorPanel(private val project: Project) : JBPanel<FeatureOrchestratorPanel>(BorderLayout()), OrchestratorController.Listener {
    private val featureName = JBLabel("", javax.swing.SwingConstants.CENTER)
    private val emptyBacklogLabel = JBLabel("Backlog is empty", javax.swing.SwingConstants.CENTER)
    private val featureDesc = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = null
        border = null
        rows = 10
    }
    private val prevButton = JButton("<").apply { isEnabled = false }
    private val nextButton = JButton(">").apply { isEnabled = false }
    private val runButton = JButton("▶ Generate prompt")
    private val editBacklogButton = JButton("Edit Backlog").apply { isVisible = false }
    private val logArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        rows = 10
    }
    private val clearLogButton = JButton("Clear Log").apply {
        addActionListener { logArea.text = "" }
    }

    private val skillsPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        border = IdeBorderFactory.createTitledBorder("Agent Skills")
        preferredSize = Dimension(-1, 210)
    }

    private val addFeatureButton = JButton("+").apply {
        toolTipText = "Add Feature"
        addActionListener {
            val popup = JBPopupFactory.getInstance().createListPopup(
                object : BaseListPopupStep<String>("Add Feature", listOf("Empty Feature", "Template Feature")) {
                    override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                        if (finalChoice) {
                            when (selectedValue) {
                                "Empty Feature" -> controller.addEmptyFeature()
                                "Template Feature" -> controller.addTemplateFeature()
                            }
                        }
                        return PopupStep.FINAL_CHOICE
                    }
                }
            )
            popup.showUnderneathOf(this)
        }
    }
    private val editFeatureButton = JButton("✎").apply {
        toolTipText = "Edit Feature"
        addActionListener { controller.editFeature() }
    }
    private val removeFeatureButton = JButton("-").apply {
        toolTipText = "Remove Feature"
        addActionListener { controller.removeFeature() }
    }
    private val completeFeatureButton = JButton("✓").apply {
        toolTipText = "Mark as Completed"
        addActionListener { controller.completeFeature() }
    }

    private val controller = OrchestratorController(project, this)
    private var lastStatus: BacklogStatus = BacklogStatus.OK

    private val createBacklogButton = JButton("Create Backlog").apply {
        addActionListener { controller.createOrUpdateBacklog() }
    }

    // Accessible variable for navPanel
    private val cardLayout = CardLayout()
    private val centerNavPanel = JBPanel<JBPanel<*>>(cardLayout).apply {
        add(featureName, "LABEL")
        add(createBacklogButton, "BUTTON")
        add(emptyBacklogLabel, "EMPTY")
    }

    init {
        preferredSize = Dimension(600, 600)
        
        // Initial skills loading should be done outside EDT or handled gracefully
        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = controller.getAvailableSkills()
            ApplicationManager.getApplication().invokeLater {
                refreshSkills(skills)
            }
        }

        val featureCard = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = IdeBorderFactory.createRoundedBorder()

            val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())


            val navPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(prevButton, BorderLayout.WEST)
                add(centerNavPanel, BorderLayout.CENTER)
                add(nextButton, BorderLayout.EAST)
            }

            val featureActionsPanel = WrappingPanel(FlowLayout(FlowLayout.CENTER, 5, 0)).apply {
                add(addFeatureButton)
                add(editFeatureButton)
                add(removeFeatureButton)
                add(completeFeatureButton)
            }

            val navContainer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(featureActionsPanel, BorderLayout.NORTH)
                add(navPanel, BorderLayout.CENTER)
            }

            contentPanel.add(navContainer, BorderLayout.NORTH)
            val scrollPane = JBScrollPane(featureDesc)
            contentPanel.add(scrollPane, BorderLayout.CENTER)
            contentPanel.add(skillsPanel, BorderLayout.SOUTH)

            add(contentPanel, BorderLayout.CENTER)
        }

        val buttons = WrappingPanel(FlowLayout(FlowLayout.CENTER)).apply {
            add(runButton)
            add(editBacklogButton)
        }

        val logPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = IdeBorderFactory.createRoundedBorder()

            val logHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(5)
                add(JBLabel("Execution Log").apply { font = JBUI.Fonts.label().asBold() }, BorderLayout.WEST)
            }

            add(logHeader, BorderLayout.NORTH)
            add(JBScrollPane(logArea), BorderLayout.CENTER)
            add(clearLogButton, BorderLayout.SOUTH)
        }

        val topPanel = JBPanel<JBPanel<*>>(BorderLayout())
        topPanel.add(featureCard, BorderLayout.NORTH)
        topPanel.add(buttons, BorderLayout.SOUTH)

        val splitter = OnePixelSplitter(true)
        splitter.proportion = 0.8f
        splitter.firstComponent = topPanel
        splitter.secondComponent = logPanel

        add(splitter, BorderLayout.CENTER)

        prevButton.addActionListener { controller.previousFeature() }
        nextButton.addActionListener { controller.nextFeature() }
        runButton.addActionListener { controller.runNextFeature() }
        editBacklogButton.addActionListener { controller.createOrUpdateBacklog() }
    }

    // Listener implementation
    override fun onStateChanged(state: OrchestratorState) {
        val canRun = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI)
        runButton.isEnabled = canRun && lastStatus == BacklogStatus.OK
    }

    override fun onLog(message: String) {
        logArea.append(message + "\n")
        logArea.caretPosition = logArea.document.length
    }

    override fun onClearLog() {
        logArea.text = ""
    }

    override fun onClearPrompt() {
        // No-op
    }

    override fun onBacklogStatusChanged(status: BacklogStatus) {
        lastStatus = status
        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = controller.getAvailableSkills()
            ApplicationManager.getApplication().invokeLater {
                refreshSkills(skills)
            }
        }
        when (status) {
            BacklogStatus.MISSING -> {
                featureDesc.text = "No BACKLOG.md found in project root. Press Create Backlog to generate a template."
                runButton.isVisible = true
                runButton.isEnabled = false
                editBacklogButton.isVisible = false // Hide the bottom one
                prevButton.isEnabled = false
                nextButton.isEnabled = false

                addFeatureButton.isEnabled = false
                editFeatureButton.isEnabled = false
                removeFeatureButton.isEnabled = false
                completeFeatureButton.isEnabled = false

                createBacklogButton.text = "Create Backlog"
                cardLayout.show(centerNavPanel, "BUTTON")
            }
            BacklogStatus.NO_FEATURES -> {
                featureName.text = "No Features"
                featureDesc.text = "No features found in BACKLOG.md. Press Add Feature (+) to append a new feature."
                runButton.isVisible = true
                runButton.isEnabled = false
                editBacklogButton.isVisible = false
                prevButton.isEnabled = false
                nextButton.isEnabled = false

                addFeatureButton.isEnabled = true
                editFeatureButton.isEnabled = false
                removeFeatureButton.isEnabled = false
                completeFeatureButton.isEnabled = false

                cardLayout.show(centerNavPanel, "EMPTY")
            }
            BacklogStatus.OK -> {
                runButton.isVisible = true
                runButton.isEnabled = true // Will be updated by onStateChanged logic if needed, but good to reset here or rely on onStateChanged if called.
                // Actually onStateChanged logic relies on lastStatus which is updated above.
                // We should probably trigger a state update or just set it here based on current controller state if we could access it,
                // but controller state isn't exposed directly here except via listener.
                // However, usually validateBacklog is called which triggers this.
                // Let's just set it to true here, and if state is not IDLE/etc it might be wrong until next state change?
                // Better to re-evaluate enablement based on controller state if possible, or just let it be true as default for OK.
                // But wait, if we are in VERIFYING state, runButton should be disabled.
                // We don't have access to controller.state directly (it's private set in controller, but we can access it via getter if public).
                // Controller has `var state: OrchestratorState` which is public get.

                val state = controller.state
                val canRun = (state == OrchestratorState.IDLE || state == OrchestratorState.FAILED || state == OrchestratorState.COMPLETED || state == OrchestratorState.AWAITING_AI)
                runButton.isEnabled = canRun

                editBacklogButton.isVisible = false

                addFeatureButton.isEnabled = true
                // edit and remove enablement depends on feature selection, handled in onFeaturePreview

                cardLayout.show(centerNavPanel, "LABEL")
            }
        }
    }

    override fun onNavigationStateChanged(hasPrevious: Boolean, hasNext: Boolean) {
        prevButton.isEnabled = hasPrevious
        nextButton.isEnabled = hasNext
    }

    override fun onSkillsUpdated() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val skills = controller.getAvailableSkills()
            ApplicationManager.getApplication().invokeLater {
                refreshSkills(skills)
            }
        }
    }

    override fun onFeatureSelected(feature: BacklogFeature?) {
        if (lastStatus != BacklogStatus.OK) return
        featureName.text = feature?.let { "${it.name}" } ?: "No feature selected"
        featureDesc.text = feature?.description?.let { truncate(it) } ?: ""

        editFeatureButton.isEnabled = feature != null
        removeFeatureButton.isEnabled = feature != null
        completeFeatureButton.isEnabled = feature != null
    }

    override fun onPromptGenerated(prompt: String) {

    }

    override fun onCompletion(success: Boolean) {

    }

    private fun truncate(text: String, max: Int = 600): String = if (text.length <= max) text else text.substring(0, max) + "…"

    private fun refreshSkills(skills: List<Skill> = emptyList()) {
        skillsPanel.removeAll()
        if (skills.isEmpty()) {
            val emptyPanel = JPanel(BorderLayout())
            emptyPanel.add(JBLabel("No skills found in .aiassistant/skills/"), BorderLayout.CENTER)
            val downloadBtn = JButton("Download Agent Skills").apply {
                addActionListener { controller.downloadSkills() }
            }
            emptyPanel.add(downloadBtn, BorderLayout.SOUTH)
            skillsPanel.add(emptyPanel, BorderLayout.NORTH)
        } else {
            val headerPanel = JPanel(BorderLayout())
            headerPanel.add(JBLabel("Available Skills:"), BorderLayout.WEST)
            val refreshBtn = JButton("Refresh/Update").apply {
                preferredSize = Dimension(120, 20)
                addActionListener { controller.downloadSkills() }
            }
            headerPanel.add(refreshBtn, BorderLayout.EAST)
            skillsPanel.add(headerPanel, BorderLayout.NORTH)

            val listPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            skills.forEach { skill ->
                val nameLabel = JBLabel(skill.name).apply {
                    toolTipText = skill.description
                    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                    addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            controller.openSkillFile(skill)
                        }
                    })
                }

                val checkbox = JBCheckBox("", controller.isSkillSelected(skill)).apply {
                    addActionListener {
                        controller.toggleSkill(skill)
                    }
                }
                
                val itemPanel = JPanel(BorderLayout())
                itemPanel.add(checkbox, BorderLayout.WEST)
                itemPanel.add(nameLabel, BorderLayout.CENTER)
                
                listPanel.add(itemPanel)
            }
            skillsPanel.add(JBScrollPane(listPanel), BorderLayout.CENTER)
        }
        skillsPanel.revalidate()
        skillsPanel.repaint()
    }
}

private class WrappingPanel(layout: FlowLayout) : JBPanel<WrappingPanel>(layout) {
    override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        val parent = parent
        if (parent != null) {
            val width = parent.width
            if (width > 0) {
                // Calculate height required for this width
                val layout = layout as FlowLayout
                var rowHeight = 0
                var totalHeight = layout.vgap
                var rowWidth = layout.hgap

                for (i in 0 until componentCount) {
                    val comp = getComponent(i)
                    if (comp.isVisible) {
                        val dComp = comp.preferredSize
                        if (rowWidth + dComp.width > width) {
                            totalHeight += rowHeight + layout.vgap
                            rowWidth = layout.hgap
                            rowHeight = 0
                        }
                        rowWidth += dComp.width + layout.hgap
                        rowHeight = maxOf(rowHeight, dComp.height)
                    }
                }
                totalHeight += rowHeight + layout.vgap
                d.height = totalHeight
                d.width = width // Constrain width to parent
            }
        }
        return d
    }
}
