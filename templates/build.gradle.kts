
apply(from = "../gradle/kotlin.gradle")
apply(from = "../gradle/publish.gradle")
apply(from = "../gradle/dokka.gradle")

// IMPORTANT: Required for compiling classes in test dependencies. It *MUST* be before dependencies
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.dependsOn(tasks.getByPath(":core:compileTestKotlin"))
val coreTest: SourceSetOutput = project(":core").sourceSet("test").output

extra["basePackage"] = "com.hexagonkt.templates"

dependencies {
    "api"(project(":core"))

    "testImplementation"(project(":serialization_yaml"))
    "testImplementation"(coreTest)
}

extensions.configure<PublishingExtension> {
    (publications["mavenJava"] as MavenPublication).artifact(tasks.named("testJar"))
}
