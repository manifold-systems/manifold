---
layout: docs_layout
---

# Overview

[Manifold](https://manifold.systems/) plugs directly into Java to supplement it with powerful features you can use
directly in your projects:

* [**Type-safe Metaprogramming**](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold) -- _type-safe_ access to structured data (like [F# _type providers_](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/information-rich-themes-v4.pdf)).
Use [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[JSON](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json), 
[YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml), 
[JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js),
[Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates), etc.
directly and type-safely from Java without a code generator in your build and with comprehensive IDE support. 
* [**Java Extensions**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext) --
provides extension methods (like [C#](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/classes-and-structs/extension-methods)),
structural typing (like [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html)),
string interpolation (like [Kotlin](https://kotlinlang.org/docs/reference/basic-types.html#string-templates)),
type-safe reflection (via [`@Jailbreak`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#type-safe-reflection-via-jailbreak)),
and lot more.


## Type-safe Metaprogramming via _Type Manifolds_ 

Bridging the worlds of information and programming, a *type manifold* acts as an adapter to automatically connect a
structured data source to Java's type system.  The core Manifold framework seamlessly plugs into the Java compiler
enabling a type manifold to transform structured data into a data _type_ directly accessible in your Java code
eliminating code generation build steps otherwise required with conventional tools. Additionally the Manifold plugin
for IntelliJ IDEA provides comprehensive integration for type manifolds. Types are always in sync; changes you make to
structured data are immediately available in the type system _without a compilation step_.  Code completion, navigation,
usage searching, refactoring, incremental compilation, hotswap debugging -- all seamlessly integrated.  With type
manifolds a data source is a virtual data _type_.

To illustrate, consider this simple properties resource file:

`/abc/MyProperties.properties`
```properties
chocolate = Chocolate
chocolate.milk = Milk chocolate
chocolate.dark = Dark chocolate
``` 

Normally in Java you access a properties resources like this:

```java
Properties myProperties = new Properties();
myProperties.load(getClass().getResourceAsStream("/abc/MyProperties.properties"));
String myMessage = myProperties.getProperty("chocolate.milk");
```

As with any resource file a properties file is foreign to Java's type system -- there is no direct, type-safe access to
it. Instead you access it indirectly using boilerplate library code sprinkled with hard-coded strings.

By contrast, with the Properties type manifold you access a properties file directly as a type:

```java
String myMessage = MyProperties.chocolate.milk;
```

Concise and type-safe, with no additional build steps to engage.

Almost any type of data source imaginable is a potential type manifold. These include resource files, schemas, query
languages, database definitions, templates, spreadsheets, web services, and programming languages.

Manifold provides type manifolds for:

*   [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
*   [JSON and JSON Schema](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
*   [YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)
*   [Properties files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
*   [Image files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
*   [Dark Java](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
*   [JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)
*   [ManTL (Template Files)](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)

More are in the works such as SQL, XML, and others.


## Java Extensions via the _Extension_ Manifold

The extension manifold is a special kind of type manifold that lets you augment existing Java classes including Java's
own runtime classes such as `String`. You can add new methods, annotations, and interfaces to any type your project
uses.

Let's say you want to make a new method on `String` so you can straightforwardly echo a String to the console. Normally
with Java you might write a "Util" library like this:

```java
public class MyStringUtil {
  public static void echo(String value) {
    System.out.println(value);
  }
}
```

And you'd use it like this:

```java
MyStringUtil.echo("Java");
```

Instead with Manifold you create an _**Extension Class**_:

```java
@Extension
public class MyStringExtension {
  public static void echo(@This String thiz) {
    System.out.println(thiz);
  }
}
```  

Here we've added a new `echo()` method to `String`, so we use it like this:

```java
"Java".echo();
```

Extensions eliminate a lot of intermediate code such as "Util" and "Manager" libraries as well as Factory classes. As a
consequence extensions naturally promote higher levels of object-orientation, which result in more readable and
maintainable code. Perhaps the most beneficial aspect of extensions, however, relate more to your overall experience
with your development environment.  For instance, code-completion conveniently presents all the extension methods
available on an extended class:

<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/ExtensionMethod.mp4">
  </video>
</p>

There's a lot more to the extension manifold including [structural interfaces](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural),
which are similar to interfaces in the [Go](https://golang.org/) and [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html)
languages. See the [Java Extension Manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
for full coverage of these features.


## Benefits

Manifold's core technology is a dramatic departure from conventional Java tooling. There are no code generation steps in
the build, no extra .class files or .java targets to manage, no annotation processors, and no extra class loaders to
engage at runtime.

Benefits of this approach include:

*   **Zero turnaround** -- direct, type-safe access to structured data
*   **Lightweight** -- requires no special compilers, annotation processors, or runtime agents
*   **Efficient, dynamic** -- Manifold only produces types as they are needed
*   **Simple, open API** -- use the Manifold API to build your own components and extensions
*   **No code generation build step** -- integrates directly with the Java compiler, incremental
*   **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** support -- all manifold types and extensions work with IntelliJ

Additionally, Manifold is just a dependency you can drop into your existing project -- you can begin using
it incrementally without having to rewrite classes or conform to a new way of doing things.


# Setup

Using Manifold in your project is straightforward, but there are configuration details you should understand before you
get going. How you configure Manifold with your project depends on which version of Java you use: Java 8, Java 9 or
later with modules, or Java 9 or later without modules.  Please keep this in mind and read this section carefully.

## I don't want to read all this

If you simply want to experiment, grab the [Manifold sample project](https://github.com/manifold-systems/manifold-sample-project)
and have at it:

* `git clone https://github.com/manifold-systems/manifold-sample-project.git`
* Open [IntelliJ IDEA](https://www.jetbrains.com/idea/download)
* Install the **Manifold plugin** from within IntelliJ:
<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`
* Restart IntelliJ to use the plugin
* Open the project you just cloned (open the root directory or the pom.xml file)
* Add the [Java 11 JDK](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot): `File | Project Structure | SDKs | + | path-to-your-Jdk11`
* Set the project JDK and language level: `File | Project Structure | Project` select `11` for both `Project JDK` and `Project language level`
* Build the project
* Experiment!
  
Note you can change the JDK to any Java release 8 - 12.  But you need to make changes in the `pom.xml` file to make that
work.  There are notes in the `pom.xml` file to help with that.  Read them carefully and maybe read the rest of this 
Setup section too.

>Don't be shy with questions, comments, or complaints. We want *all* your feedback, good or bad! We use github issues
to track feedback [here](https://github.com/manifold-systems/manifold/issues). Or start a discussion on
[gitter](https://gitter.im/manifold-systems/community).  Also feel free to send email: [info@manifold.systems](mailto:info@manifold.systems).

## Basics

Using Manifold in your Java project consists of two basic steps:

* Add Manifold dependencies you want to use
* Add Manifold as a plugin argument to javac

That's basically it, however there are nuances with Java, Maven, Gradle, etc. to consider depending on the version of
Java your project uses.

Manifold fully supports Java releases 8 - 12 on all platforms, including Windows, Linux, and Mac.

**Java 9 or later Notes**

If you are using **Java 9 or later** with **named modules**, your `module-info` file must declare dependencies to the
manifold components you are using. For example, if you are using `manifold-all`, ensure your `module-info.java` declares
`requires` statements corresponding with Manifold usage:
```java
module your.module.name {
  // Mandatory
  requires manifold.all;  // the manifold-all jar file (or a set of constituent core Manifold jars)

  // Mandatory for **Java 11** or later in MULTI-MODULE MODE
  requires jdk.unsupported; // As a convenience Manifold uses internal Java APIs to make module setup easier for you

  // Optional
  requires java.scripting;  // if using JSON or YAML manifolds: for javax.script.Bindings
  requires java.desktop;  // if using Image manifold: for javax.swing.ImageIcon
}
```
Additionally **Java 9 or later** projects must include the `-processorpath` or `--processor-module-path` argument for
the manifold jar file along with the `-Xplugin:Manifold` argument to javac:
```
javac -Xplugin:Manifold -processorpath /path/to/your/manifold-all.jar ...
```
Details concerning `-processorpath` configuration in **Maven** and **Gradle** are covered later in this section.
 
**Java 8 Notes**

If you are using **Java 8** you may need to include `tools.jar` in your classpath (runtime only). Your project requires
tools.jar if you are using Manifold in *dynamic* mode, as opposed to *static* mode. See [Modes](#Modes) for details.
```java
java -classpath <your-classpath>;/path/to/jdk/tools.jar ...
```
As a minor note, the `-processorpath` argument for **Java 8** `javac` may be omitted:
```
javac -Xplugin:Manifold ...
```

## Working with IntelliJ

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

**Install**

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>


**Sample Project**

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>


## Binaries

For the convenience of non-maven users you can directly download Manifold binaries:
* [manifold-all](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-all&v=RELEASE):
Uber-jar containing all of the binaries below (recommended)
* [manifold](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE):
Core Manifold support, also includes properties and image manifolds
* [manifold-ext](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-ext&v=RELEASE):
Support for structural typing and extensions
* [manifold-properties](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-properties&v=RELEASE):
Properties files support
* [manifold-image](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-image&v=RELEASE):
Image files support
* [manifold-darkj](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-darkj&v=RELEASE):
Dark Java support
* [manifold-json](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-json&v=RELEASE):
JSON and JSON Schema support
* [manifold-yaml](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-yaml&v=RELEASE):
YAML support
* [manifold-graphql](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-graphql&v=RELEASE):
GraphQL support
* [manifold-js](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-js&v=RELEASE):
JavaScript support
* [manifold-collections](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-collections&v=RELEASE):
Collections extensions
* [manifold-io](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-io&v=RELEASE):
I/O extensions
* [manifold-text](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-text&v=RELEASE):
Text extensions
* [manifold-templates](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-templates&v=RELEASE):
Integrated template support
* [manifold-preprocessor](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-preprocessor&v=RELEASE):
A fully integrated Preprocessor for Java
* [manifold-strings](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-strings&v=RELEASE):
String templates (aka String interpolation)


## Maven
To setup a Maven project with Manifold you must:
* specify the Manifold dependencies you need
* configure the `maven-compiler-plugin`

### Dependencies

Use the `manifold-all` dependency as a simple way to use all of Manifold's basic features.  This is the 
recommended setup.

```xml
  <!--Includes ALL basic dependencies (recommended) -->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-all</artifactId>
    <version>RELEASE</version>
  </dependency>
```

*Or* choose from the list of individual dependencies:

```xml
  <!--Core Manifold support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Support for extension methods, structural typing, string interpolation, @Jailbreak, @Self, @Precompile-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-ext</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Properties files support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-properties</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Image files support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-image</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Dark Java support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-darkj</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--JSON and JSchema support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-json</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--YAML support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-yaml</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--JavaScript support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-js</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Template support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-templates</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Collections extensions-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-collections</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--I/O extensions-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-io</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Text extensions-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-text</artifactId>
    <version>RELEASE</version>
  </dependency>    
  
  <!--String Templates support (aka String Interpolation)-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-strings</artifactId>
    <version>RELEASE</version>
  </dependency>    
  
  <!--A fully integrated Preprocessor for Java-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-preprocessor</artifactId>
    <version>RELEASE</version>
  </dependency>    
```

## Configure the maven-compiler-plugin

You setup the `maven-compiler-plugin` according to the version of Java your project uses.
Use one of the following configurations depending on whether you use:
* Java 8
* Java 9+ with *unnamed* module
* Java 9+ with named module[s] (defines module-info.java files)

### Java 8
```xml
  <build>
    <plugins>     
      <!--
       *** JAVA 8 ***
  
       Configure the maven-compiler-plugin use Manifold.
       - add the -Xplugin:Manifold argument for the javac compiler
      -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.0</version>
        <configuration>
          <encoding>UTF-8</encoding>
          <compilerArgs>
  
            <!-- Add the Manifold plugin -->
            <arg>-Xplugin:Manifold</arg>
  
          </compilerArgs>
        </configuration>
      </plugin>
    
    </plugins>
  </build>
```

```xml
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
```

### Java 9+ with *unnamed* module
```xml
  <build>
    <plugins>  
      <!--
       *** JAVA 9+ DEFAULT MODULE MODE***
        (project does *NOT* define module-info.java file)
  
       Configure the maven-compiler-plugin use Manifold.
       - add the -Xplugin:Manifold argument for the javac compiler
       - add the manifold-all module to javac -processorpath arg
      -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
  
        <!-- version 3.8.0+ is necessary to support Java 10+ -->
        <version>3.8.0</version>
  
        <configuration>
          <encoding>UTF-8</encoding>
          <compilerArgs>
  
            <!-- Add the Manifold plugin -->
            <arg>-Xplugin:Manifold</arg>
  
          </compilerArgs>
  
          <!-- Add the processor path for the plugin (required for Java 9+ -->
          <annotationProcessorPaths>
            <path>
              <groupId>systems.manifold</groupId>
              <artifactId>manifold-all</artifactId>
              <version>${manifold-version}</version>
            </path>
          </annotationProcessorPaths>
  
        </configuration>
      </plugin>
    </plugins>
  </build>
```

### Java 9+ with named modules (uses module-info.java files)
```xml
  <build>
    <plugins>
      <!--
        *** JAVA 9+ MULTI-MODULE MODE ***
         (project defines module-info.java file)
        
        Configure the maven-compiler-plugin to use Manifold.
        - add the manifold-all module to -add-modules arg
        - add the -Xplugin:Manifold argument for the javac compiler
        - add the manifold-all module to javac -processorpath arg
      -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
  
        <!-- version 3.8.0+ is necessary to support Java 10+ -->
        <version>3.8.0</version>
  
        <configuration>
          <encoding>UTF-8</encoding>
          <compilerArgs>
  
            <!--Add the manifold-all module-->
            <arg>--add-modules</arg>
            <arg>manifold.all</arg>
  
            <!--Add the Manifold plugin-->
            <arg>-Xplugin:Manifold</arg>
  
          </compilerArgs>
  
          <!-- Add the processor path for the plugin (required for Java 9+ -->
          <annotationProcessorPaths>
            <path>
              <groupId>systems.manifold</groupId>
              <artifactId>manifold-all</artifactId>
              <version>${manifold-version}</version>
            </path>
          </annotationProcessorPaths>
  
        </configuration>
      </plugin>
    </plugins>
  </build>
``` 

### Surefire

Here is a simple project layout demonstrating use of the `manifold-all` with Surefire:

```xml
  <build>
    <plugins>

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.20.1</version>
        <configuration>
        
          <!-- Java 8 only -->
          <additionalClasspathElements>
            <additionalClasspathElement>${java.home}/../lib/tools.jar</additionalClasspathElement>
          </additionalClasspathElements>
          
        </configuration>
      </plugin>
    </plugins>
  </build>

```

## Gradle

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact(s) and the `-Xplugin:Manifold` argument as a Java 
compiler argument:

```groovy
apply plugin: 'java'

dependencies {
  // -- All manifold, includes all other dependencies listed here --
  compile group: 'systems.manifold', name: 'manifold-all', version: 'RELEASE'


  // -- Or individual dependencies --
  
  // Core Manifold support
  compile group: 'systems.manifold', name: 'manifold', version: 'RELEASE'
  
  // Support for extension methods, structural typing, string interpolation, @Jailbreak, @Self, @Precompile
  compile group: 'systems.manifold', name: 'manifold-ext', version: 'RELEASE'
    
  // Properties files support  
  compile group: 'systems.manifold', name: 'manifold-properties', version: 'RELEASE'
  
  // Image files support  
  compile group: 'systems.manifold', name: 'manifold-image', version: 'RELEASE'
  
  // Dark Java support  
  compile group: 'systems.manifold', name: 'manifold-darkj', version: 'RELEASE'
  
  // JSON and JSchema support  
  compile group: 'systems.manifold', name: 'manifold-json', version: 'RELEASE'
  
  // YAML support
  compile group: 'systems.manifold', name: 'manifold-yaml', version: 'RELEASE'
  
  // GraphQL support
  compile group: 'systems.manifold', name: 'manifold-graphql', version: 'RELEASE'
  
  // JavaScript support
  compile group: 'systems.manifold', name: 'manifold-js', version: 'RELEASE'
  
  // Template support (ManTL)
  compile group: 'systems.manifold', name: 'manifold-templates', version: 'RELEASE'
  
  // Collection extensions
  compile group: 'systems.manifold', name: 'manifold-collections', version: 'RELEASE'
  
  // I/O extensions
  compile group: 'systems.manifold', name: 'manifold-io', version: 'RELEASE'
  
  // Text extensions
  compile group: 'systems.manifold', name: 'manifold-text', version: 'RELEASE'
  
  // A Java Preprocessor
  compile group: 'systems.manifold', name: 'manifold-preprocessor', version: 'RELEASE'
  
  // String Templates support (aka String Interpolation)
  compile group: 'systems.manifold', name: 'manifold-strings', version: 'RELEASE'
  
  
  // -- For Java 9+ ==
  
  // Add manifold-all to -processorpath for javac
  annotationProcessor group: 'systems.manifold', name: 'manifold-all', version: 'RELEASE'
  
  
  // -- For Java 8 only --
  
  // tools.jar dependency
  compile files("${System.properties['java.home']}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
  options.compilerArgs += '-Xplugin:Manifold'
  options.fork = true
}
```

### Java 8
Here is a sample `build.gradle` file using `manifold-all` with **Java 8**:
```gradle
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
    compile group: 'systems.manifold', name: 'manifold-all', version: '2019.1.8'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // tools.jar dependency (for Java 8 only)
    compile files("${System.properties['java.home']}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```gradle
rootProject.name = 'MySampleProject'
```

### Java 11
Here is a sample `build.gradle` file using `manifold-all` with **Java 11**:
```gradle
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
    compile group: 'systems.manifold', name: 'manifold-all', version: '2019.1.8'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold-all to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-all', version: '2019.1.8'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```gradle
rootProject.name = 'MySampleProject'
```

# What Is a Type Manifold?

Structured information is _everywhere_ and it is produced by near _everything_ with a power cord. 
As a consequence the software industry has become much less code-centric and much more information-centric. Despite 
this transformation the means by which our software consumes structured information has remained unchanged for decades.
Whether it's GraphQL, JSON, SQL, Javascript, or any one of a multitude of other structured data sources, most modern 
languages, including Java, do very little to connect them with your code.

Developers typically use _type-bridging_ tools such as code generators as a means to bridge this gap. But because
a type-bridging tool is not an integral part of the Java compiler or JVM, it must run in a separate build step. A code
generator, for example, must compile its *full domain* of types separately in advance of the normal build sequence.
This disconnect is notorious for causing a host of problems, which include:
* stale generated classes
* no support for *incremental* compilation, all or nothing
* long build times
* code bloat
* can't navigate from code reference to corresponding element in the structured data
* can't find code usages of elements from the structured data  
* can't refactor / rename structured data elements 
* complicated custom class loader issues, generated classes loaded in separate loader
* concurrency problems with the shared thread context loader
* generated code is often cached and shared, which leads to stale cache issues
* customers often need to change metadata, which requires access to code generators

In stark contrast to code generators, the _Type Manifold API_ plugs directly into the Java compiler to produce
types on demand. As such your code can reference a structured data source directly and type-safely without any code
generation build step.  In essence the Type Manifold API reinvents code generation:
* Structured data sources are virtual Java types!
* Your build process is now free of code generation management
* Using a type manifold is simply a matter of adding a Jar file to your project
* You can perform *incremental* compilation based on changes to structured data
* You can add/remove/modify a structured data source in your project and immediately use and see the change in your code
* You can compile projected classes to disk as normal class files or use them dynamically at runtime
* There are no custom class loaders involved and no thread context loaders to manage
* You can navigate from a code reference to a structured data source in your IDE
* You can perform usage searches on elements in structured data sources to find code references
* You can rename / refactor elements in structured data sources

Further, the Type Manifold API unifies code generation architecture by providing much needed structure and consistency 
for developers *writing* code gen tooling. It puts an end to "lone wolf" code gen projects only one developer fully understands.
Moreover, you don't have to invest in one-off IDE integration projects; the Manifold plugin for IntelliJ handles *everything* 
for you.  All types and extensions provided by the Type Manifold API are fully managed in IntelliJ, including incremental
compilation, usage searching, and refactoring.  As a consequence IntelliJ integration is free when you use Manifold in your
architecture.

>Note if you've already invested in a conventional code generator, you can still recycle it as a *wrapped* type
manifold -- the wrapper can delegate source production to your existing framework.


To illustrate, consider this simple example. Normally you access Java properties resources like this:

```java
Properties myProperties = new Properties();
myProperties.load(getClass().getResourceAsStream("/abc/MyProperties.properties"));
String myMessage = myProperties.getProperty("my.message");
```

This is typical boilerplate library code, but since properties files are foreign to Java's type
system there is no direct, type-safe access to them.

With the properties type manifold, however, Java escapes the confinements of its
conventional type system.  The properties type manifold provides
a Java class projection of properties resource files, eliminating the need for the
`Properties` library:

```java
String myMessage = MyProperties.my.message;
```

Concise and type-safe!  And _on-demand_ -- type manifolds supply type information only
as required by the compiler and never generate files.  Likewise, at runtime
types are created and loaded lazily and don't require special class loaders.

Any type of data source accessible to Java is a potential type manifold. These include resource files,
schemas, queries, database definitions, data services, templates, spreadsheets, programming languages, etc.
 
Currently Manifold provides reference implementations for a few commonly used data sources:

*   [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
*   [JSON and JSON Schema](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
*   [YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)
*   [Properties files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
*   [Image files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
*   [Dark Java](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
*   [ManTL (Template Files)](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)
*   [JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)

More type manifolds are in the works:
*   XML
*   R Language
*   Swagger
*   Standard SQL and DDL

# IDE -- IntelliJ IDEA

Use the [Manifold IntelliJ IDEA plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to experience Manifold to its fullest.

The plugin currently supports most high-level IntelliJ features including:
* Feature highlighting
* Error reporting
* Code completion
* Go to declaration
* Usage searching
* Rename/Move refactoring
* Quick navigation
* Structural typing
* Type-safe reflection with `@Jailbreak`
* Self type support with `@Self`
* Incremental compilation
* Hotswap debugging
* Preprocessor (conditional compilation)
* Professional template file editor

The IntelliJ plugin provides comprehensive support for Manifold. Use code completion to discover and use type manifolds, extension
methods and structural interfaces. Jump directly from usages of extension methods to their declarations.
Likewise, jump directly from references to data source elements and find usages of them in your code.
Watch your JSON/YAML, images, properties, templates, and custom type manifolds come alive as types.
Changes you make are instantly available in your code:

Install the plugin directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

# License

## Open Source
Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

## Commercial
Commercial licenses for this work are available. These replace the above ASL 2.0 and offer 
limited warranties, support, maintenance, and commercial server integrations.

For more information, please visit: http://manifold.systems//licenses

Contact: admin@manifold.systems

# Contributing

To contribute a change to Manifold [open source](https://github.com/manifold-systems/manifold):

* Fork the main manifold repository
* Create a new feature branch based on the `development` branch with a reasonably descriptive name (e.g. `fix_json_specific_thing`)
* Implement your fix and write tests
* Create a pull request for that branch against `development` in the main repository

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Building

To build:

    mvn clean compile 
    
To execute tests:

    mvn test

# Author

* [Scott McKinney](https://www.linkedin.com/in/scott-mckinney-52295625/) - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*
