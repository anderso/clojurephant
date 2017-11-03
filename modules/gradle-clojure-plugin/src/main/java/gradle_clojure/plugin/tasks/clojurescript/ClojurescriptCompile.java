/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.plugin.tasks.clojurescript;

import gradle_clojure.plugin.internal.CljsEdnUtils;
import gradle_clojure.plugin.tasks.ClojureCompile;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.process.ExecResult;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static gradle_clojure.plugin.internal.PluginMetadata.GRADLE_CLOJURE_VERSION;

public class ClojurescriptCompile extends AbstractCompile {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private ClojurescriptCompileOptions options = new ClojurescriptCompileOptions();

  @Nested
  public ClojurescriptCompileOptions getOptions() {
    return options;
  }

  public ClojurescriptCompile options(Action<? super ClojurescriptCompileOptions> configureAction) {
    configureAction.execute(options);
    return this;
  }

  @Override
  @TaskAction
  protected void compile() {
    try {
      Dependency gradleClojureToolsDep = getProject().getDependencies().create("io.github.gradle-clojure:gradle-clojure-tools:" + GRADLE_CLOJURE_VERSION);
      Configuration gradleClojureToolsClasspath = getProject().getConfigurations().detachedConfiguration(gradleClojureToolsDep);

      FileCollection classpath = getClasspath().plus(gradleClojureToolsClasspath).plus(getProject().files(getSourceRoots()));

      String compilerArgs = CljsEdnUtils.compilerOptionsToEdn(getSourceRoots(), getOptions());
      Path file = Files.createTempFile(getTemporaryDir().toPath(), "clojurescript-compile-options", ".edn");
      Files.write(file, compilerArgs.getBytes(StandardCharsets.UTF_8));

      ExecResult result = getProject().javaexec(exec -> {
        exec.setJvmArgs(options.getForkOptions().getJvmArgs());
        exec.setMinHeapSize(options.getForkOptions().getMemoryInitialSize());
        exec.setMaxHeapSize(options.getForkOptions().getMemoryMaximumSize());

        exec.setMain("clojure.main");
        exec.setClasspath(classpath);
        exec.setArgs(Arrays.asList("--main", "gradle-clojure.tools.clojurescript-compiler", file.toAbsolutePath().toString()));
        exec.setDefaultCharacterEncoding("UTF-8");
      });

      result.assertNormalExitValue();

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Internal
  private Set<String> getSourceRoots() {
    return getSourceRootsFiles().stream().map(it -> {
      try {
        return it.getCanonicalPath();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }).collect(Collectors.toSet());
  }

  @Internal
  private List<File> getSourceRootsFiles() {
    // accessing the List<Object> field not the FileTree from getSource
    return source.stream()
        .filter(it -> it instanceof SourceDirectorySet)
        .flatMap(it -> ((SourceDirectorySet) it).getSrcDirs().stream())
        .collect(Collectors.toList());
  }
}
