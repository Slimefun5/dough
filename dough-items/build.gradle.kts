plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":dough-common"))
    api(project(":dough-reflection"))
}
