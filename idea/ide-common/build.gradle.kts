
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.serializer"))

    if (!isClionBuild()) {
        compile(ideaSdkCoreDeps("annotations", "guava", "intellij-core"))
    } else {
        compile(clionSdkDeps("annotations", "guava", "clion"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

