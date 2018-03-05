description = "Kotlin metadata manipulation library"

apply { plugin("kotlin") }

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
