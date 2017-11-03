
apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("android")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":plugins:android-extensions-compiler"))
}

afterEvaluate {
    dependencies {
        compile(intellijPlugin("android") { include("**/android-jps-plugin.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

