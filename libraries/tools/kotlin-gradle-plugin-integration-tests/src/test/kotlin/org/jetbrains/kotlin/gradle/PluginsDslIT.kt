package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.io.File

object MavenLocalUrlProvider : BaseGradleIT() {
    val mavenLocalUrl by lazy {
        val buildScript = """
        repositories {
            println "mavenLocalUrl=" + mavenLocal().url
        }
        """.trimIndent()

        val tempBuildDir = FileUtil.createTempDirectory(
                File(resourcesRootFile.absolutePath, "testProject/pluginsDsl"),
                "MavenLocalUrlProvider",
                null)

        File(tempBuildDir, "build.gradle").writeText(buildScript)

        var result: String? = null

        val p = Project(tempBuildDir.name, "4.0", "pluginsDsl")
        p.build("tasks") {
            val urlPattern = "mavenLocalUrl=(.*)".toRegex()
            result = urlPattern.find(output)?.groups!![1]?.value
        }

        result
    }
}


class PluginsDslIT : BaseGradleIT() {

    companion object {
        protected const val GRADLE_VERSION = "4.0"
        protected const val DIRECTORY_PREFIX = "pluginsDsl"

        private const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
        private const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

        // Workaround for the restriction that snapshot versions are not supported
        private const val DEFAULT_MARKER_VERSION = KOTLIN_VERSION + "-test"
    }

    private fun projectWithMavenLocalPlugins(
            projectName: String,
            pluginMarkerVersion: String = DEFAULT_MARKER_VERSION,
            wrapperVersion: String = GRADLE_VERSION,
            directoryPrefix: String? = DIRECTORY_PREFIX,
            minLogLevel: LogLevel = LogLevel.DEBUG
    ): Project {

        val result = Project(projectName, wrapperVersion, directoryPrefix, minLogLevel)
        result.setupWorkingDir()

        val settingsGradle = File(result.projectDir, "settings.gradle")
        settingsGradle.modify {
            val mavenLocalUrl = MavenLocalUrlProvider.mavenLocalUrl
                    ?: throw AssumptionViolatedException("Could not get the Maven Local repository location.")

            it.replace(MAVEN_LOCAL_URL_PLACEHOLDER, mavenLocalUrl).apply {
                if (this == it)
                    throw AssumptionViolatedException("$MAVEN_LOCAL_URL_PLACEHOLDER placeholder not found in settings.gradle")
            }
        }

        result.projectDir.walkTopDown()
                .filter { it.isFile && it.name == "build.gradle" }
                .forEach { buildGradle ->
                    buildGradle.modify { text ->
                        text.replace(PLUGIN_MARKER_VERSION_PLACEHOLDER, pluginMarkerVersion)
                    }
                }

        return result
    }

    @Test
    fun testAllopenWithPluginsDsl() {
        val project = projectWithMavenLocalPlugins("allopenPluginsDsl")
        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
        }
    }
}