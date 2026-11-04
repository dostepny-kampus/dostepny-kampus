#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/straznik-jakosci.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
repo="$workdir/repo"
failures=0
clean='Brak wyciszeń analizy, plików baseline i zmian reguł jakości.'
config='konfiguracji reguł jakości'
changed='zmieniony plik reguł jakości'
removed='usunięty plik reguł jakości'
silenced='wyciszenie analizy'
unmarked='usunięte @NullMarked'

main_dir=backend/src/main/java/kampus/building
test_dir=backend/src/test/java/kampus
android_test=android/app/src/test/kotlin/kampus/app

fixture() {
  mkdir -p backend/.mvn "$main_dir" "$test_dir/building" "$android_test"
  cat >backend/pom.xml <<'XML'
<project>
  <properties>
    <java.version>25</java.version>
    <errorprone.version>2.42.0</errorprone.version>
    <nullaway.version>0.12.10</nullaway.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <compilerArgs>
            <arg>-XDcompilePolicy=simple</arg>
            <arg>--should-stop=ifError=FLOW</arg>
            <arg>-Xplugin:ErrorProne -XepDisableAllChecks -Xep:NullAway:ERROR -XepOpt:NullAway:OnlyNullMarked=true</arg>
          </compilerArgs>
          <annotationProcessorPaths>
            <path>
              <groupId>com.uber.nullaway</groupId>
              <artifactId>nullaway</artifactId>
              <version>${nullaway.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
XML
  printf '%s\n' '--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED' \
    '--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED' >backend/.mvn/jvm.config
  cat >"$main_dir/package-info.java" <<'JAVA'
/**
 * Budynki kampusu.
 */
@NullMarked
package kampus.building;

import org.jspecify.annotations.NullMarked;
JAVA
  cat >"$main_dir/Building.java" <<'JAVA'
package kampus.building;

public final class Building {
    private final String name;

    public Building(final String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }
}
JAVA
  cat >"$test_dir/ArchitectureTest.java" <<'JAVA'
package kampus;

final class ArchitectureTest {
    @ArchTest
    static final ArchRule DOMAIN = noClasses().should().dependOnClassesThat().resideInAPackage("..web..");

    @ArchTest
    static final ArchRule CYCLES = slices().matching("kampus.(*)..").should().beFreeOfCycles();
}
JAVA
  printf 'package kampus;\n\nfinal class ModularityTest {\n    @Test\n    void verifiesModules() {\n%s\n    }\n}\n' \
    '        ApplicationModules.of(Application.class).verify();' >"$test_dir/ModularityTest.java"
  printf 'package kampus.building;\n\nfinal class BuildingTest {\n    @Test\n    void keepsName() {\n%s\n    }\n}\n' \
    '        assertEquals("C-13", new Building("C-13").name());' >"$test_dir/building/BuildingTest.java"
  printf 'package kampus.app\n\nclass ArchitectureTest {\n    @Test\n    fun viewModelsLiveInUi() {\n%s\n    }\n}\n' \
    '        Konsist.scopeFromProject().classes().assertTrue { it.resideInPackage("..ui..") }' \
    >"$android_test/ArchitectureTest.kt"
  printf 'package kampus.app\n\nclass MapViewModelTest {\n    @Test\n    fun startsEmpty() {\n%s\n    }\n}\n' \
    '        assertEquals(emptyList(), MapViewModel().buildings.value)' >"$android_test/MapViewModelTest.kt"
}

run_case() {
  local name="$1" expected="$2" change="$3" prepare="${4:-}" status=0 out
  rm -rf "$repo"
  mkdir -p "$repo"
  (
    cd "$repo"
    git init --quiet
    git config user.name Test
    git config user.email test@example.com
    git config core.autocrlf false
    fixture
    if [[ -n $prepare ]]; then "$prepare"; fi
    git add -A
    git commit --quiet -m baza
    "$change"
    git add -A
    git commit --quiet --allow-empty -m zmiana
  )
  out="$(cd "$repo" && PR_BODY='' PR_AUTHOR='' bash "$script" HEAD~1 HEAD 2>&1)" || status=$?
  if [[ -z $expected && $status -eq 0 && $out == *"$clean"* ]] ||
    [[ -n $expected && $status -eq 1 && $out == *"$expected"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected:-braku zgłoszeń}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

pom_nullaway_warn() { sed -i 's/-Xep:NullAway:ERROR/-Xep:NullAway:WARN/' backend/pom.xml; }
pom_drop_compiler_arg() { sed -i '/--should-stop=ifError=FLOW/d' backend/pom.xml; }
pom_errorprone_version() { sed -i 's/2\.42\.0/2.41.0/' backend/pom.xml; }
pom_insert_after() { sed -i "s#$1#&\n$2#" backend/pom.xml; }
pom_dependency() {
  pom_insert_after '^  <dependencies>$' "    <dependency><groupId>$1</groupId><artifactId>$2</artifactId></dependency>"
}
pom_add_jspecify() { pom_dependency org.jspecify jspecify; }
pom_add_dependency() { pom_dependency org.postgresql postgresql; }
pom_java_version() { sed -i 's#<java.version>25<#<java.version>26<#' backend/pom.xml; }
pom_boot_layers() { pom_insert_after '<artifactId>spring-boot-maven-plugin</artifactId>$' '<configuration/>'; }
pom_excluded_paths() { pom_insert_after 'compilePolicy=simple</arg>$' '<arg>-XepExcludedPaths:.*/web/.*</arg>'; }
pom_disable_all() { pom_insert_after '<java.version>25</java.version>$' '<ep.flags>-XepDisableAllChecks</ep.flags>'; }
pom_nullaway_off() { sed -i 's/-Xep:NullAway:ERROR/-Xep:NullAway:OFF/' backend/pom.xml; }

jvm_config_delete() { git rm --quiet backend/.mvn/jvm.config; }
jvm_config_edit() { sed -i '/--add-opens/d' backend/.mvn/jvm.config; }
jvm_config_absent() { rm backend/.mvn/jvm.config; }
jvm_config_add() { echo '--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED' >backend/.mvn/jvm.config; }

architecture_drop_rule() { sed -i '6,8d' "$test_dir/ArchitectureTest.java"; }
modularity_delete() { git rm --quiet "$test_dir/ModularityTest.java"; }
architecture_rename() { git mv "$test_dir/ArchitectureTest.java" "$test_dir/Layers.java"; }
konsist_edit() { sed -i 's/"..ui.."/"..ui..", "..map.."/' "$android_test/ArchitectureTest.kt"; }
architecture_new_backend() {
  printf 'package kampus.building;\n\nfinal class BuildingArchitectureTest {\n}\n' \
    >"$test_dir/building/BuildingArchitectureTest.java"
}
architecture_new_android() {
  mkdir -p android/core/data/src/test/kotlin/kampus/data
  printf 'package kampus.data\n\nclass DataArchitectureTest\n' \
    >android/core/data/src/test/kotlin/kampus/data/DataArchitectureTest.kt
}
architecture_support() {
  printf 'package kampus;\n\nfinal class ArchitectureTestSupport {\n}\n' >"$test_dir/ArchitectureTestSupport.java"
}
test_edit() { sed -i 's/C-13/D-20/g' "$test_dir/building/BuildingTest.java"; }
android_test_edit() { sed -i 's/startsEmpty/startsWithoutBuildings/' "$android_test/MapViewModelTest.kt"; }

code_suppress_nullaway() {
  sed -i 's/^public final class Building {$/@SuppressWarnings("NullAway")\n&/' "$main_dir/Building.java"
}
code_cast_to_non_null() { sed -i 's/this.name = name;/this.name = castToNonNull(name);/' "$main_dir/Building.java"; }
code_null_unmarked() { sed -i 's/^public final class Building {$/@NullUnmarked\n&/' "$main_dir/Building.java"; }
code_require_non_null() {
  sed -i 's/this.name = name;/this.name = Objects.requireNonNull(name);/' "$main_dir/Building.java"
}
code_rename_method() { sed -i 's/name()/label()/' "$main_dir/Building.java"; }

package_drop_marked() { sed -i '/^@NullMarked$/d' "$main_dir/package-info.java"; }
package_delete() { git rm --quiet "$main_dir/package-info.java"; }
package_unmark() { sed -i 's/^@NullMarked$/@NullUnmarked/' "$main_dir/package-info.java"; }
package_javadoc() { sed -i 's/Budynki kampusu./Budynki kampusu i ich wejścia./' "$main_dir/package-info.java"; }
package_qualified() {
  sed -i 's/^@NullMarked$/@org.jspecify.annotations.NullMarked/; /^import/d' "$main_dir/package-info.java"
}
package_new() {
  mkdir -p backend/src/main/java/kampus/review
  printf '@NullMarked\npackage kampus.review;\n\nimport org.jspecify.annotations.NullMarked;\n' \
    >backend/src/main/java/kampus/review/package-info.java
}
package_unmarked_base() {
  printf '/** Testy budynków. */\npackage kampus.building;\n' >"$test_dir/building/package-info.java"
}
package_unmarked_delete() { git rm --quiet "$test_dir/building/package-info.java"; }

run_case "S1, O1: NullAway z ERROR na WARN" "$config" pom_nullaway_warn
run_case "S1: usunięta opcja w bloku maven-compiler-plugin" "usunięta linia" pom_drop_compiler_arg
run_case "S1: zmieniona wersja errorprone poza blokiem" "$config" pom_errorprone_version
run_case "S1: dodana zależność jspecify" "$config" pom_add_jspecify
run_case "S1, S6, O5: zwykła zależność w pom.xml" "" pom_add_dependency
run_case "S1, S6, O5: zwykła właściwość w pom.xml" "" pom_java_version
run_case "S1, S6, O5: konfiguracja innej wtyczki niż kompilator" "" pom_boot_layers

run_case "S2, O2: usunięty jvm.config" "$removed" jvm_config_delete
run_case "S2: edytowany jvm.config" "$changed" jvm_config_edit
run_case "S2: dodany jvm.config" "$changed" jvm_config_add jvm_config_absent

run_case "S3, O3: usunięta reguła z ArchitectureTest.java" "$changed" architecture_drop_rule
run_case "S3: usunięty ModularityTest.java" "$removed" modularity_delete
run_case "S3: przeniesiony ArchitectureTest.java" "$changed" architecture_rename
run_case "S3: zmieniony ArchitectureTest.kt w Androidzie" "$changed" konsist_edit
run_case "S3: nowy plik reguł w backendzie" "$changed" architecture_new_backend
run_case "S3: nowy plik reguł w module Androida" "$changed" architecture_new_android
run_case "S3, S6: klasa pomocnicza o podobnej nazwie" "" architecture_support
run_case "S3, S6: zwykły test backendu" "" test_edit
run_case "S3, S6: zwykły test Androida" "" android_test_edit

run_case "S4, O4: @SuppressWarnings(\"NullAway\")" "$silenced" code_suppress_nullaway
run_case "S4: castToNonNull" "$silenced" code_cast_to_non_null
run_case "S4: @NullUnmarked na klasie" "$silenced" code_null_unmarked
run_case "S4: -XepExcludedPaths w kompilatorze" "$silenced" pom_excluded_paths
run_case "S4: -XepDisableAllChecks w nowej linii" "$silenced" pom_disable_all
run_case "S4: NullAway:OFF" "$silenced" pom_nullaway_off
run_case "S4, S6: Objects.requireNonNull" "" code_require_non_null

run_case "S5, O6: usunięte @NullMarked" "$unmarked" package_drop_marked
run_case "S5: usunięty package-info.java z @NullMarked" "$unmarked" package_delete
run_case "S5: @NullMarked zamienione na @NullUnmarked" "$unmarked" package_unmark
run_case "S5, S6: zmieniony opis pakietu" "" package_javadoc
run_case "S5, S6: pełna nazwa adnotacji @NullMarked" "" package_qualified
run_case "S5, S6: nowy pakiet z @NullMarked" "" package_new
run_case "S5, S6: usunięty package-info.java bez @NullMarked" "" package_unmarked_delete package_unmarked_base

run_case "S6: zwykła zmiana kodu src/main" "" code_rename_method

exit "$((failures > 0 ? 1 : 0))"
