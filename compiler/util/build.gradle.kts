
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:deserialization"))
    if (!isClionBuild()) {
        compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    } else {
        compile(clionSdkDeps("openapi", "util", "annotations", "log4j", "trove4j", "guava"))
    }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources")).apply { include("**") }
    }
    "test" {}
}

