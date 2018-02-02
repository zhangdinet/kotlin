import org.jetbrains.kotlin.gradle.dsl.Coroutines

apply {
    plugin("java")
    plugin("kotlin")
}

kotlin { // configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension>
    experimental.coroutines = Coroutines.ENABLE
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compileOnly(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

if (project.hasProperty("teamcity"))
tasks["compileJava"].dependsOn(":prepare:build.version:writeCompilerVersion")