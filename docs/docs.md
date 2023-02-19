---
layout: default
---

## Table of Contents
* [Overview](#overview)
* [Type-safe Metaprogramming (GraphQL, JSON, XML, etc.)](#type-safe-metaprogramming-with-type-manifolds)
* [Java Extensions with the _Extension_ Manifold](#the-extension-manifold)
* [Benefits](#benefits)
* [IDE Support](#ide-support)
* [Projects](#projects)
* [Platforms](#platforms)  
* [**Setup**](#setup)
* [Download](#download)
* [License](#license)
* [Author](#author)
* [Forum](#forum)

# Overview

[Manifold](https://manifold.systems/) plugs directly into Java to supplement it with powerful features you can use
directly in your projects:

* [**Type-safe Metaprogramming**](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold) -- _type-safe_ access to structured data.
Use [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[XML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml), 
[JSON](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json), 
[YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml), 
[CSV](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv), 
[JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js),
[Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates), etc.
directly and type-safely from Java without a code generator in your build and with comprehensive IDE support. 
* [**Java Extensions**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext) --
provides [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension),
[properties](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props),
[operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading),
[unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
[structural typing](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural),
[string interpolation](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings),
[type-safe reflection](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#type-safe-reflection-via-jailbreak),
and a lot more.


# Type-safe Metaprogramming with Type Manifolds

Bridging the worlds of data and code, a *type manifold* acts as an adapter to automatically connect data resources to
Java's type system.  The core Manifold framework seamlessly plugs into the Java compiler enabling a type manifold to
transform structured data into data _types_ directly accessible in your Java code eliminating code generation build
steps otherwise required with conventional tools. Additionally, the [Manifold plugin](#ide-support)
provides comprehensive integration for type manifolds in both [IntelliJ IDEA](https://www.jetbrains.com/idea/download)
and [Android Studio](https://developer.android.com/studio). Types are always in sync; changes you make to resources are
immediately available in your code _without a compilation step_.  Code completion, navigation, usage searching,
deterministic refactoring, incremental compilation, hotswap debugging -- all seamlessly integrated.  With type manifolds
a data resource is a virtual data _type_.

To illustrate, consider this simple properties resource file:

`/abc/MyProperties.properties`
```properties
chocolate = Chocolate
chocolate.milk = Milk chocolate
chocolate.dark = Dark chocolate
``` 

Normally in Java you access a properties file like this:

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

Any data resource is a potential type manifold, including file schemas, query languages, database definitions, 
data services, templates, spreadsheets, and programming languages.

Manifold provides type manifolds for:

*   [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
*   [JSON and JSON Schema](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
*   [XML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml)
*   [YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)
*   [CSV](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv)
*   [Property files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
*   [Image files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
*   [Dark Java](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
*   [JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)
*   [Java Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)

# The Extension Manifold

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
maintainable code. Additionally, with the Manifold IntelliJ plugin you can use code-completion which conveniently
presents all the extension methods available on an extended class:

<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/ExtensionMethod.mp4">
  </video>
</p>

There's a lot more to the extension manifold including [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading),
[unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
[structural interfaces](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural),
which are similar to interfaces in the [Go](https://golang.org/) and [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html)
languages. See the [Java Extension Manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
for full coverage of these features.

> _**New!**_
> * Finally, [_**Properties**_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props) for Java
> * Includes automatic property _inference_ for existing Java classes  **😎**
>
> [Learn more](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props).


# Benefits

Manifold's core technology is a dramatic departure from conventional Java tooling. There are no code generation steps in
the build, no extra build target files to manage, no annotation processors, and no extra class loaders to engage at
runtime.

Benefits of this approach include:

*   **Zero turnaround** -- direct, type-safe access to structured data
*   **Lightweight** -- requires no special compilers, annotation processors, or runtime agents
*   **Efficient** -- Manifold only produces types as they are needed
*   **Simple, open API** -- use the Manifold API to build your own components and extensions
*   **No code generation build step** -- integrates directly with the Java compiler
*   **Incremental** -- only builds types that have changed
*   **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** -- fully supported
*   **[Android Studio](https://developer.android.com/studio)** -- fully supported

Manifold is just a dependency you can drop into your existing project. You can begin using it incrementally without
having to rewrite classes or conform to a new way of doing things.

# IDE Support

Use Manifold to its fullest in **IntelliJ IDEA** and **Android Studio** using the [Manifold IDE plugin](https://plugins.jetbrains.com/plugin/10057-manifold).

The plugin provides comprehensive support for IDE features including:
* Feature highlighting
* Error reporting
* Code completion
* Go to declaration
* Usage searching
* Rename/Move refactoring
* Quick navigation
* Operator overloading
* Unit expressions
* Structural typing
* Type-safe reflection with `@Jailbreak`
* Self type support with `@Self`
* Incremental compilation
* Hotswap debugging
* Preprocessor (conditional compilation)
* Professional template file editing

Use code completion to discover and use type manifolds such as GraphQL and to access extension methods and structural
interfaces. Jump directly from usages of extension methods to their declarations. Jump directly from call sites to
resource elements and find usages of them in your code. Watch your GraphQL, JSON, XML, YAML, CSV, images, properties,
templates, and custom type manifolds come alive as types. Changes you make in resource files are instantly available in
your code, _without compiling_.

Install the plugin directly from the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

Get the plugin from JetBrains Marketplace:

<iframe frameborder="none" width="245px" height="48px" src="https://plugins.jetbrains.com/embeddable/install/10057">
</iframe>

# Projects

The Manifold framework consists of the *core project* and a collection of *sub-projects* implementing SPIs provided
by the core. Each project represents a separate *dependency* you can use directly in your project. See details in each
projects' docs.

### Core Framework
* [Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)

### Resource Manifolds
* [Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
* [Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
* [Manifold : _XML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml)
* [Manifold : _CSV_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv)
* [Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)
* [Manifold : _Property files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
* [Manifold : _Image files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
* [Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
* [Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)

### Java Extension Manifold
* [Manifold : _Java Extension_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)

### Java Properties
* [Manifold : _Java Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)

### Java Templates Framework
* [Manifold : _Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)

### Java Compiler Extensions
* [Manifold : _String Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings) <small>(string interpolation)</small>
* [Manifold : _[Un]checked_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)

### Java Preprocessor
* [Manifold : _Preprocessor_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)

### Java Science
* [Manifold : _Science_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)

### Java Extension Libraries 
* [Manifold : _Collections_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
* [Manifold : _I/0_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-io)
* [Manifold : _Text_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-text)

### Sample Projects
* [Manifold sample project](https://github.com/manifold-systems/manifold-sample-project)
* [Manifold sample GraphQL App](https://github.com/manifold-systems/manifold-sample-graphql-app)
* [Manifold sample REST API App](https://github.com/manifold-systems/manifold-sample-rest-api)
* [Manifold sample Web App](https://github.com/manifold-systems/manifold-sample-web-app)
* [Manifold sample Gradle Project](https://github.com/manifold-systems/manifold-simple-gradle-project)
* [Manifold sample Kotlin App](https://github.com/manifold-systems/manifold-sample-kotlin-app)

# Platforms

Manifold supports:
* Java SE (8 - 19)
* [Android](http://manifold.systems/android.html)
* [Kotlin](http://manifold.systems/kotlin.html) (limited)

Comprehensive IDE support is also available for IntelliJ IDEA and Android Studio.

# Setup

Manifold is designed to work with most build systems, including Maven and Gradle.

The Manifold root project consists of several sub-projects you can include separately in your build, see [Projects](#projects)
above for a complete listing. If you're targeting Android using Android Studio, see [Using Manifold with Android Studio](http://manifold.systems/android.html)
for additional setup information.

Setup instructions are consistent for each sub-project/dependency.  Here are direct links:

* Setup for [Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#setup)

<hr/>

* Setup for [Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql#setup)
* Setup for [Manifold : _XML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml#setup)
* Setup for [Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json#setup)
* Setup for [Manifold : _CSV_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv#setup)
* Setup for [Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml#setup)
* Setup for [Manifold : _Property Files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties#setup)
* Setup for [Manifold : _Image_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image#setup)
* Setup for [Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj#setup)
* Setup for [Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js#setup)

<hr/>

* Setup for [Manifold : _Java Extension_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#setup)

<hr/>

* Setup for [Manifold : _Java Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props#setup)

<hr/>

* Setup for [Manifold : _Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates#setup)

<hr/>

* Setup for [Manifold : _String Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings#setup)
* Setup for [Manifold : _[Un]checked_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions#setup)

<hr/>

* Setup for [Manifold : _Preprocessor_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#setup)

<hr/>

* Setup for [Manifold : _Science_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science#setup)

<hr/>
 
* Setup for [Manifold : _Collections_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#setup)
* Setup for [Manifold : _I/0_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-io#setup)
* Setup for [Manifold : _Text_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-text#setup)


# Download

For the convenience of non-maven/non-gradle users you can directly download latest release binaries below. 

>**WARNING** If you plan to build your project **without** Maven or Gradle using select binaries, your classpath
>**must** include the **transitive closure** of binaries in terms of the **dependencies** declared in corresponding
>project's POM file. Additionally, you will need to adapt your build to reflect the Maven or Gradle setup instructions
>from the list above.
>
>For instance, to use the *manifold-preprocessor* jar using **Ant** your project needs:
>* [manifold-preprocessor-2023.1.3.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-preprocessor&v=RELEASE)
>* [manifold-2023.1.3.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE)
>* [manifold-util-2023.1.3.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-util&v=RELEASE)
>
>As such your *javac* command line should include:
>```text
>javac -Xplugin:Manifold -classpath <jar-path>/manifold-preprocessor-2023.1.3.jar;<jar-path>/manifold-2023.1.3.jar;<jar-path>/manifold-util-2023.1.3.jar
>```

* Download [Manifold : _Core_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE)

<hr/>

* Download [Manifold : _GraphQL_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-graphql&v=RELEASE)
* Download [Manifold : _XML_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-xml&v=RELEASE)
* Download [Manifold : _JSON_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-json&v=RELEASE)
* Download [Manifold : _CSV_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-csv&v=RELEASE)
* Download [Manifold : _YAML_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-yaml&v=RELEASE)
* Download [Manifold : _Properties_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-properties&v=RELEASE)
* Download [Manifold : _Image_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-image&v=RELEASE)
* Download [Manifold : _Dark Java_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-darkj&v=RELEASE)
* Download [Manifold : _JavaScript_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-js&v=RELEASE)

<hr/>

* Download [Manifold : _Java Extension_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-ext&v=RELEASE)

<hr/>

* Download [Manifold : _Java Properties_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-props&v=RELEASE)

<hr/>

* Download [Manifold : _Templates_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-templates&v=RELEASE)

<hr/>

* Download [Manifold : _String Templates_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-strings&v=RELEASE)
* Download [Manifold : _[Un]checked_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-exceptions&v=RELEASE)

<hr/>

* Download [Manifold : _Preprocessor_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-preprocessor&v=RELEASE)

<hr/>

* Download [Manifold : _Science_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-science&v=RELEASE)

<hr/>
 
* Download [Manifold : _Collections_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-collections&v=RELEASE)
* Download [Manifold : _I/0_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-io&v=RELEASE)
* Download [Manifold : _Text_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-text&v=RELEASE)

<hr/>
 
* Download [Manifold : Util](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-util&v=RELEASE)

# License

Manifold is open source and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Author

* [Scott McKinney](mailto:scott@manifold.systems)

# Forum
Join our [Slack Group](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg) to start
a discussion, ask questions, provide feedback, etc. Someone is usually there to help.
