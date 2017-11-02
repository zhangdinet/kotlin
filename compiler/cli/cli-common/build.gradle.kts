
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellijCoreJarDependencies())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

