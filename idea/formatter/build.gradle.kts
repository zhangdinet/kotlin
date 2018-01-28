
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkDeps("openapi"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

