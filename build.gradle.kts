import dev.clojurephant.plugin.clojure.tasks.ClojureCompile
import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
  id("dev.clojurephant.clojure")

  id("org.ajoberstar.reckon")
  id("com.diffplug.spotless")

  `java-gradle-plugin`
  id("com.gradle.plugin-publish")
  id("org.ajoberstar.stutter")
}

group = "dev.clojurephant"

reckon {
  setDefaultInferredScope("patch")
  stages("alpha", "beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  // edn support
  implementation("us.bpsm:edn-java:0.7.1")

  // util
  implementation("org.apache.commons:commons-text:1.9")

  // compat testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.clojure:clojure:1.11.1")
  compatTestImplementation("org.clojure:tools.namespace:1.3.0")
  compatTestImplementation("nrepl:nrepl:0.9.0")
  compatTestImplementation("org.ajoberstar:ike.cljj:0.4.1")
  compatTestImplementation("org.clojure:data.xml:0.0.8")
  compatTestRuntimeOnly("org.ajoberstar:jovial:0.3.0")
}

stutter {
  val java8 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(8))
    }
    gradleVersions {
      compatibleRange("6.4")
    }
  }
  val java11 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    gradleVersions {
      compatibleRange("6.4")
    }
  }
  val java17 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
    gradleVersions {
      compatibleRange("7.3")
    }
  }
}

plugins.withId("eclipse") {
  val eclipse = extensions.getByType(EclipseModel::class)
  eclipse.classpath.plusConfigurations.add(configurations["compatTestCompileClasspath"])
}

tasks.withType<Test>() {
  useJUnitPlatform()
}

tasks.withType<Test>() {
  inputs.dir("src/compatTest/projects")
  systemProperty("stutter.projects", "src/compatTest/projects")
  systemProperty("org.gradle.testkit.dir", file("build/stutter-test-kit").absolutePath)
}

java {
  withSourcesJar()
}

publishing {
  repositories {
    maven {
      name = "clojars"
      url = uri("https://repo.clojars.org")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_TOKEN")
      }
    }
  }

  publications.withType<MavenPublication>() {
    // use static versions in poms
    versionMapping {
      usage("java-api") {
        fromResolutionOf("runtimeClasspath")
      }
      usage("java-runtime") {
        fromResolutionResult()
      }
    }

    pom {
      // include repository info in POM (needed for cljdoc)
      scm {
        connection.set("https://github.com/clojurephant/clojurephant.git")
        developerConnection.set("git@github.com:clojurephant/clojurephant.git")
        url.set("https://github.com/clojurephant/clojurephant")
        if (!version.toString().contains("+")) {
          tag.set(version.toString())
        }
      }
    }
  }
}

// Clojars doesn"t support module metadata
tasks.withType<GenerateModuleMetadata>() {
    enabled = false
}

gradlePlugin {
  plugins {
    create("clojureBase") {
      id = "dev.clojurephant.clojure-base"
      displayName = "Clojure base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojureBasePlugin"
    }
    create("clojure") {
      id = "dev.clojurephant.clojure"
      displayName = "Clojure language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojure.ClojurePlugin"
    }
    create("clojurescriptBase") {
      id = "dev.clojurephant.clojurescript-base"
      displayName = "ClojureScript base language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin"
    }
    create("clojurescript") {
      id = "dev.clojurephant.clojurescript"
      displayName = "ClojureScript language plugin for Gradle"
      implementationClass = "dev.clojurephant.plugin.clojurescript.ClojureScriptPlugin"
    }
  }
}

pluginBundle {
  website = "https://clojurephant.dev/"
  vcsUrl = "https://github.com/clojurephant/clojurephant.git"
  description = "Clojure and ClojureScript language support for Gradle"
  tags = listOf("clojure", "clojurescript", "language")
}

spotless {
  java {
    importOrder("java", "javax", "")
    eclipse().configFile(rootProject.file("gradle/eclipse-java-formatter.xml"))
  }
}

// accept build scan terms
extensions.findByName("buildScan")?.withGroovyBuilder {
  setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
  setProperty("termsOfServiceAgree", "yes")
}
