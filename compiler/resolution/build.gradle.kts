
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":core:descriptors"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
