
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compileOnly(project(":kotlin-reflect-api"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
