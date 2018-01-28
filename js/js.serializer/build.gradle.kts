
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:serialization"))
    compile(project(":js:js.ast"))
    compile(ideaSdkCoreDeps("intellij-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

