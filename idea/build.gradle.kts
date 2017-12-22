

import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-compiler-runner"))
    compile(project(":compiler:plugin-api"))
    compile(project(":idea:formatter"))
    compile(project(":idea:idea-core"))
    compile(project(":kotlin-script-util")) { isTransitive = false }

    compile(preloadedDeps("markdown"))

    if (!isClionBuild()) {
        compile(project(":idea:kotlin-gradle-tooling"))
        compile(project(":eval4j"))
        compile(project(":plugins:uast-kotlin"))
        compile(project(":plugins:uast-kotlin-idea"))

        compileOnly(ideaSdkDeps("velocity", "boot", "gson", "swingx-core", "jsr305", "forms_rt"))

        testCompile(project(":kotlin-test:kotlin-test-junit"))
        testCompile(projectTests(":compiler:tests-common"))
        testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
        testCompile(project(":idea:idea-jvm")) { isTransitive = false }

        testCompile(ideaPluginDeps("idea-junit", plugin = "junit"))
        testCompile(ideaPluginDeps("testng", "testng-plugin", plugin = "testng"))
        testCompile(ideaPluginDeps("coverage", plugin = "coverage"))
        testCompile(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))

        testCompile(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
        testCompile(ideaPluginDeps("copyright", plugin = "copyright"))
        testCompile(ideaPluginDeps("properties", plugin = "properties"))
        testCompile(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
        testCompile(project(":idea:idea-gradle")) { isTransitive = false }
        testCompile(project(":idea:idea-maven")) { isTransitive = false }
        testCompile(commonDep("junit:junit"))

        testCompileOnly(ideaPluginDeps("gradle-base-services", "gradle-tooling-extension-impl", "gradle-wrapper", plugin = "gradle"))
        testCompileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))
        testCompileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))

        testCompileOnly(ideaSdkDeps("groovy-all", "velocity", "gson", "jsr305", "idea_rt"))

        testRuntime(ideaSdkDeps("*.jar"))

        testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
        testRuntime(ideaPluginDeps("jcommander", "resources_en", plugin = "testng"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
        testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))

        testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
        testRuntime(projectDist(":kotlin-reflect"))
        testRuntime(projectDist(":kotlin-preloader"))

        // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
        testCompile(project(":plugins:android-extensions-compiler"))
        testCompile(project(":plugins:android-extensions-ide")) { isTransitive = false }
        testCompile(project(":allopen-ide-plugin")) { isTransitive = false }
        testCompile(project(":kotlin-allopen-compiler-plugin"))
        testCompile(project(":noarg-ide-plugin")) { isTransitive = false }
        testCompile(project(":kotlin-noarg-compiler-plugin"))
        testCompile(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
        testCompile(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
        testCompile(project(":kotlin-sam-with-receiver-compiler-plugin"))
        testCompile(project(":idea:idea-android")) { isTransitive = false }
        testCompile(project(":plugins:lint")) { isTransitive = false }
        testCompile(project(":plugins:uast-kotlin"))

        (rootProject.extra["compilerModules"] as Array<String>).forEach {
            testCompile(project(it))
        }
    }
}


sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("idea-completion/src",
                     "idea-live-templates/src")
    }
    "test" {
        projectDefault()
        java.srcDirs(
                "idea-completion/tests",
                "idea-live-templates/tests")
    }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}

testsJar {}

classesDirsArtifact()
configureInstrumentation()

