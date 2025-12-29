plugins {
  alias(libs.plugins.kotlinMultiplatform).apply(false)
}

allprojects {
  group = "org.jraf.blessr"
  version = "1.0.0"
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew jvmDistZip` to create a ZIP file containing the CLI application (results in `cliApp/build/distributions/cliApp-<version>-jvm.zip`)
// `./gradlew installJvmDist` to install the CLI application (results in `cliApp/build/install/cliApp-jvm/bin/cliApp`)
