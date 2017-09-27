
description = "Sample Kotlin JSR 223 scripting jar with daemon (out-of-process) compilation and local (in-process) evaluation"

apply { plugin("kotlin") }

val compilerCP by configurations.creating

dependencies {
    testCompile(project(":kotlin-stdlib"))
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-script-util"))
    testCompile(project(":kotlin-daemon-client"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    compilerCP(projectRuntimeJar(":kotlin-compiler"))
    compilerCP(projectDist(":kotlin-reflect"))
    compilerCP(projectDist(":kotlin-stdlib"))
    compilerCP(projectDist(":kotlin-script-runtime"))
}

projectTest {
    doFirst {
        systemProperty("kotlin.compiler.classpath", compilerCP.asFileTree.asPath)
    }
}
