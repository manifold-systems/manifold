---
layout: default
---

## Table of Contents
* [Overview](#overview)
* [Type-safe Metaprogramming](#type-safe-metaprogramming-via-_type-manifolds_)
* [Java Extensions via the _Extension_ Manifold](#java-extensions-via-the-_extension_-manifold)
* [Benefits](#benefits)
* [Projects](#projects)
* [IDE Support](#ide-support)
* [**Setup**](#setup)
* [Download](#download)
* [License](#license)
* [Author](#author)
* (_New!_) [Forum](#forum)

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
provides extension methods (like [C#](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/classes-and-structs/extension-methods)),
[operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading),
[unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
structural typing (like [TypeScript](https://www.typescriptlang.org/docs/handbook/interfaces.html)),
string interpolation (like [Kotlin](https://kotlinlang.org/docs/reference/basic-types.html#string-templates)),
type-safe reflection (via [`@Jailbreak`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#type-safe-reflection-via-jailbreak)),
and a lot more.


# Type-safe Metaprogramming via _Type Manifolds_ 

Bridging the worlds of data and code, a *type manifold* acts as an adapter to automatically connect data resources to
Java's type system.  The core Manifold framework seamlessly plugs into the Java compiler enabling a type manifold to
transform structured data into a data _type_ directly accessible in your Java code eliminating code generation build
steps otherwise required with conventional tools. Additionally the [Manifold plugin for IntelliJ IDEA](#ide-support)
provides comprehensive integration for type manifolds. Types are always in sync; changes you make to resources are
immediately available in the type system _without a compilation step_.  Code completion, navigation, usage searching,
refactoring, incremental compilation, hotswap debugging -- all seamlessly integrated.  With type manifolds a data file
is a virtual data _type_.

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
*   [Properties files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
*   [Image files](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
*   [Dark Java](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
*   [JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)
*   [Java Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)

# Java Extensions via the _Extension_ Manifold

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
*   **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** support -- all manifold types and extensions work with IntelliJ

Manifold is just a dependency you can drop into your existing project -- you can begin using it incrementally without
having to rewrite classes or conform to a new way of doing things.

# IDE Support

Use the [Manifold IntelliJ IDEA plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to experience Manifold to its fullest.

The plugin currently supports most high-level IntelliJ features including:
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
* Professional template file editor

The IntelliJ plugin provides comprehensive support for Manifold. Use code completion to discover and use type manifolds,
extension methods and structural interfaces. Jump directly from usages of extension methods to their declarations.
Likewise, jump directly from references to resource elements and find usages of them in your code. Watch your
JSON/YAML/XML/CSV, images, properties, templates, and custom type manifolds come alive as types. Changes you make are
instantly available in your code.

Install the plugin directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

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
* [Manifold : _Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)
* [Manifold : _Image_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)
* [Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)
* [Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)

### Java Extension Manifold
* [Manifold : _Java Extension_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)

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

### Manifold "Fat" Jar
[Manifold : _All&nbsp;&nbsp;(Über jar)_](https://github.com/manifold-systems/manifold/tree/master/manifold-all)

### Sample Projects
* [Manifold sample project](https://github.com/manifold-systems/manifold-sample-project)
* [Manifold sample GraphQL project](https://github.com/manifold-systems/manifold-sample-graphql-app)
* [Manifold sample REST API project](https://github.com/manifold-systems/manifold-sample-rest-api)
* [Manifold sample Web App project](https://github.com/manifold-systems/manifold-sample-web-app)
* [Manifold sample Gradle Project](https://github.com/manifold-systems/manifold-simple-gradle-project)

# Setup

Manifold is designed to work with most build systems, including Maven and Gradle.

The Manifold root project consists of several sub-projects you can include separately in your build, see [Projects](#projects)
above for a complete listing. You can also integrate all Manifold dependencies into your build using the "Fat" Jar
dependency, [`manifold-all`](https://github.com/manifold-systems/manifold/tree/master/manifold-all).

Setup instructions are consistent for each sub-project/dependency.  Here are direct links:

* Setup for [Manifold : _All_](https://github.com/manifold-systems/manifold/tree/master/manifold-all#setup)

<hr/>

* Setup for [Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#setup)

<hr/>

* Setup for [Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql#setup)
* Setup for [Manifold : _XML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml#setup)
* Setup for [Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json#setup)
* Setup for [Manifold : _CSV_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv#setup)
* Setup for [Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml#setup)
* Setup for [Manifold : _Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties#setup)
* Setup for [Manifold : _Image_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image#setup)
* Setup for [Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj#setup)
* Setup for [Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js#setup)

<hr/>

* Setup for [Manifold : _Java Extension_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#setup)

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

>**WARNING** If you plan to build your project **without** Maven or Gradle using select binaries (as opposed to manifold-all), 
your classpath **must** include the **transitive closure** of binaries in terms of the **dependencies** declared in corresponding
project's POM file. Additionally, you will need to adapt your build to reflect the Maven or Gradle setup instructions
from the list above.
>
>For instance, to use the *manifold-preprocessor* jar using **Ant** your project needs:
>* [manifold-preprocessor-2020.1.12.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-preprocessor&v=RELEASE)
>* [manifold-2020.1.12.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold&v=RELEASE)
>* [manifold-util-2020.1.12.jar](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-util&v=RELEASE)
>* [antlr-runtime-4.7.jar](https://www.antlr.org/download/antlr-runtime-4.7.jar)  
>
>As such your *javac* command line should include:
>```text
>javac -Xplugin:Manifold -classpath <jar-path>/manifold-preprocessor-2020.1.12.jar;<jar-path>/manifold-2020.1.12.jar;<jar-path>/manifold-util-2020.1.12.jar;<jar-path>/antlr-runtime-4.7.jar
>```

* Download [manifold : _All_](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=systems.manifold&a=manifold-all&v=RELEASE):

<hr/>

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
  * [Antlr : Runtime](https://www.antlr.org/download/antlr-runtime-4.7.jar) _Note the antlr runtime library is a dependency of manifold-util_

# License

## Open Source
Manifold is licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

## Commercial
Commercial licenses for this work are available. These replace the above ASL 2.0 and offer limited warranties, support,
maintenance, and commercial server integrations.

For more information contact: [admin@manifold.systems](mailto:admin@manifold.systems)

# Author

* [Scott McKinney](mailto:scott@manifold.systems)

# Forum
Join our [Slack Group](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg) to start
a discussion, ask questions, provide feedback, etc. Someone is usually there to help.
