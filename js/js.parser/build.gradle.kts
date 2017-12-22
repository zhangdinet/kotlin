
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

