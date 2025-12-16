plugins {
    `java-library`
    antlr
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // OpenRewrite core APIs and testing support.
    implementation(platform("org.openrewrite:rewrite-bom:8.68.1"))
    implementation("org.openrewrite:rewrite-core")
    testImplementation(platform("org.openrewrite:rewrite-bom:8.68.1"))
    testImplementation("org.openrewrite:rewrite-test")

    // ANTLR for DRL parsing.
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages")
}

tasks.withType<JavaCompile>().configureEach {
    // Ensure generated sources are available to compilation.
    source(tasks.generateGrammarSource)
    options.encoding = "UTF-8"
}

sourceSets {
    main {
        java.srcDir("$buildDir/generated-src/antlr/main")
    }
}
