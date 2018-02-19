
description = "Kotlin SamWithReceiver Compiler Plugin"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}
sourcesJar()
javadocJar()
testsJar {}

publish()

dist {
    rename("kotlin-", "")
}

// Do not rename, used in JPS importer
val ideaPluginArtifactName by extra(project.name.replace("^kotlin-".toRegex(), ""))

ideaPlugin {
    from(jar)
    rename { ideaPluginArtifactName }
}

projectTest {
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }
}
