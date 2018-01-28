
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    testCompile(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
