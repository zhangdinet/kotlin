
description = "Kotlin Gradle Tooling support"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("gradle")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:cli-common"))
}

afterEvaluate {
    dependencies {
        compile(intellijPlugin("gradle") {
            include("gradle-tooling-api-*.jar",
                    "gradle-tooling-extension-api.jar",
                    "gradle.jar",
                    "gradle-core-*.jar",
                    "gradle-base-services-groovy-*.jar")
        })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()
