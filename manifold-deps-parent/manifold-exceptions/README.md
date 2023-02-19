# Manifold : Exceptions

Simply add the `manifold-exceptions` dependency to your project. Now checked exceptions behave like unchecked
exceptions!  No more compiler errors, no more unintentional exception swallowing, no more boilerplate
try/catch/wrap/rethrow nonsense. This is how most modern JVM languages behave, now you have the option to make Java do
the same.

## Table of Contents
* [No More Catch-n-Wrap](#no-more-catch-n-wrap)
* [Lambdas](#lambdas)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## No More Catch-n-Wrap
The vast majority of checked exceptions go unhandled, instead they are caught, wrapped in unchecked exceptions, and
rethrown like this:
```java
URL url;
try {
  url = new URL("http://manifold.systems");
}
catch(MalformedURLException e) {
  throw new RuntimeException(e);
}
process(url);
```
This code alone explains why the designers of modern languages such as Scala, Kotlin, and others chose not to follow
Java's example.  The `exceptions` plugin option provides you with the same choice.  With it enabled you can write the
same code like this:
```java
process(new URL("http://manifold.systems"));
```
Sells itself.

## Lambdas
Perhaps the most offensive checked exception use-cases involve lambdas:
```java
List<String> strings = ...;
List<URL> urls = strings.stream()
  .map(URL::new) // Unhandled exception error: MalformedURLException
  .collect(Collectors.toList());
```
The checked exception prevents concise lambda usage here.  With Manifold, however, you are free to write code as you
like:
```java
List<String> strings = ...;
List<URL> urls = strings.stream()
  .map(URL::new) // Mmm, life is good
  .collect(Collectors.toList());
```

# IDE Support 

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. With the plugin
installed IntelliJ is aware of all things Manifold, including checked exception handling e.g., the editor won't complain
about checked exceptions.

# Setup

## Building this project

The `manifold-exceptions` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-exceptions` dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 19.

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 19), the script takes care of the rest.  
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
    compileOnly 'systems.manifold:manifold-rt:2023.1.3'
    testImplementation 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-exceptions', version: '2023.1.3'
    testAnnotationProcessor group: 'systems.manifold', name: 'manifold-exceptions', version: '2023.1.3'
}

if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
    sourceSets.main.allJava.files.any {it.name == "module-info.java"}) {
    tasks.withType(JavaCompile) {
        // if you DO define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold', '--module-path', it.classpath.asPath]
    }
} else {
    tasks.withType(JavaCompile) {
        // If you DO NOT define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold']
    }
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyProject'
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-exceptions-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Exceptions App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>

    <dependencies>
        <dependency>
            <!-- Necessary only during compile-time to resolve generated source-level annotations -->
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-rt</artifactId>
            <version>${manifold.version}</version>
            <scope>provided</scope> <!-- dependency is only applied during compile-time for manifold-exceptions -->
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
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-exceptions</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

# Javadoc

`manifold-exceptions`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-exceptions/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-exceptions/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
