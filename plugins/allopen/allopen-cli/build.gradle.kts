
description = "Kotlin AllOpen Compiler Plugin"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))

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

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

// Do not rename, used in JPS importer
val ideaPluginArtifactName by extra(project.name.replace("^kotlin-".toRegex(), ""))

ideaPlugin {
    from(jar)
    rename { ideaPluginArtifactName }
}

projectTest {
    workingDir = rootDir
}
