import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinx.serialization)
}

kotlin {
  jvm()

  macosArm64()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kable.core)
        implementation(libs.klibnanolog)
        implementation(libs.klibfitbit)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.io)
      }
    }
  }
}

tasks.withType<KotlinCompile>().all {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}
