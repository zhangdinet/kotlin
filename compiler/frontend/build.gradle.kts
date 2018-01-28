
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:util"))
    compile(project(":compiler:container"))
    compile(project(":compiler:resolution"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(commonDep("io.javaslang","javaslang"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
