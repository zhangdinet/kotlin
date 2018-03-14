description = "Merge utility for Kotlin Standard Library for JS"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":js:js.ast"))
    compile(project(":js:js.parser"))
    compile(project(":js:js.translator"))

    runtime(intellijCoreDep())
}

sourceSets {
    "main" { projectDefault() }
}
