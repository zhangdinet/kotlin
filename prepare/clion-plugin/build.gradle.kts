/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin CLion plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

plugins {
    `java-base`
}

val projectsToShadow = listOf(
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":idea:formatter",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-core",
        ":idea:idea-jps-common",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":j2k",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.serializer",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":core:util.runtime")

val packedJars by configurations.creating
val sideJars by configurations.creating

dependencies {
    packedJars(preloadedDeps("protobuf-${rootProject.extra["versions.protobuf-java"]}"))
    packedJars(project(":kotlin-stdlib", configuration = "builtins"))
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(projectDist(":kotlin-stdlib"))
    sideJars(projectDist(":kotlin-reflect"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(preloadedDeps("markdown", "kotlinx-coroutines-core", "kotlinx-coroutines-jdk8", "java-api", "java-impl"))
    sideJars(ideaSdkDeps("asm-all"))
}

val ideaProjectResources =  getSourceSetsFrom(":idea")["main"].output.resourcesDir
val preparedResources = File(buildDir, "prepResources")

val clionPluginXmlContent: String by lazy {
    val sectRex = Regex("""^\s*</?idea-plugin>\s*$""")
    File(ideaProjectResources, "META-INF/clion.xml")
            .readLines()
            .filterNot { it.matches(sectRex) }
            .joinToString("\n")
}

val prepareResources by task<Copy> {
    from(ideaProjectResources, {
        exclude("META-INF/plugin.xml")
    })
    into(preparedResources)
}

val preparePluginXml by task<Copy> {
    var isInsideClionPlaceholder = false
    val start = "<!-- CLION-PLUGIN-PLACEHOLDER-START -->"
    val end = "<!-- CLION-PLUGIN-PLACEHOLDER-END -->"
    from(ideaProjectResources, { include("META-INF/plugin.xml") })
    into(preparedResources)
    filter {
        if (it.contains(start)) {
            isInsideClionPlaceholder = true
            clionPluginXmlContent
        } else if (it.contains(end)) {
            isInsideClionPlaceholder = false
            ""
        } else if (isInsideClionPlaceholder) {
            ""
        } else it
    }
}

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    dependsOn(preparePluginXml)
    dependsOn(prepareResources)

    from(preparedResources)
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(packedJars)
    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output.minus(getSourceSetsFrom(":idea")["main"].resources))
    }
    archiveName = "kotlin-plugin.jar"
}

clionPlugin {
    shouldRunAfter(":dist")
    from(jar)
    from(sideJars)
}

