
description = "Kotlin SamWithReceiver Compiler Plugin"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
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
sourcesJar()
javadocJar()

publish()

dist {
    rename("kotlin-", "")
}

ideaPlugin {
    from(jar)
    rename("^kotlin-", "")
}

