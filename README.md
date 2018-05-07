<center>
  <img src="https://i.imgur.com/No1RPUf.png" width=80% height=80%/>
</center>

# Introduction

At its core [Manifold](https://manifold.systems/) is a unique framework to dynamically and _seamlessly_ extend
Java. Building on this core framework Manifold supplements Java with new features you can use in your applications:

* **Type-safe Metaprogramming** -- renders code generators obsolete, similar in concept to [F# _type providers_](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/information-rich-themes-v4.pdf)
* **Extension Methods** -- add methods to classes you don't own, comparable to the same feature in [C#](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/classes-and-structs/extension-methods) and [Kotlin](https://kotlinlang.org/docs/reference/extensions.html)
* **Structural Typing** -- type-safe duck typing, much like interfaces in [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html) and [Go](https://tour.golang.org/methods/10)

Leveraging these key features Manifold delivers a set of high-level components you can plug into your project, these
include:
* **JSON** and **JSON Schema** integration
* **JavaScript** interop
* Type-safe **Templating** 
* **Structural interfaces** and **Expando** objects
* **Extension libraries** for collections, I/O, and text
* **SQL** and **DDL** interop (coming soon)
* Lots more

At a high level each of these features is classified as either a **Type Manifold** or an
**Extension** via the **Extension Manifold**.

### Type Manifolds

Bridging the worlds of information and programming, type manifolds are Java
projections of schematized data sources.  More specifically, a type manifold
transforms a data source into a data _type_ directly accessible in your Java code
without a code generation build step or extra compilation artifacts. In essence with Manifold a data
source **_is_** a data type.

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

Concise and type-safe, with no generated files or other build steps to engage.

Almost any type of data source imaginable is a potential type manifold. These
include resource files, schemas, query languages, database definitions, templates,
spreadsheets, web services, and programming languages.

Currently Manifold provides type manifolds for:

*   JSON and [JSON Schema](http://json-schema.org/)
*   JavaScript
*   Properties files
*   Image files
*   Dark Java
*   ManTL (Manifold Template Language)
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

*   **Zero turnaround** -- live, type-safe access to data; make, discover, and use changes instantly
*   **Lightweight** -- direct integration with standard Java, requires no special compilers, annotation
processors, or runtime agents
*   **Efficient, dynamic** -- Manifold only produces types as they are needed
*   **Simple, open API** -- you can build your own Manifolds
*   **No code generation build step** -- no generated files, no special compilers
*   **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** support -- all manifold types and extensions work with IntelliJ

Additionally, Manifold is just a JAR file you can drop into your existing project -- you can begin using
it incrementally without having to rewrite classes or conform to a new way of doing things.


## Setup

### Basics

Using Manifold in your Java project is simple:

* Add the Manifold jar[s] to your classpath (and tools.jar if you're using Java 8)
* Add `-Xplugin:Manifold` as an argument to java**c** (for compilation only)

That's all.

Manifold fully supports both [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Java 9](http://www.oracle.com/technetwork/java/javase/downloads/jdk9-downloads-3848520.html).

**Java 9 Notes**

If you are using **Java 9** with `module-info` files you must declare dependencies to the manifold jars you are using.  For example, if you are using `manifold-all.jar`:
```java
module your.module.name {
  requires manifold.all;    // the manifold-all jar file
  requires java.scripting;  // if using Json manifold: for javax.script.Bindings
  requires java.desktop;    // if using Image manifold: for javax.swing.ImageIcon
}
```
Additionally **Java 9** modular projects must include the processor path for the manifold jar file along with the `-Xplugin:Manifold` argument to javac:
```
javac -Xplugin:Manifold -processorpath /path/to/your/manifold-all.jar ...
```

**Java 8 Notes**

If you are using **Java 8** you may need to include `tools.jar` in your classpath (runtime only).
Your application requires tools.jar if you are using Manifold in *dynamic* mode, as opposed to 
*static* mode. See [Modes](#Modes) for details.


### Modes

You can use Manifold in one of two ways: **dynamically** or **statically**.  The mode 
determines whether or not Manifold compiles class projections to disk at compile-time, and in 
turn whether or not Manifold dynamically compiles and loads the classes at runtime.  The mode is
controlled using the `-Xplugin` javac argument:

**Dynamic**: `-Xplugin:Manifold` (default, compiles class projections dynamically at runtime)

**Static**: `-Xplugin:Manifold static` (compiles class projections statically at compile-time)
(alternatively `-Xplugin:"Manifold static"`, some tools may require quotes)

The mode you use largely depends on your use-case and personal preference. As a general rule
dynamic mode is usually better for development and static mode is usually better for production, 
however you can use either mode in any situation you like. Things to consider:

* Both modes operate _lazily_ -- regardless of mode, a class projection is not compiled unless it is used.
For example, if you are using the [Json manifold](#json-and-json-schema), only the Json files you reference 
in your code will be processed and compiled.

* Even if you use static mode, you can still reference type manifold classes dynamically e.g., _reflectively_.
In such a case Manifold will dynamically compile the referenced class as if you were operating in 
dynamic mode.  In general, your code will work regardless of the mode you're using; Manifold will
figure out what needs to be done. 

* Dynamic mode requires `tools.jar` at runtime for Java 8.  Note tools.jar may still be required with 
static mode, depending on the Manifold features you use.  For example, [structural interfaces](#structural-interfaces)
requires tools.jar, regardless of mode.  The Json manifold models both sample Json files and [Json Schema](http://json-schema.org/) 
files as structural interfaces.

* Static mode is generally faster at runtime since it pre-compiles all the type manifold projection when you 
build your project

* Static mode automatically supports incremental compilation of class projections in IntelliJ (coming in version 0.10-alpha)
   

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
??ber-jar containing all of the binaries below (recommended)
* [manifold](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE):
Core Manifold support, also includes properties and image manifolds
* [manifold-ext](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-ext&v=RELEASE):
Support for structural typing and extensions
* [manifold-json](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-json&v=RELEASE):
JSON and JSchema support
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


### Maven

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument.  Note you can use the `manifold-all` dependency
to use all basic manifold features, this is the recommended setup.

**Settings**

```xml
  <dependencies>
    <!--Includes all basic dependencies (recommended) -->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-all</artifactId>
      <version>RELEASE</version>
    </dependency>

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
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <compilerArgs>
            <arg>-Xplugin:Manifold</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <profiles>
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

For Java 8, executing tests of classes leveraging Manifold will also require `tools.jar` at test execution time.

Here is a simple project layout demonstrating use of the `manifold-all` dependency and including `tools.jar` with Surefire:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.7.0</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <compilerArgs>
            <arg>-Xplugin:Manifold</arg>
          </compilerArgs>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.20.1</version>
        <configuration>
          <additionalClasspathElements>
            <additionalClasspathElement>${java.home}/../lib/tools.jar</additionalClasspathElement>
          </additionalClasspathElements>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-all</artifactId>
      <version>RELEASE</version> <!-- there were known issues with manifold-all 0.9-alpha and earlier -->
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.12</version>
    </dependency>
  </dependencies>
```

Note the above snippet should work with `manifold-all` release `0.10-alpha` and beyond.

**Archetype**

A Maven archetype facilitates new project creation.  Use the Manifold [archetype](https://github.com/manifold-systems/archetype) to quickly
create a new Manifold project.  This is an easy process from IntelliJ:

<p><img src="/images/archetype.png" alt="echo method" width="60%" height="60%"/></p>


### Gradle

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument:

```groovy
apply plugin: 'java'

dependencies {
  // All manifold, includes all other dependencies listed here
  compile group: 'systems.manifold', name: 'manifold-all', version: 'RELASE'

  // Core Manifold support, includes properties and image manifolds
  compile group: 'systems.manifold', name: 'manifold', version: 'RELASE'
  
  // Support for structural typing and extensions
  compile group: 'systems.manifold', name: 'manifold-ext', version: 'RELASE'
    
  // JSON and JSchema support  
  compile group: 'systems.manifold', name: 'manifold-json', version: 'RELASE'
  
  // JavaScript support
  compile group: 'systems.manifold', name: 'manifold-js', version: 'RELASE'
  
  // Template support
  compile group: 'systems.manifold', name: 'manifold-templates', version: 'RELASE'
  
  // Collection extensions
  compile group: 'systems.manifold', name: 'manifold-collections', version: 'RELASE'
  
  // I/O extensions
  compile group: 'systems.manifold', name: 'manifold-io', version: 'RELASE'
  
  // Text extensions
  compile group: 'systems.manifold', name: 'manifold-text', version: 'RELASE'
  
  // tools.jar
  compile files("\${System.getProperty('java.home')}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
  options.compilerArgs += '-Xplugin:Manifold'
  options.fork = true
}
```

### IntelliJ

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA.

Install the plugin directly from IntelliJ via: `Settings | Plugins | Browse Repositories | Manifold`


## Contributing

To contribute a change to Manifold:

* Fork the main manifold repository
* Create a new feature branch based on the `development` branch with a reasonably descriptive name (e.g. `fix_json_specific_thing`)
* Implement your fix and write tests
* Create a pull request for that branch against `development` in the main repository

## Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags). 

## Building

To execute tests:

    mvn test

To change the version number:

    mvn -B release:update-versions -DdevelopmentVersion=0.x-SNAPSHOT

## Authors

* **Scott McKinney** - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*
</p>

* **Carson Gross** - *Contributor, ManTL (Manifold Template Language)*
* **Kyle Moore** - *Contributor, build system hero*
* **Natalie McKinney** - *Contributor, CSV and FASTA parsers*
* **Luca Boasso** - *Contributor*

See also the list of [contributors](https://github.com/manifold-systems/manifold/graphs/contributors) who participated in this project.

## License

The open source portion of this project is licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) - see [our website](http://manifold.systems) for details

## Acknowledgments

* Much of the inspiration for Manifold came from the [Gosu language](https://gosu-lang.github.io/), namely its _Open Type System_
* Many thanks to Carson Gross for getting the Manifold Systems website off the ground
* Shout out to [Lazerhawk](https://lazerhawk.bandcamp.com/album/redline) for world class coding music

## Website

Visit the [Manifold](http://manifold.systems) website to learn more about manifold.

[![Join the chat at https://gitter.im/manifold-io/Lobby](https://badges.gitter.im/manifold-io/Lobby.svg)](https://gitter.im/manifold-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)