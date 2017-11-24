
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
        compileOnly(intellij { include("jdom.jar", "util.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

