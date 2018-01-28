
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:deserialization"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources")).apply { include("**") }
    }
    "test" {}
}

