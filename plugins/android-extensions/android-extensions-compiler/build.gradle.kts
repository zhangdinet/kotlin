
description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    from(getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs)
}

dist()

ideaPlugin()
