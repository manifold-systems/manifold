# Manifold : Collections

The `manifold-collections` dependency consists of [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension)
on Java's collection classes and the `Range` API, which leverages the unit (or binding) expressions from the
[`manifold-ext`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext) dependency
for concise syntax. 

```java
// Use unit expressions with the Range API  
for( Mass m: 10kg to 100kg ) {
  . . .
}
``` 
 
## Table of Contents
* [Collections](#collections-extensions)
* [Ranges](#ranges)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)


# Collections Extensions

The `manifold-collections` library uses the `manifold-ext` framework to provide extension methods for Java collection
classes such as `List`, `Iterator`, and `Map`.  The new methods aim to add a bit more convenience to Java collections.

Simply add the `manifold-collections` dependency to your application to automatically access the extensions directly on
the collection classes.  See the [Using this project](#using-this-project) section below for instructions to use this
dependency with your build environment.

# Ranges 

You can easily work with ranges using Manifold's [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions).
Simply import the the `RangeFun` constants to create ranges:
```java
// imports the `to`, `step`, and other "binding" constants
import static manifold.collections.api.range.RangeFun.*;

if (2 inside 1 to 5) {
  out.println("yer in");
}
```
Although `inside` and `to` look like new Java keywords, they are instead normal variables defined as constants in
`RangeFun`. They use Manifold's unit expressions to type-safely build ranges with sensible syntax.

All `Number` types from `int` and `float` to `BigInteger`, `BigDecimal`, and `Rational` support *sequential* ranges that
can be iterated:
```java
for (int i: 1 to 5) {
  out.println(i);
}
``` 
To iterate in reverse order, simply reorder the range endpoints:
```java
for (int i: 5 to 1) {
  out.println(i);
}
``` 

`RangeFun` also defines constants for iterating over a range with a *step*:
```java
for (int i: 10 to 0 step 2) {
  out.println(i);
}
``` 

If you are using [Dimensions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
for the endpoints, you can use the `unit` constant to define unit increments: 
```java
for (Mass m: 2kg to 10kg unit oz) {
  out.println(m);
}
``` 

You can use variations of the `to` constant to exclude the range endpoints:
```java
for (int i: 1 _to 5) {
  out.println(i); // 1 is excluded
}
for (int i: 1 to_ 5) {
  out.println(i); // 5 is excluded
}
for (int i: 1 _to_ 5) {
  out.println(i); // both 1 and 5 are excluded
}
``` 

## Kinds of Ranges

Use the `to` constant with any `Comparable`, `Sequential`, or `Number` type to create a range. A range consisting of
`Sequential` endpoints is a `SequentialRange`, which means it can be iterated, while a range with just `Comparable`
endpoints can only be tested for containment using the `inside` RangeFun constant:

```java
if ("ockham" inside "n" to "zzz") {
  out.println("ok");
}
```
 
Note the `Sequential`, `Range`, `SequentialRange` and others abstractions are defined as part of the Range API in the
`manifold.collections.api.range` package.

 
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

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access extension methods. Create extension methods using a convenient user interface. Make
changes to your extensions and use the changes immediately, no compilation! Use extensions provided by extension library
dependencies. Find usages of any extension. Use the `Range` API and unit expressions with complete type-safety.

# Setup

## Building this project

The `manifold-collections` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-collections` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 19.

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
    implementation 'systems.manifold:manifold-collections:2023.1.3'
    testImplementation 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-collections', version: '2023.1.3'
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

tasks.compileJava {
    classpath += files(sourceSets.main.output.resourcesDir) //adds build/resources/main to javac's classpath
    dependsOn processResources
}
tasks.compileTestJava {
    classpath += files(sourceSets.test.output.resourcesDir) //adds build/resources/test to test javac's classpath
    dependsOn processTestResources
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyExtProject'
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-ext-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Java App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-collections</artifactId>
            <version>${manifold.version}</version>
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
                            <artifactId>manifold-collections</artifactId>
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

`manifold-collections`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-collections/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-collections/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
