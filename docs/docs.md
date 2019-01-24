---
layout: docs_layout
---

## Manifold in a Nutshell

[Manifold](https://manifold.systems/) is a unique framework to dynamically and _seamlessly_ extend
Java. Building on this core framework Manifold supplements Java with new features you can use in your applications:

* **Type-safe Metaprogramming** -- renders code generators obsolete, similar in concept to [F# _type providers_](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/information-rich-themes-v4.pdf)
* **Extension Methods** -- add methods to classes you don't own, comparable to the same feature in [C#](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/classes-and-structs/extension-methods) and [Kotlin](https://kotlinlang.org/docs/reference/extensions.html)
* **Structural Typing** -- type-safe duck typing, much like interfaces in [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html) and [Go](https://tour.golang.org/methods/10)

Leveraging these key features Manifold delivers a set of high-level components you can plug into your project, these
include:
* **JSON** and **JSON Schema** integration
* Type-safe **Templating** 
* **Structural interfaces** and **Expando** objects
* **Extension libraries** for collections, I/O, and text
* **JavaScript** interop (experimental)
* **SQL** and **DDL** interop (coming soon)
* **Self** type support
* **Jailbreak** type-safe reflection
* Lots more

At a high level each of these features is classified as either a **Type Manifold** or an
**Extension** via the **Extension Manifold**.

### Type Manifolds

Bridging the worlds of information and programming, *type manifolds* act as adapters 
to automatically connect schematized data sources with Java.  More specifically, 
a type manifold transforms a data source into a data _type_ directly accessible in 
your Java code eliminating code generation build steps involved with conventional tools. 
Additionally Manifold provides IDE integration to automatically keep types in sync with 
data sources as you make changes, again with no addtitional build steps. In essence with 
Manifold a data source **_is_** a data type.

To illustrate, consider this properties resource file:

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

As with any resource file a properties file is foreign to Java's type system -- there is no direct,
type-safe access to it. Instead you access it indirectly using boilerplate library code sprinkled
with hard-coded strings.

By contrast, with the Properties type manifold you access a properties file directly as a type:

```java
String myMessage = MyProperties.chocolate.milk;
```

Concise and type-safe, with no additional build steps to engage.

Almost any type of data source imaginable is a potential type manifold. These
include resource files, schemas, query languages, database definitions, templates,
spreadsheets, web services, and programming languages.

Currently Manifold provides type manifolds for:

*   JSON and [JSON Schema](http://json-schema.org/)
*   Properties files
*   Image files
*   Dark Java
*   ManTL (Manifold Template Language)
*   JavaScript (experimental)
*   DDL and SQL (work in progress)


### The Extension Manifold

The extension manifold is a special kind of type manifold that lets you augment existing Java classes
including Java's own runtime classes such as `String`. You can add new methods, annotations, and 
interfaces to any type your project uses.

Let's say you want to make a new method on `String` so you can straightforwardly echo a String to the
console. Normally with Java you might write a "Util" library like this:

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

Extensions eliminate a lot of intermediate code such as "Util" and "Manager"
libraries as well as Factory classes. As a consequence extensions naturally
promote higher levels of object-orientation, which result in more readable and
maintainable code. Perhaps the most beneficial aspect of extensions, however, relate more
to your overall experience with your development environment.  For instance,
code-completion conveniently presents all the extension methods available on an
extended class:

<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/ExtensionMethod.mp4">
  </video>
</p>

There's a lot more to the extension manifold including [structural interfaces](#structural_interfaces), which are
similar to interfaces in the [Go](https://golang.org/) language. We'll cover more later in this guide.


### Benefits

Manifold's core technology is a dramatic departure from conventional Java tooling. There is no code
generation step in the build, no extra .class files or .java files to manage, no annotation processors, and no extra
class loaders to engage at runtime.

Benefits of this approach include:

*   **Zero turnaround** -- direct, type-safe access to structured data
*   **Lightweight** -- requires no special compilers, annotation processors, or runtime agents
*   **Efficient, dynamic** -- Manifold only produces types as they are needed
*   **Simple, open API** -- use the Manifold API to build your own components and extensions
*   **No code generation build step** -- integrates directly with the Java compiler, incremental
*   **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** support -- all manifold types and extensions work with IntelliJ

Additionally, Manifold is just a JAR file you can drop into your existing project -- you can begin using
it incrementally without having to rewrite classes or conform to a new way of doing things.


## Setup

Using Manifold in your project is easy, but there are configuration details you should understand before you get going.
Your project's configuration of Manifold largely depends on which version of Java it uses, namely Java 8, Java 9 or later with 
modules, or Java 9 or later without modules.  Please keep this in mind and read this section carefully.

### I don't want to read all this

If you simply want to experiment, grab the [Manifold sample project](https://github.com/manifold-systems/manifold-sample-project) and go nuts:

* `git clone https://github.com/manifold-systems/manifold-sample-project.git`
* Open [IntelliJ IDEA](https://www.jetbrains.com/idea/download)
* Install the **Manifold plugin** from within IntelliJ: `Settings | Plugins | Browse Repositories | Manifold`
* Restart IntelliJ to use the plugin
* Open the project you just cloned (open the root directory or the pom.xml file)
* Add the [Java 11 JDK](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot): `File | Project Structure | SDKs | + | path-to-your-Jdk11`
* Set the project JDK and language level: `File | Project Structure | Project` select `11` for both `Project JDK` and `Project language level`
* Build the project
* Go nuts
  
Note you can change the JDK to Java 8, 9, 10 or 11.  But you need to make changes in the `pom.xml` file to make that
work.  There are notes in the `pom.xml` file to help with that.  Read them carefully and maybe read the rest of this 
Setup section too.

### Basics

Using Manifold in your Java project is simple:

* Add the Manifold jar as a plugin argument to java**c**
* Add the Manifold jar to your java classpath (optional)

That's basically it, however there are nuances with Java, Maven, Gradle, etc. to consider depending on the version of
Java your project uses.

Manifold fully supports [Java 8](https://adoptopenjdk.net/releases.html?variant=openjdk8&jvmVariant=hotspot), 
[Java 9](https://adoptopenjdk.net/releases.html?variant=openjdk9&jvmVariant=hotspot), 
[Java 10](https://adoptopenjdk.net/releases.html?variant=openjdk10&jvmVariant=hotspot), and 
[Java 11](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot).  

**Java 9 or later Notes**

If you are using **Java 9 or later** with `module-info` files you must declare dependencies to the manifold jars you are using.  
For example, if you are using `manifold-all.jar`, ensure your `module-info.java` declares `requires` statements corresponding
with Manifold usage:
```java
module your.module.name {
  // Mandatory
  requires manifold.all;  // the manifold-all jar file (or a set of constituent core Manifold jars)

  // Mandatory for **Java 11** or later in MULTI-MODULE MODE
  requires jdk.unsupported; // As a convenience Manifold uses internal Java APIs to make module setup easier for you

  // Optional
  requires java.scripting;  // if using Json manifold: for javax.script.Bindings
  requires java.desktop;  // if using Image manifold: for javax.swing.ImageIcon
  requires jdk.scripting.nashorn;  // if using Javascript manifold
}
```
Additionally **Java 9 or later** projects must include the `-processorpath` for the manifold jar file along with the `-Xplugin:Manifold` argument to javac:
```
javac -Xplugin:Manifold -processorpath /path/to/your/manifold-all.jar ...
```
Details concerning `-processorpath` configuration in **Maven** and **Gradle** are covered later in this section.
 
**Java 8 Notes**

If you are using **Java 8** you may need to include `tools.jar` in your classpath (runtime only).
Your project requires tools.jar if you are using Manifold in *dynamic* mode, as opposed to 
*static* mode. See [Modes](#Modes) for details.
```java
java -classpath <your-classpath>;/path/to/jdk/tools.jar ...
```
As a minor note, the `-processorpath` argument for **Java 8** `javac` may be omitted:
```
javac -Xplugin:Manifold ...
```

### Modes

You can use Manifold in one of two ways: **dynamically** or **statically**.  The mode 
determines whether or not Manifold compiles resource types to disk at compile-time, and in 
turn whether or not Manifold dynamically compiles and loads the classes at runtime.  The mode is
controlled using the `-Xplugin` javac argument:

**Static**: `-Xplugin:Manifold` (default, compiles resource types statically at compile-time)

**Dynamic**: `-Xplugin:Manifold dynamic` (compiles resource types dynamically at runtime)
(alternatively `-Xplugin:"Manifold dynamic"`, some tools may require quotes)

The mode you use largely depends on your use-case and personal preference. Things to consider:

* Both modes operate _lazily_: a type is not compiled unless it is used. For example, if you are using the [Json manifold](#json-and-json-schema), 
only the Json files you reference in your code will be processed and compiled. This means Manifold will not try to
compile resources your project does not expect to use directly as types.

* Even if you use static mode, you can still reference type manifold classes dynamically e.g., _reflectively_.
In such a case Manifold will dynamically compile the referenced class as if you were operating in 
dynamic mode.  In general, your code will work regardless of the mode you're using; Manifold will
figure out what needs to be done. 

* Dynamic mode requires `tools.jar` at runtime for **Java 8**.  Note tools.jar may still be required with 
static mode, depending on the Manifold features you use.  For example, [structural interfaces](#structural-interfaces)
requires tools.jar, regardless of mode.  The Json manifold models both sample Json files and [Json Schema](http://json-schema.org/) 
files as structural interfaces.

* Static mode is generally faster at runtime since it pre-compiles all the type manifold resources when you 
build your project

* Static mode automatically supports **incremental compilation** and **hotswap debugging** of modified resources in IntelliJ
   

### Working with IntelliJ

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

**Install**

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ via:

```Settings | Plugins | Browse Repositories | Manifold```

<p><img src="/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>


**New Project**

Creating a new project with Manifold support is easy:

<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/NewProject.mp4">
  </video>
</p>


**Add Manifold to Existing Module**

Adding manifold to module[s] of an existing project is easy:

<p><img src="/images/ManifoldModule.png" alt="echo method" width="60%" height="60%"/></p>


**Sample Project**

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

```File | New | Project from Version Control | Git```

<p><img src="/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

<p><img src="/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>


### Binaries

For the convenience of non-maven users you can directly download Manifold binaries:
* [manifold-all](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-all&v=RELEASE):
Uber-jar containing all of the binaries below (recommended)
* [manifold](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE):
Core Manifold support, also includes properties and image manifolds
* [manifold-ext](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-ext&v=RELEASE):
Support for structural typing and extensions
* [manifold-json](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-json&v=RELEASE):
JSON and JSchema support
* [manifold-js](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-js&v=RELEASE):
JavaScript support (experimental)
* [manifold-collections](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-collections&v=RELEASE):
Collections extensions
* [manifold-io](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-io&v=RELEASE):
I/O extensions
* [manifold-text](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-text&v=RELEASE):
Text extensions
* [manifold-templates](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-templates&v=RELEASE):
Integrated template support


### Maven
To setup a Maven project with Manifold you must:
* specify the Manifold dependencies you need
* configure the `maven-compiler-plugin`

**Dependencies**

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
  <!--Core Manifold support, includes properties and image manifolds-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--Support for structural typing and extensions-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-ext</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--JSON and JSchema support-->
  <dependency>
    <groupId>systems.manifold</groupId>
    <artifactId>manifold-json</artifactId>
    <version>RELEASE</version>
  </dependency>
  
  <!--JavaScript support (experimental)-->
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
```

**Configure the maven-compiler-plugin**

You setup the `maven-compiler-plugin` according to the version of Java your project uses.
Use one of the following configurations depending on whether you use:
* Java 9 or later with modules
* Java 9 or later without modules
* Java 8

`Java 9 or later` using *modules* mode -- project defines at least one `module-info.java` file
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
  
            <!--Add the Manifold plugin, with string templates enabled-->
            <arg>-Xplugin:Manifold strings</arg>
  
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

`Java 9 or later` using *default* mode -- project does **NOT** defines a `module-info.java` file
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
  
            <!-- Add the Manifold plugin, with string templates enabled -->
            <arg>-Xplugin:Manifold strings</arg>
  
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

`Java 8`
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
  
            <!-- Add the Manifold plugin, with string templates enabled -->
            <arg>-Xplugin:Manifold strings</arg>
  
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

***Surefire***

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

**Archetype**

A Maven archetype facilitates new project creation.  Use the Manifold [archetype](https://github.com/manifold-systems/archetype) to quickly
create a new Manifold project.  This is an easy process from IntelliJ:

<p><img src="/images/archetype.png" alt="echo method" width="60%" height="60%"/></p>


### Gradle

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact(s) and the `-Xplugin:Manifold` argument as a Java 
compiler argument:

```groovy
apply plugin: 'java'

dependencies {
  // -- All manifold, includes all other dependencies listed here --
  compile group: 'systems.manifold', name: 'manifold-all', version: 'RELEASE'


  // -- Or individual dependencies --
  
  // Core Manifold support, includes properties and image manifolds
  compile group: 'systems.manifold', name: 'manifold', version: 'RELEASE'
  
  // Support for structural typing and extensions
  compile group: 'systems.manifold', name: 'manifold-ext', version: 'RELEASE'
    
  // JSON and JSchema support  
  compile group: 'systems.manifold', name: 'manifold-json', version: 'RELEASE'
  
  // JavaScript support (experimental)
  compile group: 'systems.manifold', name: 'manifold-js', version: 'RELEASE'
  
  // Template support
  compile group: 'systems.manifold', name: 'manifold-templates', version: 'RELEASE'
  
  // Collection extensions
  compile group: 'systems.manifold', name: 'manifold-collections', version: 'RELEASE'
  
  // I/O extensions
  compile group: 'systems.manifold', name: 'manifold-io', version: 'RELEASE'
  
  // Text extensions
  compile group: 'systems.manifold', name: 'manifold-text', version: 'RELEASE'
  
  
  // -- For Java 9 or later ==
  
  // Add manifold-all to -processorpath for javac
  annotationProcessor group: 'systems.manifold', name: 'manifold-all', version: 'RELEASE'
  
  
  // -- For Java 8 only --
  
  // tools.jar dependency
  compile files("\${System.getProperty('java.home')}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
  options.compilerArgs += '-Xplugin:Manifold'
  options.fork = true
}
```

## What Is a Type Manifold?

Structured information is _everywhere_ and it is produced by near _everything_ with a power cord. 
As a consequence the software industry has become much less code-centric and much more information-centric. Despite 
this transformation the means by which our software consumes structured information has remained unchanged for decades.
Whether it's JSON, XSD/XML, RDF, CSV, DDL, SQL, Javascript, or any one of a multitude of other metadata sources, most modern 
languages, including Java, do very little to connect them with your code.

Developers are conditioned to reach for code generators and static libraries as a means to bridge the gap. 
Collectively these are referred to as _type-bridging_ tools because they provide types and methods to
bridge or map Java code to structured information.  But because type-bridging is not an integral part of the
Java compiler or JVM, code generators necessarily implement a **_push_** architecture -- the compiler can't ask for
a class to be generated, instead the generator processes its full domain of types ahead of time and writes them to disk 
so that the compiler can use them in a later build step.  This disconnect is the source of a host of problems:
* stale generated classes
* long build times
* the domain graph of metadata is too large to generate, code bloat
* changes to structured data don't invalidate generated code
* no support for incremental compilation, all or nothing
* can't navigate from code reference to corresponding element in the structured data
* can't find code usages of elements from the structured data  
* can't refactor / rename structured data elements 
* complicated custom class loader issues, generated classes loaded in separate loader
* concurrency problems with the shared thread context loader
* generated code is often cached and shared, which leads to stale cache issues
* customers often need to change metadata, which requires access to code generators

In stark contrast to code generators, the _Type Manifold API_ naturally promotes a **_pull_** architecture.  The API plugs into the Java compiler so that 
a type manifold implementing the API resolves and produces types only as needed. In other words the compiler **_drives_** a type manifold by 
asking it to resolve types as the compiler encounters them.  As such your code can reference structured data sources 
directly as Java types as defined by the type manifolds your project uses.  In essence the Type Manifold API reinvents code generation:
* Structured data sources are virtual Java types!
* Your build process is now free of code generation management
* Using a type manifold is simply a matter of adding a Jar file to your project
* You can perform incremental compilation based on changes to structured data
* You can add/remove/modify a structured data source in your project and immediately use and see the change in your code
* You can compile projected classes to disk as normal class files or use them dynamically at runtime
* There are no custom class loaders involved and no thread context loaders to manage
* You can navigate from a code reference to a structured data source in your IDE
* You can perform usage searches on elements in structured data sources to find code references
* You can rename / refactor elements in structured data sources

Further, the Type Manifold API unifies code generation architecture by providing much needed structure and consistency 
for developers writing code generators. It puts an end to "lone wolf" code gen projects only one developer fully understands.
Moreover, you don't have to invest in one-off IDE integration projects; the Manifold plugin for IntelliJ handles everything 
for you, from incremental compilation to usage searching to refactoring.  Finally, even if you've already invested in an 
existing code generator, you can still recycle it as a wrapped type manifold -- the wrapper can delegate 
source production to your existing framework.

To illustrate, consider this simple example. Normally you access Java properties
resources like this:

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

*   JSON and JSON Schema
*   Properties files
*   Image files
*   Dark Java
*   Templating

We are working on support for more data sources including:
*   RDF
*   CSV
*   JavaScript
*   Standard SQL and DDL


### JSON and JSON Schema
[JSON](http://www.json.org/) has become the wire protocol of choice and, more generally, the preferred
structured data format. There is no shortage of JSON libraries for Java, these include 
[Jackson](https://github.com/FasterXML/jackson-docs), [Gson](https://github.com/google/gson), and a 
multitude of others.  They all do basically the same thing -- given a pre-defined or generated type, 
the libraries can read from and write to the type in terms of JSON:

```java
Widget widget = new Widget(bindings);
ObjectMapper mapper = new ObjectMapper();

String jsonStr = mapper.writeValueAsString(widget);
Widget result = mapper.readValue(jsonStr, Widget.class);
```

The JSON type manifold takes a different approach. Avoiding multiple systems of
record, instead of generating classes or weaving annotations into hand crafted
code the type manifold directly maps JSON sample files or JSON Schema files to
Java's type system as abstract types.

As this example illustrates, you work directly with a JSON bindings object as if it were a
Java class:

```java
Widget widget = (Widget) bindings;

String jsonStr = widget.toJson();
Widget result = Widget.fromJson(jsonStr); 
```

This approach eliminates library usage and connects your code directly to JSON metadata.
Another advantage involves object identity.  The `Widget` type is just
an interface directly on the JSON bindings.  The interface both abstracts and preserves the 
implementation details of the underlying JSON object -- the `Bindings` object isn't wrapped or proxied.
The type manifold achieves this via [extension interfaces](#extension_interfaces) on the
`javax.script.Bindings` object and through the use of [structural interfaces](#structural_interfaces) -- 
JSON types are structural interfaces. Essentially, the `widget` object _is_ the `Bindings` object; 
changes you make to `widget` are changes directly on the Bindings.  Additionally, like all type 
manifolds, there are no generated files or other build steps involved. 
 
Combining forces, Manifold [extension libraries](#extension_libraries) help reduce
common remote API drudgery involving JSON:

```java
WidgetQuery query = WidgetQuery.create();
...
WidgetResults result = (WidgetResults) query.postForJsonContent("http://acme.widgets/find");
```

This example uses the `postForJsonContent()` extension method which performs an HTTP Post using `query` 
JSON bindings and transforms the resulting document to JSON bindings, which is directly castable to the 
`WidgetResults` structural interface. 

### Properties Files

Many Java applications incorporate
[properties resource files](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html)
 (*.properties files) as a means of separating configurable text from code:

`resources/abc/MyProperties.properties`:
```properties
my.chocolate = Chocolate
my.chocolate.dark = Dark Chocolate
my.chocolate.milk = Milk Chocolate
```

Unfortunately access to these files requires boilerplate library code and the use
of hard-coded strings:
```java
Properties myProperties = new Properties();
myProperties.load(getClass().getResourceAsStream("/abc/MyProperties.properties"));

println(myProperties.getProperty("my.dark.chocolate"));
```

With the Properties type manifold we can access properties directly using simple, type-safe code:
```java
println(abc.MyProperties.my.dark.chocolate);
```

Behind the scenes the properties type manifold creates a Java class for the
properties file, which reflects its hierarchy of properties.  As you develop your
application, changes you make in
the file are immediately available in your code with no user intervention in
between -- no code gen files and no compiling between changes.

### Image Files

User interfaces frequently use image resource files for one purpose or another.  Java supports most of the
popular formats including png, jpg, gif, and bmp via a collection of utility classes such as
`javax.swing.ImageIcon` and `javax.scene.image.Image`.
  
As with any library, access to the underlying data source is indirect. Here we
manually create an `ImageIcon` with a raw String naming the image file.  This is
error prone because there is no type-safety connecting the String with the file
on disk -- your build process will not catch typos or file rename related errors:

```java
ImageIcon image = new ImageIcon("abc/widget/images/companyLogo.png");
```

Custom library layers often contribute toward image caching and other services:
```java
import abc.widget.util.ImageUtilties;

ImageIcon image = ImageUtilities.getCachedImage("abc/widget/images/companyLogo.png");
render(image);
```

The image manifold eliminates much of this with direct, type-safe access to image resources.
```java
import abc.widget.images.*;

ImageIcon image = companyLogo_png.get();
render(image);
```

All image resources are accessible as classes where each class has the same name as its image file
including a suffix encoding the image extension, this helps distinguish between images of 
different types sharing a single name.  Additionally image classes are direct subclasses of the familiar
 `ImageIcon` class to conform with existing frameworks. As with all
  type manifolds there are no code gen files or other build steps involved.

### JavaScript

** Warning: Javascript support is experimental and incomplete **

The JavaScript type manifold provides direct, type-safe access to JavaScript files
as if they were Java files.

Here we have JavaScript resource file, `abc/JsProgram.js`:
```javascript
var x = 1;

function nextNumber() {
  return x++;
}

function exampleFunction(x) {
  return x + " from Javascript";
}
```

The type manifold maps type information directly to Java's type system so that
this JavaScript program is accessible as a Java class:

```java
import abc.JsProgram;

...

  String hello = JsProgram.exampleFunction("Hello");
  System.out.println(hello); // prints 'Hello from JavaScript'
  
  double next = JsProgram.nextNumber();
  System.out.println(next); // prints '1'
  next = JsProgram.nextNumber();
  System.out.println(next); // prints '2'
```

In addition to JavaScript programs you can also access JavaScript _classes_ directly.

This JavaScript defines class `Person` from file `abc/Person.js':
```javascript
class Person {
  constructor(firstName, lastName) {
    this._f = firstName
    this._l = lastName
  }

  displayName() {
    return this._f + " " + this._l
  }

  get firstName() {
    return this._f
  }
  set firstName(s) {
    this._f = s
  }

  get lastName() {
    return this._l
  }
  set lastName(s) {
    this._l = s
  }
}
```

You can access this JavaScript class file directly as a Java class:
```java
import abc.Person;

Person person = new Person("Joe", "Bloe");
String first = person.getFirstName();
System.out.println(first);
```

You can also create simple, type-safe Javascript _templates_.  Javascript template files have a `.jst` extension
and work very similar to JSP syntax. To illustrate:

File `com/foo/MyTemplate.jst`:
```
<%@ params(names) %>
This template lists each name provided in the 'names' parameter
Names:
<%for (var i = 0; i < names.length; i++) { %>
-${names[i]}
<% } %>
The end
```
From Java we can use this template in a type-safe manner:
```java
import com.foo.MyTemplate;
...
String reuslts = MyTemplate.renderToString(Arrays.asList("Orax", "Dynatron", "Lazerhawk", "FM-84"));
System.out.println(results);
``` 
This prints the following to the console:
```
This template lists each name provided in the 'names' parameter
Names:
-Orax
-Dynatron
-Lazerhawk
-FM-84
The end
```

For full-featured template engine functionality see project [ManTL](http://manifold.systems/manifold-templates.html).

### Dark Java

Java is a statically compiled language, which generally means a Java program must be compiled to .class files before it
can be executed -- the JVM cannot execute .java files directly.  Although the advantages of static compilation usually
outweigh other means, there are times when it can be a burden.  One example involves targeting multiple runtime 
environments a.k.a. multi-release distributions.  This is basically about releasing software for use with different Java
runtime environments (JREs), like targeting both Java 8 and Java 9.  The main idea is for a product to function in two or
more JREs, and if possible use new features in the latest Java release while still supporting older releases.  Multi-release is notoriously complicated in part 
because Java's static compiler is version-specific -- either you compile with Java 8 or Java 9, conditional compilation is not directly supported.  As a consequence 
many software vendors resort to reflection or deploy separate libraries for different JREs, neither of which is 
desirable.  

Java 9 attempts to mitigate the multi-release dilemma with a new feature: [Multi-release Jar files](http://openjdk.java.net/jeps/238). 
What this feature does, essentially, is let you provide different versions of a .class file per JRE version in a single Jar file; 
the Java 9 classloader knows how to find the proper class.  While this is a welcome improvement, much of the multi-release
burden remains.  The general problem involves tooling -- how are you going to build your product?  Are you using Maven? 
Gradle?  Ant??  What IDE do you use?  Can your IDE and your build tooling work with multiple compilers and target 
multi-release Jars?  Even if your tooling can manage all of this, working with it tends to be overly complicated. Ideally 
a complete multi-release solution would involve source code and nothing else; You should be able to conditionally access 
classes based on the JRE version (or other runtime state) and not be concerned about building separate classes using 
multiple compilers and jar files etc. 

Dark Java provides this capability by avoiding static compilation altogether.  A Dark Java file is the same as a normal 
Java file with the extension, `.darkj`, as the only difference.  The basic properties of a type manifold are what makes
Dark Java work for multi-release:
* You can reference a Dark Java class as a normal Java class directly from any Java file in your project 
* The JRE compiles and loads a Dark Java class dynamically at runtime only if and when it is first used, otherwise it is ignored  

To illustrate, the following example is statically compiled with Java 8.  It can be run with Java 8 or Java 9.  
It demonstrates how you can target these JREs dynamically -- Java 8 with `Foo8` and Java 9 with `Foo9` using an interface 
to abstract their use:

```java
public interface Foo {
  Set<Integer> makeSet();
  
  static Foo create() {
    if(JreUtil.isJava8()) {
      return (Foo)Class.forName("abc.Foo8").newInstance();
    }
    return (Foo)Class.forName("abc.Foo9").newInstance();
  }
}

// File: Foo8.darkj
public class Foo8 implements Foo {
  @Override
  public Set<Integer> makeSet() {
    Set<Integer> set = new HashSet<>();
    set.add(1);
    set.add(2);
    set.add(3);
    return Collections.unmodifiableSet(set);
  }
}

// File: Foo9.darkj
public class Foo9 implements Foo {
  @Override
  public Set<Integer> makeSet() {
    return Set.of(1, 2, 3);
  }
}
```

When run in Java 8 the `Main` class uses `Foo8`, when run in Java 9 or later it uses `Foo9`:

```java
public class Main {
  public static void main( String[] args ) {
    Foo foo = Foo.create();
    Set<Integer> set = foo.makeSet();
  }
}
```

This basic interface factory pattern can be used anywhere you need to use a Dark Java class for any dynamic 
compilation use-case.  
 
 
### Templating

Manifold provides two forms of templating:
* String Templates (or String interpolation)
* Template *Files* with [ManTL](http://manifold.systems/manifold-templates.html)

A **String template** lets you use the `$` character to embed a Java expression directly into a String.  You can 
use `$` to embed a simple variable:
```java
int hour = 8;
String time = "It is $hour o'clock";  // prints "It is 8 o'clock"
```
Or you can embed an expression of any complexity in curly braces:
```java
LocalTime localTime = LocalTime.now();
String ltime = "It is ${localTime.getHour()}:${localTime.getMinute()}"; // prints "It is 8:39"
```

By default String templates are _disabled_.  Enable the feature with the `strings` Manifold plugin argument:
```
-Xplugin:Manifold strings
```  
or
```
-Xplugin:"Manifold strings"  // some tools require quotes)
```
The argument enables templates in any String literal anywhere in your project. 

If you need to turn the feature off in specific areas in your code, you can use the `@DisableStringLiteralTemplates` 
annotation to control its use.  You can annotate a class, method, field, or local variable declaration to turn it on 
or off in that scope:
```java
@DisableStringLiteralTemplates // turns off String templating inside this class
public class MyClass
{
  void foo() {
    int hour = 8;
    String time = "It is $hour o'clock";  // prints "It is $hour o'clock"
  }
  
  @DisableStringLiteralTemplates(false) // turns on String templating inside this method
  void bar() {
    int hour = 8;
    String time = "It is $hour o'clock";  // prints "It is 8 o'clock"
  }
}
```

Finally, you can use the `$` literally and bypass string templates using standard Java character escape syntax (since ver 0.14-alpha):
```java
String verbatim = "It is \$hour o'clock"; // prints "It is $hour o'clock"
```
Or, if you prefer, you can use template syntax:
```java
String verbatim = "It is ${'$'}hour o'clock"; // prints "It is $hour o'clock"
``` 

Template **_files_** are much more powerful and are documented in project [ManTL](http://manifold.systems/manifold-templates.html).


### Build Your Own Manifold

Almost any data source is a potential type manifold.  These include file schemas, query languages, database definitions, 
data services, templates, spreadsheets, programming languages, and more.  So while the Manifold team provides several 
type manifolds out of the box the domain of possible type manifolds is virtually unlimited.  Importantly, their is 
nothing special about the ones we provide -- you can build type manifolds using the same public API with which ours
are built.

The API is comprehensive and aims to fulfill the 80/20 rule -- the common use-cases are a breeze to implement, but the API is 
flexible enough to achieve almost any kind of type manifold.  For instance, since most type manifolds are resource
file based the API foundation classes handle most of the tedium with respect to file management, caching, and modeling.
Also, since the primary responsibility for a type manifold is to dynamically produce Java source, Manifold provides 
a simple API for building and rendering Java classes. But the API is flexible so that you can use other tooling as you
prefer.

Most resource file-based type manifolds consist of three basic classes:
* JavaTypeManifold subclass
* A class to produce Java source 
* Model subclass
 
The Image manifold nicely illustrates this structure:


**JavaTypeManifold Subclass**
```java
public class ImageTypeManifold extends JavaTypeManifold<Model> {
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "png", "bmp", "wbmp", "gif"));

  @Override
  public void init(IModule module) {
    init(module, Model::new);
  }

  @Override
  public boolean handlesFileExtension(String fileExtension) {
    return FILE_EXTENSIONS.contains(fileExtension.toLowerCase());
  }

  @Override
  protected String aliasFqn(String fqn, IFile file) {
    return fqn + '_' + file.getExtension();
  }

  @Override
  protected boolean isInnerType(String topLevel, String relativeInner) {
    return false;
  }

  @Override
  protected String produce(String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler) {
    SrcClass srcClass = new ImageCodeGen(model._url, topLevelFqn).make();
    StringBuilder sb = srcClass.render(new StringBuilder(), 0);
    return sb.toString();
  }
}
```
Like most type manifolds the image manifold is file extension based, specifically it handles the domain of files having 
image extensions: jpg, png, etc.  As you'll see, the `JavaTypeManifold` base class is built to handle this use-case.
First, `ImageTypeManifold` overrides the `init()` method to supply the base class with its `Model`.  We'll cover
that shortly.  Next, it overrides `handlesFileExtension()` to tell the base class which file extensions it handles.
Next, since the image manifold produces classes with a slightly different name than the base file name, it overrides 
`aliasFqn()` to provide an alias for the qualified name of the form "<package>.<image-name>_<ext>".  The name must
match the class name the image manifold produces. There are no inner classes produced by this manifold, therefore
it overrides `isInnerType()` returning false; the base class must ask the subclass to resolve inner types.  Finally,
the image manifold overrides `produce()`, this is where you produce Java source for a specified class name.


**Source Production Class**

Most often, you'll want to create a separate class to handle the production of Java source.  The image manifold does 
that with `ImageCodeGen`:

```java
public class ImageCodeGen {
  private final String _fqn;
  private final String _url;

  ImageCodeGen(String url, String topLevelFqn) {
    _url = url;
    _fqn = topLevelFqn;
  }

  public SrcClass make() {
    String simpleName = ManClassUtil.getShortClassName(_fqn);
    return new SrcClass(_fqn, SrcClass.Kind.Class).imports(URL.class, SourcePosition.class)
      .superClass(new SrcType(ImageIcon.class))
      .addField(new SrcField("INSTANCE", simpleName).modifiers(Modifier.STATIC))
      .addConstructor(new SrcConstructor()
        .addParam(new SrcParameter("url")
          .type(URL.class))
        .modifiers(Modifier.PRIVATE)
        .body(new SrcStatementBlock()
          .addStatement(new SrcRawStatement()
            .rawText("super(url);"))
          .addStatement(new SrcRawStatement()
            .rawText("INSTANCE = this;"))))
      .addMethod(new SrcMethod().modifiers(Modifier.STATIC)
        .name("get")
        .returns(simpleName)
        .body(new SrcStatementBlock()
          .addStatement(
            new SrcRawStatement()
              .rawText("try {")
              .rawText("  return INSTANCE != null ? INSTANCE : new " + simpleName + 
                "(new URL("\\" + ManEscapeUtil.escapeForJavaStringLiteral(_url) + "\\"));")
              .rawText("} catch(Exception e) {")
              .rawText("  throw new RuntimeException(e);")
              .rawText("}"))));
  }
}
```

Here the image manifold utilizes `SrcClass` to build a Java source model of image classes.  `SrcClass` is a
source code production utility in the Manifold API.  It's simple and handles basic code generation use-cases.
Feel free to use other Java source code generation tooling if `SrcClass` does not suit your use-case, because
ultimately you're only job here is to produce a `String` consisting of Java source for your class.


**Model Subclass**

The third and final class the image manifold provides is the `Model` class:

```java
class Model extends AbstractSingleFileModel {
  String _url;

  Model(String fqn, Set<IFile> files) {
    super(fqn, files);
    assignUrl();
  }

  private void assignUrl() {
    try {
      _url = getFile().toURI().toURL().toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getUrl() {
    return _url;
  }

  @Override
  public void updateFile(IFile file) {
    super.updateFile(file);
    assignUrl();
  }
}
```

This class models the image data necessary for `ImageCodeGen` to produce source as a `AbstractSingleFileModel` subclass. 
In this case the model data is simply the URL for the image. Additionally, `Model` overrides `updateFile()` to keep the
URL up to date in environments where it can change, such as in an IDE.


**Registration**

In order to use a type manifold in your project, it must be registered as a service.  Normally, as a type
manifold provider to save users of your manifold from this step you self-register your manifold in your 
META-INF directly like so:
```
src
-- main
---- resources
------ META-INF
-------- services
---------- manifold.api.type.ITypeManifold
```
Following standard Java [ServiceLoader protocol](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html)
you create a text file called `manifold.api.type.ITypeManifold` in the `service` directory under your `META-INF` directory.
The file should contain the fully qualified name of your type manifold class (the one that implements `ITypeManifold`) followed
by a new blank line:
```
com.abc.MyTypeManifold

```

As you can see building a type manifold can be relatively simple. The image manifold illustrates the basic structure of
most file-based manifolds. Of course there's much more to the API. Examine the source code for other manifolds such as the JSON manifold ([manifold-json](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json))
and the JavaScript manifold ([manifold-js](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)).  These
serve as decent reference implementations for wrapping existing code generators and binding to existing languages, 
respectively.


## Extension Classes

Similar to other languages such as 
[C#](https://docs.microsoft.com/en-us/dotnet/csharp/csharp), [Kotlin](https://kotlinlang.org/), and
[Gosu](https://gosu-lang.github.io/), with Manifold you can define methods and other features as logical 
extensions to existing Java classes. This is achieved using _extension classes_. An extension class is a 
normal Java class you define as a container for features you want to apply to another class, normally to 
one you can't modify directly, such as `java.lang.String`:

```java
package extensions.java.lang.String;

import manifold.ext.api.*;

@Extension
public class MyStringExtension {

  public static void print(@This String thiz) {
    System.out.println(thiz);
  }

  @Extension
  public static String lineSeparator() {
    return System.lineSeparator();
  }
}
```

All extension classes must be sub-rooted in the `extensions` package where the remainder of the package
must be the qualified name of the extended class. As the example illustrates, an extension
class on `java.lang.String` must reside directly in a package equal to or ending with `extensions.java.lang.String`. Note this
convention facilitates the extension discovery process and avoids the overhead and ceremony of
alternative means such as annotation processors.

In **Java 9** because a package must reside in a single module, you should prepend your module name to the extension package
name to avoid illegal sharing of packages between modules.  For example, if your module were named `foo.widget` you
should define your extension class as `foo.widget.extensions.java.lang.String`.  In Java 8 all extension classes can be 
directly rooted in the `extensions` package, however it is still best to qualify extension classes with your project
or module name to prevent naming collisions.

Additionally, an extension class must be annotated with `manifold.ext.api.Extension`, which distinguishes
extension classes from other classes that may reside in the same package.

### Extension Method Basics

An extension method must be declared `static` and non-`private`. As the receiver of the call, the first
parameter of an extension _instance_ method must have the same type as the extended class. The
`MyStringExtension` example illustrates this; the first parameter of instance method `print` is
`java.lang.String`. Note the parameter name _thiz_ is conventional, you can use any name you like.
Finally, the receiver parameter must be annotated with `manifold.ext.api.This` to distinguish it from 
regular methods in the class.

That's all there is to it. You can use extensions just like normal methods on the extended class:

```java
String name = "Manifold";
name.print();
```

You can define `static` extension methods too. Since static methods don't have a receiver, the method
itself must be annotated with `manifold.ext.api.Extension`:

```java
@Extension
public static String lineSeparator() {
  return System.lineSeparator();
}
```

Call static extensions just as if they were on the extended class:

```java
String.lineSeparator()
```

### Generics

You can extend generic classes too and define generic methods. This is how Manifold extension libraries
work with collections and other generic classes. For example, here is the `first()` extension method on
`Iterable`:

```java
public static <T> T first(@This Iterable<T> thiz, Predicate<T> predicate) {
  for (T element: thiz) {
    if (predicate.test(element)) {
      return element;
    }
  }
  throw new NoSuchElementException();
}
```

Notice the extension is a generic method with the same type variable designation as the
extended class: `T` from `Iterable<T>`. Since extension methods are static this is how we convey type
variables from the extended class to the extension method. Note type variable names must match the 
extended type's type variables and must be declared in the same order.

To define a generic extension method you append the type variables of the method to the list of the
extended class' type variables. Manifold's `map()` extension illustrates this format:

```java
public static <E, R> Stream<R> map(@This Collection<E> thiz, Function<? super E, R> mapper) {
  return thiz.stream().map(mapper);
}
```

Here `map` is a generic extension method having type variable `R` and conveying `Collection`'s type
variable `E`.

### Static Dispatching

An extension class does not physically alter its extended class; the methods defined in an extension are
not really inserted into the extended class. Instead the Java compiler and Manifold cooperate to make a
call to a static method in an extension look like a call to an instance method on the extended class. As a
consequence extension calls dispatch **statically**.

So unlike a virtual method call an extension call is always made based on the extended type declared in
the extension, not the runtime type of the left hand side of the call. To illustrate:

```java
public class Tree {
}

public class Dogwood extends Tree {
}

public static void bark(@This Tree thiz) {
  println("rough");
}
public static void bark(@This Dogwood thiz) {
  println("ruff");
}

Tree tree = new Dogwood();
tree.bark(); // "rough"
```

At compile-time `tree` is of type `Tree`, therefore it transforms to a static invocation of `bark(Tree)`,
which prints "rough".

Another consequence of static dispatching is that an extension method can receive a call even if the value
of the extended object is `null` at the call site. Manifold extension libraries exploit this feature to
improve readability and null-safety. For example, `CharSequence.isNullOrEmpty()` compares the
receiver's value to null so you don't have to:

```java
public static boolean isNullOrEmpty(@This CharSequence thiz) {
  return thiz == null || thiz.length() == 0;
}

String name = null;
if (name.isNullOrEmpty()) {
  println("empty");
}
```

Here the example doesn't check for null and instead shifts the burden to the extension.

### Accessibility and Scope

An extension method never shadows or overrides a class method; when an extension method's name and
parameters match a class method, the class method always has precedence over the extension. For example:

```java
public class Tree {
  public void kind() {
    println("evergreen");
  }
}

public static void kind(@This Tree thiz) {
  println("binary");
}
```

The extension method never wins, a call to `kind()` always prints "evergreen". Additionally, if at
compile-time `Tree` and the extension conflict as in the example, the compiler warns of the conflict
in the extension class.

An extension method can still _overload_ a class method where the method names are the same, but the 
parameter types are different:

```java
public class Tree {
  public void harvest() {
    println("nuts");
  }
}

public static void harvest(@This Tree thiz, boolean all) {
  println(all ? "wood" : thiz.harvest());
}
```

A call to `tree.harvest(true)` prints "wood".

Since extension method references resolve at compile-time, you can limit the compile-time accessibility
of an extension class simply by limiting the scope of the JAR file containing the extension. For example,
if you're using Maven the scope of an extension matches the dependency relationship you assign in your
pom.xml file. Similarly in module-aware IDEs such as IntelliJ IDEA, an extension's scope is the same as
the module's.

### Annotation Extensions

In addition to adding new methods, extension classes can also add _annotations_ to a class. At present
annotation extensions are limited to the extended _class_; you can't yet add annotations to members of 
the class.

Beware, extensions are limited to a compile-time existence. Therefore, even if an 
annotation has `RUNTIME` retention, it will only be present on the extended class at compile-time. This 
feature is most useful when using annotation processors and you need to annotate classes you otherwise 
can't modify.

Also it's worth pointing out you can make existing interfaces _structural_ using annotation extensions:

```java
package extensions.abc.Widget;
@Extension
@Structural // makes the interface structural
public class MyWidgetExtension {
}
```

This extension effectively changes the `abc.Widget` _nominal_ interface to a _structural_ interface. In the context
of your project classes no longer have to declare they implement it nominally.  

See [Structural Interfaces](#structural_interfaces) later in this guide for fuller coverage of the topic.

### Extension Interfaces

An extension class can add structural interfaces to its extended class.  This feature helps with a 
variety of use-cases.  For example, let's say we have a class `Foo` and interface `Hello`:
  
```java
public final class Foo {
  public String sayHello() {
    return "hello";      
  }
}

@Structural
public interface Hello {
  String sayHello();
}
```

Although `Foo` does not implement `Hello` nominally, it defines the `sayHello()` method that otherwise 
satisfies the interface.  Let's assume we don't control `Foo`'s implementation, but we need it to
implement `Hello` nominally.  We can do that with an extension interface:

```java
@Extension
public class MyFooExtension implements Hello {
}
```

Now the compiler believes `Foo` directly implements `Hello`: 

```java
Hello hello = new Foo();
hello.sayHello();
```
Note `Hello` is structural, so even without the extension interface, instances of `Foo` are still 
compatible with `Hello`. It's less convenient, though, because you have to explicitly cast `Foo` to `Hello` --
a purely structural relationship in Manifold requires a cast. Basically extension interfaces save you
from casting. This not only
improves readability, it also prevents confusion in cases involving type inference where it may not be 
obvious that casting is necessary.

It's worth pointing out you can both add an interface _and_ implement its methods
by extension:
```java
public final class Shy {
}

@Extension
public abstract class MyShyExtension implements Hello {
  public static String sayHello(@This Shy thiz) {
    return "hi";    
  }
}
```
This example extends `Shy` to nominally implement `Hello` _and_ provides the `Hello` implementation. Note
the abstract modifier on the extension class.  This is necessary because it doesn't really implement the
interface, the extended class does.

You can also use extension interfaces to extract interfaces from classes you don't
control.  For example, if you want to provide an immutable view of a collection class
such as `java.util.List`, you could use extension interfaces to extract immutable and
mutable interfaces from the class.  As such your code is better suited to confine
`List` operations on otherwise fully mutable lists.


### Extension Libraries

An extension library is a logical grouping of functionality defined by a set of extension classes.
Manifold includes several extension libraries for commonly used classes, many of which are adapted
from Kotlin extensions.  Each library is available as a separate module or Jar file you can add 
to your project separately depending on its needs.

*   **Collections**

    Defined in module `manifold-collections` this library extends:
    - java.lang.Iterable
    - java.util.Collection
    - java.util.List
    - java.util.stream.Stream

*   **Text**

    Defined in module `manifold-text` this library extends:
    - java.lang.CharSequence
    - java.lang.String

*   **I/O**

    Defined in module `manifold-io` this library extends:
    - java.io.BufferedReader
    - java.io.File
    - java.io.InputStream
    - java.io.OutputStream
    - java.io.Reader
    - java.io.Writer

*   **Web/Json**
 
    Defined in module `manifold-json` this library extends:
    - java.net.URL
    - javax.script.Bindings

### Generating Extension Classes

Sometimes the contents of an extension class reflect metadata from other resources.  In this case rather 
than painstakingly writing such classes by hand it's easier and less error prone to produce them via 
type manifold.  To facilitate this use-case, your type manifold must implement the `IExtensionClassProducer`
interface so that the `ExtensionManifold` can discover information about the classes your type
manifold produces. For the typical use case your type manifold should extend `AbstractExtensionProducer`.

See the `manifold-ext-producer-sample` module for a sample type manifold implementing `IExtensionClassProvider`.


## Structural Interfaces

Java is a _nominally_ typed language -- types are assignable based on the names declared in their
definitions. For example:

```java
public class Foo {
  public void hello() {
    println("hello");
  }
}

public interface Greeting {
  void hello();
}

Greeting foo = new Foo(); // error
```

This does not compile because `Foo` does not explicitly implement `Greeting` by name in its `implements`
clause.

By contrast a _structurally_ typed language has no problem with this example.  Basically, structural typing
requires only that a class implement interface _methods_, there is no need for a class to declare that it
implements an interface.

Although nominal typing is perhaps more sound and easier for both people and machines to digest, in some
circumstances the flexibility of structural typing makes it more suitable. Take the following classes:

```java
public class Rectangle {
  public double getX();
  public double getY();
  ...
}

public class Point {
  public double getX();
  public double getY();
  ...
}

public class Component {
  public int getX();
  public int getY();
  ...
}
```

Let's say we're tasked with sorting instances of these according to location in the coordinate plane, say
as a `Comparator` implementation. Each class defines methods for obtaining X, Y coordinates, but these
classes don't implement a common interface. We don't control the implementation of the classes, so we're
faced with having to write three distinct, yet nearly identical, Comparators.

This is where the flexibility of structural interfaces could really help. If Java supported it, we'd
declare a structural interface with `getX()` and` getY()` methods and write only one `Comparator`:

```java
public interface Coordinate {
  double getX();
  double getY();
}

Comparator<Coordinate> coordSorter = new Comparator<>() {
  public int compare(Coordinate c1, Coordinate c2) {
    int res = c1.Y == c2.Y ? c1.X - c2.X : c2.Y - c1.Y;
    return res < 0 ? -1 : res > 0 ? 1 : 0;
  }
}

List<Point> points = Arrays.asList(new Point(2, 1), new Point(3, 5), new Point(1, 1));
Collections.sort(points, coordSorter); // error
```

Of course Java is not happy with this because because `Point` does not nominally implement `Coordinate`. 

This is where Manifold can help with structural interfaces:

```java
@Structural
public interface Coordinate {
  double getX();
  double getY();
}
```

Adding `@Structural` to `Coordinate` effectively changes it to behave _structurally_ -- Java no longer
requires classes to implement it by name, only its methods must be implemented.

Note a class can still implement a structural interface nominally. Doing so helps both people and tooling 
comprehend your code faster. The general idea is to use an interface structurally when you otherwise can't 
use it nominally or doing so overcomplicates your code.


### Type Assignability and Variance

A type is assignable to a structural interface if it provides compatible versions of all the
methods declared in the interface. The use of the term _compatible_ here instead of _identical_ is
deliberate. The looser term concerns the notion that a structural interface method is variant with respect
to the types in its signature:

```java
@Structural
public interface Capitalizer {
  CharSequence capitalize(String s);
}

public static class MyCapitalizer {
  public String capitalize(CharSequence s) {
    return s.isNullOrEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
```

At first glance it looks like `MyCapitaizer` does not satisfy the structure of `Capitalizer`, neither the
parameter type nor the return type of the method match the interface. After careful inspection, however,
it is clear the methods are call-compatible from the perspective of `Capitalizer`:

```java
Capitalizer cap = (Capitalizer) new MyCapitalizer();
CharSequence properName = cap.capitalize("tigers");
```

`MyCapitalizer`'s method can be called with `Capitalizer`'s `String` parameter because `MyCapitalizer`'s
`CharSequence` parameter is assignable from `String` -- _contravariant_ parameter types support
call-compatibility. Similarly we can accept `MyCapitaizer`'s `String` return type because it is
assignable to `Capitalizer`'s `CharSequence` return type -- _covariant_ return types support
call-compatibility. Therefore, even though their method signatures aren't identical, `MyCapitalizer` is
structurally assignable to `Capitalizer` because it is safe to use in terms of `Capitalizer`'s methods.

Signature variance also supports primitive types.  You may have spotted this in the `Component`
class referenced earlier in the `Coordinate` example where `Component.getX()` returns `int`, not `double`
as declared in `Coordinate.getX()`. Because `int` coerces to `double` with no loss of precision
the method is call-compatible. As a result signature variance holds for primitive types as well as
reference types.

### Implementation by Field

Another example where classes have wiggle room implementing structural interfaces involves property 
getter and setter methods, a.k.a. accessors and mutators. Essentially, a property represents a value you 
can access and/or change. Since a field is basically the same thing a class can implement a getter and/or 
a setter with a field:

```java
@Structural
public interface Name {
  String getName();
  void setName(String name);
}

public class Person {
  public String name;
}

Name person = (Name) new Person();
person.setName("Bubba");
String name = person.getName();
```                                                             

Basically a field implements a property method if its name matches the method's name minus the 
is/get/set prefixes and taking into account field naming conventions. For example, fields `Name`, `name`, 
and `_name` all match the `getName()` property method and are weighted in that order.


### Implementation by Extension

It's possible to implement methods of a structural interface via extension methods.  Looking back at the
`Coordinate` example, consider this class:
```java
public class Vector {
  private double _magnitude;
  private double _direction;
  
  public Vector(double magnitude, double direction) {
    _magnitude = magnitude;
    _direction = direction;
  }
  
  // Does not have X, Y coordinate methods  :(
}
```

In physics a vector and a coordinate are different ways of expressing the same thing; they can be converted 
from one to another.  So it follows the `coordSorter` example can sort `Vector` instances in terms of X, Y 
`Coordinates`... if `Vector` supplied `getX()` and `getY()` methods, which it does not.

What if an extension class supplied the methods?
  
```java
@Extension
public class MyVectorExtension {
  public double getX(@This Vector thiz) {
    return thiz.getMagnitude() * Math.cos(thiz.getDirection()); 
  }
  public double getY(@This Vector thiz) {
    return thiz.getMagnitude() * Math.sin(thiz.getDirection()); 
  }
}
```

Voila! `Vector` now structurally implements `Coordinate` and can be used with `coordSorter`.

Generally _implementation by extension_ is a powerful technique to provide a common API for classes your 
project does not control.

  
### Dynamic Typing with `ICallHandler`

Manifold supports a form of dynamic typing via `manifold.ext.api.ICallHandler`:  

```java
public interface ICallHandler {
  /**
   * A value resulting from #call() indicating the call could not be dispatched.
   */
  Object UNHANDLED = new Object() {
    @Override
    public String toString() {
      return "Unhandled";
    }
  };

  /**
   * Dispatch a call to an interface method.
   *
   * @param iface The extended interface and owner of the method
   * @param name The name of the method
   * @param returnType The return type of the method
   * @param paramTypes The parameter types of the method
   * @param args The arguments from the call site
   * @return The result of the method call or UNHANDLED if the method is not dispatched.  
   *   Null if the method's return type is void.
   */
  Object call(Class iface, String name, Class returnType, Class[] paramTypes, Object[] args);
}
```

A class can implement `ICallHandler` nominally or it can be made to implement it via extension class.
Either way instances of the class can be cast to _any_ structural interface where structural calls
dispatch to `ICallHandler.call()`.  The class' implementation of `call()` can delegate the call any 
way it chooses.

For instance, via class extension Manifold provides `ICallHandler` support for `java.util.Map` so that 
getter and setter calls work directly with values in the map:

```java
Map<String,Object> map = new HashMap<>();
Name person = (Name) map;
person.setName("Manifold");
println(person.getName());
```
 
Because `Map` is a `ICallHandler` instances of it can be cast to any structural interface, such as
`Name` from the earlier example.  The `ICallHandler` implementation transforms get/set property calls
to get/put calls into the map using the name of the property in the method.  Additionally, method calls
can be made on map entries where the entry key matches the name of the method and the value is an instance
of a functional interface matching the signature of the call:

```java
map.put( "run", (Runnable)()-> println("hello") );
Runnable runner = (Runnable) map;
runner.run();
```

This example prints "hello" because `Map.call()` dispatches the call to the "run" entry having a 
`Runnable` functional interface value.

Note the similarity of this functionality on `Map` with _expando_ types in dynamic languages.  The
main difference is that invocations must be made through structural interfaces and not directly on 
the map, otherwise `Map` behaves much like an expando object.

See `manifold.collections.extensions.java.util.Map.MapStructExt.java` for details.

## Type-safe Reflection

Sometimes you have to use Java reflection to access fields, methods, and types that are not directly accessible from
your code. But writing reflection code is not only tedious and error-prone, it also loses type-safety in the process. 
Manifold mitigates these issues with the `@Jailbreak` annotation and the `jailbreak()` extension method.  Use them to
leverage the convenience and type-safety of the Java compiler and let Manifold generate reliable, efficient reflection 
code for you.

### Using `@Jailbreak`

Annotate the type of any variable with `@Jailbreak` to gain type-safe access to private fields, methods, and 
types.

#### Basic Use

```java
@Jailbreak Foo foo = new Foo();
foo.privateMethod();
foo.privateMethod("hey");
foo._privateField = 88;
```
```java
public class Foo {
  private final int _privateField;
  
  public Foo(int value) {
    _privateField = value;
  }
  
  private String privateMethod() {
    return "hi";
  }
  
  private String privateMethod(String param) {
    return param;
  }
}
```

#### Use With Static Members

Since Java does not permit you to annotate the type in a static expression, you must use an instance:

```java
@Jailbreak MyClass myClass = null; // value is insignificant
myClass.staticMethod();
myClass.Static_Field = "hi";
```
```java
public class MyClass {
  private static String Static_Field = "hello";
  
  private static void staticMethod() {
  }
}
```

#### Use With Types and Constructors

Use `@Jailbreak` to access hidden types and constructors:
```java
com.abc. @Jailbreak SecretClass secretClass = 
  new com.abc. @Jailbreak SecretClass("hi");
secretClass._data = "hey";
```
```java
package com.abc;
// not public
class SecretClass {
  private final String _data;

  // not public
  SecretClass(String data){
    _data = data;
  }
}
```

#### Break JPMS Barriers

Access fields, methods, and constructors from packages otherwise prohibited for use in your module by the JPMS:
```java
jdk.internal.misc. @Jailbreak VM vm = null;
String[] args = vm.getRuntimeArguments();
```
        
### Using the `jailbreak()` Extension

Similar to `@Jailbreak` you can call the `jailbreak()` extension method from any expression to gain type-safe access to 
private fields, methods, and types.

```java
Foo foo = new Foo();
foo.jailbreak().privateMethodOnFoo();
```
This method is especially handy when you have a chain of member access expressions and you want to quickly use
inaccessible members:

```java
something.foo().jailbreak().bar.jailbreak().baz = value;
``` 

## The Self Type

The *self type* is a common term used in the language community for the *type of this* and is most useful
in situations where you want the return type of a method in a supertype to have the type of the subtype.  Java does not 
directly support the self type, but it does provide some useful features that can be used as a substitute for it, namely
covariant return types and recursive generic types.  While both of these features are useful they fall short as a self
type alternative in terms of convenience, simplicity, and type-safety.

Manifold's `@Self` annotation provides Java with a direct self type implementation.  Use it on method return types to 
enforce `type of this` where suitable.

### The Basics

A common use-case for the self type involves the *Builder* pattern:

```java
public class VehicleBuilder {
  private int _wheels;
  
  public VehicleBuilder withWheels(int wheels) {
    _wheels = wheels;
    return this; // returns THIS
  }
}
```  

This is fine until we subclass it:

```java
public class AirplaneBuilder extends VehicleBuilder {
  private int _wings;
  
  public AirplaneBuilder withWings(int wings) {
    _wings = wings;
    return this; // returns THIS
  }
}

...

Airplane airplane = new AirplaneBuilder()
  .withWheels(2) // returns VehicleBuilder
  .withWings(1)  // ERROR
```

`withWheels()` returns `VehicleBuilder`, not `AirplaneBuilder`.  This is a classic example where we want to return the 
*type of this*.  This is what the self type accomplishes:

```java
  public @Self VehicleBuilder withWheels(int wheels) {
    _wheels = wheels;
    return this; // returns THIS
  }
```

Now with the return type annotated with `@Self` the example works as desired:

```java
Airplane airplane = new AirplaneBuilder()
  .withWheels(2) // returns AirplaneBuilder
  .withWings(1)  // GOOD!
``` 

Annotate with `manifold.ext.api.Self` to preserve the "type of this" anywhere on or in a method return type.

### Self + Generics

You can also use `@Self` to annotate a _type argument_ of a return type.  A nice example of this involves a 
typical graph or tree structure where the nodes in the structure are homogeneous:

```java
public class Node {
  private List<Node> children;
  
  public List<@Self Node> getChildren() {
      return children;
  }
}

public class MyNode extends Node {
  ...
}
```

Here you can make the component type of `List` the self type so you can use the `getChildren` method type-safely from 
subtypes of node:

```java
MyNode myNode = findMyNode();
List<MyNode> = myNode.getChildren(); // wunderbar! 
```

### Self + Extensions

You can use `@Self` with extension methods too.  Here we make an extension method as a means to conveniently chain 
insertions to `Map` while preserving its concrete type:

```java
public static <K,V> @Self Map<K,V> add(@This Map<K,V> thiz, K key, V value) {
  thiz.put(key, value);
  return thiz;
}

HashMap<String, String> map = new HashMap<>()
  .add("bob", "fishspread")
  .add("alec", "taco")
  .add("miles", "mustard");
```
 
## IDE -- IntelliJ IDEA

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
* Professional template file editor

The IntelliJ plugin provides comprehensive support for Manifold. Use code completion to discover and use type manifolds, extension
methods and structural interfaces. Jump directly from usages of extension methods to their declarations.
Likewise, jump directly from references to data source elements and find usages of them in your code.
Watch your JSON, images, properties, templates, and custom type manifolds come alive as types.
Changes you make are instantly available in your code:

Install the plugin directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Browse repositories</kbd> ➜ search: <kbd>Manifold</kbd>

## Use with other JVM Languages

Manifold is foremost a JVM development tool.  It works straightaway with the Java Language and Java 
compiler.  It's also designed for use within other JVM languages via the _plugin host_ API. You can
implement this API to host Manifold from within another JVM language.

See _manifold.api.host.IManifoldHost_. 

## License

### Open Source
Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

### Commercial
Commercial licenses for this work are available. These replace the above ASL 2.0 and offer 
limited warranties, support, maintenance, and commercial server integrations.

For more information, please visit: http://manifold.systems//licenses

Contact: admin@manifold.systems

## Contributing

To contribute a change to Manifold [open source](https://github.com/manifold-systems/manifold):

* Fork the main manifold repository
* Create a new feature branch based on the `development` branch with a reasonably descriptive name (e.g. `fix_json_specific_thing`)
* Implement your fix and write tests
* Create a pull request for that branch against `development` in the main repository

## Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

## Building

To build:

    mvn clean compile 
    
To execute tests:

    mvn test

## Author

* [Scott McKinney](https://www.linkedin.com/in/scott-mckinney-52295625/) - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*
