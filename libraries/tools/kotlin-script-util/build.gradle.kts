
description = "Kotlin scripting support utilities"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:0.10.1") { isTransitive = false }
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompile(project(":compiler:cli"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":kotlin-reflect"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectRuntimeJar(":kotlin-compiler"))
    testCompile("com.jcabi:jcabi-aether:0.10.1") { isTransitive = false }
    testCompile("org.sonatype.aether:aether-api:1.13.1")
    testCompile("org.apache.maven:maven-core:3.0.3")
}

projectTest()

runtimeJar()
sourcesJar()
javadocJar()

publish()

ideaPlugin()
