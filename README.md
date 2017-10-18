<center>
  <img src="https://i.imgur.com/No1RPUf.png" width=80% height=80%/>
</center>

# Introduction

Manifold is a unique framework that allows developers to dynamically and seamlessly 
extend Java's type system. Building on this core framework Manifold provides features 
to make Java development more appealing and productive.

What does Manifold do for Java programmers?
* **Meta-programming**: Type-safe, direct access to your data. Eliminate code generators. Minimize build time.
* **Extensions**: Add methods to existing Java classes such as `String`, `List`, and `File`. Eliminate boilerplate code involving "Util" and "Manager" libraries.
* **Structural Typing**: Unify disparate APIs. Bridge software components you do not control.

Leveraging these key features Manifold delivers a powerful set of Java extensions including **JSON**
integration, **JavaScript** interop, **Structural typing**, seamless **extension libraries** to Java's
runtime classes, and (coming soon) type-safe access to raw **SQL** and **DDL**.

At a high level each of these features is classified as either a **Type Manifold** or an
**Extension** via the **Extension Manifold**.

### Type Manifolds

Bridging the worlds of information and programming, type manifolds are Java
projections of schematized data sources.  More specifically, a type manifold
transforms a data source into a data _type_ directly accessible in your Java code
without a code generation build step or extra compilation artifacts. In essence with Manifold a data
source **_is_** a data type.

To illustrate, normally you access Java properties resources like this:

```java
Properties myProperties = new Properties();
myProperties.load(getClass().getResourceAsStream("/abc/MyProperties.properties"));
String myMessage = myProperties.getProperty("my.message");
```

As with any resource file a properties file is foreign to Java's type system -- there is no direct,
type-safe access to it. Instead you access it indirectly using boilerplate library code sprinkled
with hard-coded strings.

By contrast, with the Properties type manifold you access a properties file directly as a type:

```java
String myMessage = MyProperties.my.message;
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
*   Manifold Templates (work in progress)
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

<p><img src="images/ext_demo.gif" alt="echo method" width="100%" height="100%"/></p>


There's a lot more to the extension manifold including [structural interfaces](#structural-interfaces), which are
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


## Getting Started

Using Manifold in your Java project is simple:

* Add the Manifold jar[s] to your classpath
* Add '-Xplugin:Manifold' as an argument to Javac

That's all.

Manifold currently works with Java 8.  Support for Java 9 is coming soon.

For the convenience of non-maven users you can directly download Manifold binaries:
* [manifold](http://repo1.maven.org/maven2/systems/manifold/manifold/0.1-alpha/manifold-0.1-alpha.jar):
Core Manifold support, also includes properties and image manifolds
* [manifold-ext](http://repo1.maven.org/maven2/systems/manifold/manifold-ext/0.1-alpha/manifold-ext-0.1-alpha.jar):
Support for structural typing and extensions
* [manifold-json](http://repo1.maven.org/maven2/systems/manifold/manifold-json/0.1-alpha/manifold-json-0.1-alpha.jar):
JSON and JSchema support
* [manifold-js](http://repo1.maven.org/maven2/systems/manifold/manifold-js/0.1-alpha/manifold-js-0.1-alpha.jar):
JavaScript support
* [manifold-collections](http://repo1.maven.org/maven2/systems/manifold/manifold-collections/0.1-alpha/manifold-collections-0.1-alpha.jar):
Collections extensions
* [manifold-io](http://repo1.maven.org/maven2/systems/manifold/manifold-io/0.1-alpha/manifold-io-0.1-alpha.jar):
I/O extensions
* [manifold-text](http://repo1.maven.org/maven2/systems/manifold/manifold-text/0.1-alpha/manifold-text-0.1-alpha.jar):
Text extensions
* [manifold-templates](http://repo1.maven.org/maven2/systems/manifold/manifold-templates/0.1-alpha/manifold-templates-0.1-alpha.jar):
Integrated template support

### Maven

Add manifold artifacts that suit your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument:

```xml
  <dependencies>
    <!--Core Manifold support, includes properties and image manifolds-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Support for structural typing and extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-ext</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--JSON and JSchema support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-json</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--JavaScript support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-js</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Template support-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-templates</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Collections extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-collections</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--I/O extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-io</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--I/O extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-io</artifactId>
      <!--<version>\${project.version}</version>-->
    </dependency>
    
    <!--Text extensions-->
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-text</artifactId>
      <!--<version>\${project.version}</version>-->
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

### Gradle

Add manifold artifacts that suite your project's needs.  The minimum requirements are to 
include the core `manifold` artifact and `tools.jar` and add the `-Xplugin:Manifold`
argument as a Java compiler argument:

```groovy
dependencies {
  // Core Manifold support, includes properties and image manifolds
  compile group: 'systems.manifold', name: 'manifold', version: '0.1-SNAPSHOT'
  
  // Support for structural typing and extensions
  compile group: 'systems.manifold', name: 'manifold-ext', version: '0.1-SNAPSHOT'
    
  // JSON and JSchema support  
  compile group: 'systems.manifold', name: 'manifold-json', version: '0.1-SNAPSHOT'
  
  // JavaScript support
  compile group: 'systems.manifold', name: 'manifold-js', version: '0.1-SNAPSHOT'
  
  // Template support
  compile group: 'systems.manifold', name: 'manifold-templates', version: '0.1-SNAPSHOT'
  
  // Collection extensions
  compile group: 'systems.manifold', name: 'manifold-collections', version: '0.1-SNAPSHOT'
  
  // I/O extensions
  compile group: 'systems.manifold', name: 'manifold-io', version: '0.1-SNAPSHOT'
  
  // Text extensions
  compile group: 'systems.manifold', name: 'manifold-text', version: '0.1-SNAPSHOT'
  
  // tools.jar
  compile files("\${System.getProperty('java.home')}/../lib/tools.jar")
}

compileJava {
  options.compilerArgs += ['-Xplugin:Manifold']
}
```

### IntelliJ

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA.

Install the plugin directly from IntelliJ via: `Settings | Plugins | Browse Repositories | Manifold`


## Contributing

To contribute a change to Manifold:

* Fork the main manifold repository
* Create a new feature branch based on the `development` branch with a reasonably descriptive name (e.g. `fix_json_specific_thing`)
* Implement your fix
* Add a test to `/test/unit_tests.html`.  (It's pretty easy!)
* Create a pull request for that branch against `development` in the main repository

## Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags). 

## Authors

* **Scott McKinney** - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*
</p>

* **Carson Gross** - *Contributor, Manifold Templates author*
* **Kyle Moore** - *Contributor, build system hero*
* **Natalie McKinney** - *Contributor, CSV and FASTA parsers*
* **Luca Boasso** - *Contributor*

See also the list of [contributors](https://github.com/manifold-systems/manifold/graphs/contributors) who participated in this project.

## License

The open source portion of this project is licensed under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) - see [our website](http://manifold.systems) for details

## Acknowledgments

* Much of the inspiration for Manifold came from the [Gosu language](https://gosu-lang.github.io/), namely its _Open Type System_
* Many thanks to Carson Gross for getting the Manifold Systems website off the ground
* Shout out to [Lazerhawk](https://lazerhawk.bandcamp.com/album/redline) for world class coding music

## Website

Visit the [Manifold](http://manifold.systems) website to learn more about manifold.

[![Join the chat at https://gitter.im/manifold-io/Lobby](https://badges.gitter.im/manifold-io/Lobby.svg)](https://gitter.im/manifold-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)