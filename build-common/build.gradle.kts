
description = "Kotlin Build Common"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(ideaSdkDeps("util"))
    testCompile(project(":compiler:cli-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(protobufFull())
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(ideaSdkDeps("openapi"))
    testRuntime(projectDist(":kotlin-compiler"))
    testRuntime(projectDist(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest()

publish()
