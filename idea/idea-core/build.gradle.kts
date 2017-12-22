apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:light-classes"))
    compile(project(":j2k"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(preloadedDeps("kotlinx-coroutines-core", "kotlinx-coroutines-jdk8"))

    if (!isClionBuild()) {
        compile(project(":plugins:android-extensions-compiler"))

        compile(ideaSdkCoreDeps("intellij-core", "util"))
        compile(ideaSdkDeps("openapi", "idea"))
        compile(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
    } else {
        compile(preloadedDeps("java-api", "java-impl"))
        compile(clionSdkDeps("openapi", "clion", "forms_rt"))
    }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../idea-analysis/src")
        resources.srcDir("../idea-analysis/src").apply { include("**/*.properties") }
    }
    "test" {}
}
