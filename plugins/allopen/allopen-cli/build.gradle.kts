
description = "Kotlin AllOpen Compiler Plugin"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(projectDist(":kotlin-stdlib"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

