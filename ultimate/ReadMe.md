# Kotlin Ultimate

This project is a part of **Kotlin IntelliJ IDEA Plugin** 
which provides support for Ultimate IDEA features from Kotlin side.

If you want to work on this project you should open it in IDEA as separate project.

## Build a plugin

If you want to build a **Kotlin IntelliJ IDEA Plugin** locally 
with ultimate features support you should:

1. Build kotlin plugin in main project: `gradlew ideaPlugin`.
2. Run `gradlew ideaUltimatePlugin` in **kotlin-ultimate** folder.

Then you will get kotlin-plugin with ultimate features in `kotlin/dist/artifacts/KotlinUltimate` folder.
And then you can move/copy/symlink `dist/artifacts/KotlinUltimate` to the IDEA config: `config/plugins/Kotlin`.  