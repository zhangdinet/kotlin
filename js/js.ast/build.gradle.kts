
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkCoreDeps("trove4j", "intellij-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

