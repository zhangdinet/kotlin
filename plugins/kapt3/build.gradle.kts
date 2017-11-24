
description = "Annotation Processor for Kotlin"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

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
        compileOnly(intellijCoreJar())
        compileOnly(intellij { include("asm-all.jar") })
        testCompile(intellijCoreJar())
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
