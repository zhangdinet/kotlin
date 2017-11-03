
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("jps-build-test")
}

dependencies {
    compile(protobufFull())
    compile(project(":idea"))
    compile(project(":j2k"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":idea:idea-test-framework"))
    compile(projectDist(":kotlin-test:kotlin-test-jvm"))
    compile(projectTests(":kotlin-build-common"))
    compile(projectTests(":compiler"))
    compile(projectTests(":compiler:tests-java8"))
    compile(projectTests(":compiler:container"))
    compile(projectTests(":compiler:incremental-compilation-impl"))
    compile(projectTests(":idea"))
    compile(projectTests(":idea:idea-gradle"))
    compile(projectTests(":idea:idea-maven"))
    compile(projectTests(":j2k"))
    compile(projectTests(":idea:idea-android"))
    compile(projectTests(":jps-plugin"))
    compile(projectTests(":plugins:plugins-tests"))
    compile(projectTests(":plugins:android-extensions-ide"))
    compile(projectTests(":kotlin-annotation-processing"))
    compile(projectTests(":plugins:uast-kotlin"))
    compile(projectTests(":js:js.tests"))
    compile(projectTests(":generators:test-generator"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
}

afterEvaluate {
    dependencies {
        compile(intellijExtra("jps-build-test"))
        testRuntime(intellij { include("idea_rt.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
