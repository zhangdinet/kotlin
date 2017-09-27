
description = "Sample Kotlin JSR 223 scripting jar with local (in-process) compilation and evaluation"

apply { plugin("kotlin") }

val scriptCP by configurations.creating

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(project(":kotlin-script-util"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-reflect"))
    scriptCP(projectDist(":kotlin-stdlib"))
    scriptCP(projectDist(":kotlin-script-runtime"))
    scriptCP(projectRuntimeJar(":kotlin-script-util"))
}

projectTest()

projectTest("test9") {
    val jdkPath = project.property("JDK_9")
    onlyIf { jdkPath != null }
    executable = "$jdkPath/bin/java"
    doFirst {
        systemProperty("kotlin.script.classpath", scriptCP.asFileTree.asPath)
    }
}
