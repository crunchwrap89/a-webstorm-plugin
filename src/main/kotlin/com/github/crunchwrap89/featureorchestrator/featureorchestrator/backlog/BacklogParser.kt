package com.github.crunchwrap89.featureorchestrator.featureorchestrator.backlog

import com.github.crunchwrap89.featureorchestrator.featureorchestrator.model.*
import com.intellij.openapi.diagnostic.Logger

object BacklogParser {
    private val log = Logger.getInstance(BacklogParser::class.java)

    private val featureHeaderRegex = Regex("""^## +Feature name\s*$""")
    private val nameRegex = Regex("""^(?:\[( |x|X)] +)?(.+)$""")
    private val sectionHeaderRegex = Regex("""^### +(.+)$""")
    private val separatorRegex = Regex("""^---+\s*$""")

    fun parse(text: String): Backlog {
        val lines = text.lines()
        val features = mutableListOf<BacklogFeature>()
        val warnings = mutableListOf<String>()

        var i = 0
        while (i < lines.size) {
            if (!featureHeaderRegex.matches(lines[i])) { i++; continue }
            val blockStart = i
            i++
            // Skip blank lines after ## Feature name
            while (i < lines.size && lines[i].isBlank()) {
                i++
            }

            var name = "Untitled Feature"
            var checked = false

            // Check if line is a name or a new section/feature/separator
            if (i < lines.size) {
                if (!featureHeaderRegex.matches(lines[i]) &&
                    !sectionHeaderRegex.matches(lines[i]) &&
                    !separatorRegex.matches(lines[i])) {

                    if (nameRegex.matches(lines[i])) {
                        val nameMatch = nameRegex.matchEntire(lines[i])!!
                        checked = nameMatch.groupValues[1].equals("x", ignoreCase = true)
                        name = nameMatch.groupValues[2].trim()
                        i++
                    } else {
                        warnings += warning(blockStart, "Invalid feature name line. Using default.")
                        i++
                    }
                }
            }

            // Skip blank lines
            while (i < lines.size && lines[i].isBlank()) {
                i++
            }

            // Expect Description header
            if (i >= lines.size || !sectionHeaderRegex.matches(lines[i]) || !lines[i].trim().equals("### Description", ignoreCase = true)) {
                warnings += warning(blockStart, "Missing '### Description' section. Skipping feature '$name'.")
                // skip till next feature
                i = skipToNextFeature(lines, i)
                continue
            }
            i++
            val descriptionBuilder = StringBuilder()
            while (i < lines.size && !featureHeaderRegex.matches(lines[i]) && !sectionHeaderRegex.matches(lines[i]) && !separatorRegex.matches(lines[i])) {
                descriptionBuilder.appendLine(lines[i])
                i++
            }
            val description = descriptionBuilder.toString().trim()
            if (description.isBlank()) {
                // Allow empty description for templates
            }

            val optionalSections = mutableMapOf<Section, String>()

            while (i < lines.size && !featureHeaderRegex.matches(lines[i]) && !separatorRegex.matches(lines[i])) {
                if (!sectionHeaderRegex.matches(lines[i])) { i++; continue }
                val secTitle = lines[i].removePrefix("### ").trim()
                i++
                val content = StringBuilder()
                while (i < lines.size && !featureHeaderRegex.matches(lines[i]) && !sectionHeaderRegex.matches(lines[i]) && !separatorRegex.matches(lines[i])) {
                    content.appendLine(lines[i])
                    i++
                }
                val body = content.toString().trim()
                when (secTitle.lowercase()) {
                    "requirements" -> optionalSections[Section.REQUIREMENTS] = body
                    "out of scope", "out-of-scope", "out_of_scope" -> optionalSections[Section.OUT_OF_SCOPE] = body
                    "notes" -> optionalSections[Section.NOTES] = body
                    "context" -> optionalSections[Section.CONTEXT] = body
                    "acceptance criteria", "acceptance-criteria", "acceptance_criteria" -> {
                        // Acceptance Criteria section is no longer used for verification, 
                        // but we can still keep it in optionalSections if we want to show it as text,
                        // but the instructions say to remove "The Preview window of Acceptance Criterias".
                        // However, keeping it as a generic section might be okay? 
                        // No, Section.ACCEPTANCE_CRITERIA was also removed from enum.
                    }
                    else -> {
                        warnings += warning(blockStart, "Unknown section '### $secTitle' in feature '$name'. Ignored.")
                    }
                }
            }

            // Compute raw block boundaries
            val blockEnd = (i - 1).coerceAtLeast(blockStart)
            val rawBlock = lines.subList(blockStart, blockEnd + 1).joinToString("\n")
            features += BacklogFeature(
                name = name,
                checked = checked,
                description = description,
                optionalSections = optionalSections.toMap(),
                rawBlock = rawBlock,
                blockStartOffset = startOffset(text, blockStart),
                blockEndOffset = endOffset(text, blockEnd),
            )
        }

        return Backlog(features, warnings)
    }

    private fun startOffset(text: String, lineIndex: Int): Int {
        if (lineIndex <= 0) return 0
        var idx = 0
        var i = 0
        while (i < lineIndex && idx < text.length) {
            val nl = text.indexOf('\n', idx)
            if (nl == -1) return text.length
            idx = nl + 1
            i++
        }
        return idx
    }

    private fun endOffset(text: String, lineIndex: Int): Int {
        if (lineIndex < 0) return 0
        var idx = 0
        var i = 0
        while (i <= lineIndex && idx < text.length) {
            val nl = text.indexOf('\n', idx)
            if (nl == -1) return text.length
            idx = nl + 1
            i++
        }
        return (idx - 1).coerceAtLeast(0)
    }

    private fun skipToNextFeature(lines: List<String>, start: Int): Int {
        var i = start
        while (i < lines.size && !featureHeaderRegex.matches(lines[i])) i++
        return i
    }

    private fun warning(location: Int, msg: String): String = "[backlog.md:${location + 1}] $msg"
}
