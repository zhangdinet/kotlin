package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class JpsCompatiblePlugin : Plugin<Project> {
    override fun apply(project: Project) {}
}

class JpsCompatibleRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("pill") {
            this.doLast {
                val jpsProject = KotlinSpecific.postProcess(parse(project))
                val files = render(jpsProject, KotlinSpecific.getProjectLibraries(jpsProject))

                File(project.rootProject.projectDir, ".idea/libraries").deleteRecursively()

                for (file in files) {
                    val stubFile = file.path
                    stubFile.parentFile.mkdirs()
                    stubFile.writeText(file.text)
                }
            }
        }
    }


}