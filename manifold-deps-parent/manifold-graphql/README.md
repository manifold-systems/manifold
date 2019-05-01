# The GraphQL Manifold

[![Gitter](https://badges.gitter.im/manifold-systems/community.svg)](https://gitter.im/manifold-systems/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

The GraphQL Manifold is a [Manifold](http://manifold.systems/) extension library that allows for seamless interaction
with [GraphQL](https://graphql.org/) resources (`.graphql` schema files).

Use the GraphQL Manifold in any Java project to **type-safely** build and execute GraphQL queries and mutations without
introducing a code generation step in your build process.

## Build Queries
```java
private static String ENDPOINT = "http://example/graphql";
...
var query = MovieQuery.builder().withGenre(Action).build();
var result = query.request(ENDPOINT).post();
var actionMovies = result.getMovies();
for (var movie : actionMovies) {
  out.println(
    "Title: ${movie.getTitle()}\n" +
    "Genre: ${movie.getGenre()}\n" +
    "Year: ${movie.getReleaseDate().getYear()}\n");
}
```

## Execute Mutations
```java
// Find the movie to review ("Le Mans")
var movie = MovieQuery.builder().withTitle("Le Mans").build()
  .request(ENDPOINT).post().getMovies().first();
// Submit a review for the movie
var review = ReviewInput.builder(5).withComment("Topnotch racing film.").build();
var mutation = ReviewMutation.builder(movie.getId(), review).build();
var createdReview = mutation.request(ENDPOINT).post().getCreateReview();
out.println(
  "Review for: ${movie.getTitle()}\n" +
  "Stars: ${createdReview.getStars()}\n" +
  "Comment: ${createdReview.getComment()}\n"
);
```

# IDE Support

Use the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA to really boost your
productivity.  Use code completion to conveniently build queries and discover the schema's API.  Navigate to/from
call-sites and GraphQL schema file elements.  Make changes to your query schema files and use the changes immediately,
no compilation!  Find usages of any element in your schema files. Perform rename refactors to quickly and safely make
project-wide changes.

todo: insert screencast


# Building

The GraphQL Manifold (Manifold general) works with all build tooling, including Maven and Gradle. It
also works with Java versions 8 - 12.

Here are some sample build configurations references.

## Gradle

### Java 8
Here is a sample `build.gradle` file using `manifold-graphql` with **Java 8**:
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 1.8
sourceCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    compile group: 'systems.manifold', name: 'manifold-graphql', version: '0.XX-alpha'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // tools.jar dependency (for Java 8 only)
    compile files("${System.properties['java.home']}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold strings'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyGraphQLProject'
```

### Java 11+
Here is a sample `build.gradle` file using `manifold-all` with **Java 11**:
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    mavenCentral()
}

dependencies {
    compile group: 'systems.manifold', name: 'manifold-graphql', version: '0.XX-alpha'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold-all to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-graphql', version: '0.XX-alpha'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold strings'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyGraphQLProject'
```

## Maven

### Java 8

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-graphql-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My GraphQL App</name>

    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-graphql</artifactId>
            <version>0.XX-alpha</version>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin, enable string interpolation and checked exception suppression-->
                        <arg>-Xplugin:Manifold strings</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- Java 8 only, for tools.jar  -->
        <profile>
            <id>internal.tools-jar</id>
            <activation>
                <file>
                    <exists>\${java.home}/../lib/tools.jar</exists>
                </file>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.sun</groupId>
                    <artifactId>tools</artifactId>
                    <version>1.8.0</version>
                    <scope>system</scope>
                    <systemPath>\${java.home}/../lib/tools.jar</systemPath>
                </dependency>
              </dependencies>
        </profile>
    </profiles>
</project>

### Java 11+

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-graphql-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My GraphQL App</name>

    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-graphql</artifactId>
            <version>0.XX-alpha</version>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin, enable string interpolation and checked exception suppression-->
                        <arg>-Xplugin:Manifold strings</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
