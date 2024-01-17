buildscript {
  dependencies {
    classpath(libs.kotlin.plugin)
  }
}

plugins {
  //trick: for the same plugin versions in all sub-modules
  alias(libs.plugins.kotlinMultiplatform).apply(false)
  alias(libs.plugins.kotlinx.serialization).apply(false)
  alias(libs.plugins.kotlin.jvm).apply(false)
  alias(libs.plugins.ben.manes.versions).apply(false)
  alias(libs.plugins.maven.publish).apply(false)
  alias(libs.plugins.androidLibrary).apply(false)
  alias(libs.plugins.jetbrainsKotlinAndroid).apply(false)
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
