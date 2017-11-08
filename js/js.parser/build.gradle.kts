
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(commonDep("org.json", "json"))
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

