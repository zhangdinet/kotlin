
description = "Annotation Processor for Kotlin"

apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

afterEvaluate {
    dependencies {
        testCompile(intellij { include("idea.jar", "idea_rt.jar", "openapi.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
}

runtimeJar()
sourcesJar()
javadocJar()

dist()

publish()
