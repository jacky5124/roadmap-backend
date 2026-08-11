plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass = "dev.roadmap.githubactivity.MainKt"
    applicationName = "github-activity"
}
