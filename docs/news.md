---
layout: default
---

# News
<br/><br/>

## New article at Jaxenter (16 August 2019):

<table>
 <tr>
  <td>
  <p><a href="https://jaxenter.com/manifold-say-goodbye-to-checked-exceptions-161065.html"><img width="240" height="135" src="/images/shutterstock_1470545906.jpg" alt="a"></a></p>
  </td>
  <td>
  <p><a href="https://jaxenter.com/manifold-say-goodbye-to-checked-exceptions-161065.html">Say Goodbye to Checked Exceptions</a></p>
  <p>Modern languages don’t do checked exceptions. But you don’t have to jump ship to share the experience. In this
  article Scott McKinney shows you how to stick with Java and completely neutralize checked exceptions with a simple
  new addition to the <a href="http://manifold.systems/">Manifold framework</a>.</p>
  <p><small>jaxenter.com</small></p>
  </td>
 </tr>
</table>
<br/><br/>



## Manifold 2019.1.11 released (12 August 2019)

Some API and structural cleanup and documentation improvements 
* Refined project readme.md files, standardized them, and overall shifted documentation focus more toward Manifold’s
individual projects and away from the current monolithic docs 
* JPMS related changes: 
  * All manifold projects are defined as "automatic" modules now, with explicit names via "Automatic-Module-Name"
  * API changes 
    * renamed 'manifold' package 'manifold.util' to 'manifold.api.util' 
    * renamed 'manifold.ext' package 'manifold.util' to 'manifold.ext.api' 
* Removed 'bootstrap' plugin
* Manifold JavacPlugin is now a self registered Plugin service
* DEPRECATED: '-Xplugin:Manifold' arguments: 'strings' and 'exceptions' 
  * replaced with new dependencies: 'manifold-strings' and 'manifold.exceptions' 
  * renamed package 'manifold.api.templ' to 'manifold.strings.api' 
* Sample application improvements
  * where applicable restructure projects to use only the manifold component frameworks needed (instead of 'manifold-all')

Bug fixes: 
* Fix incremental compilation 
  * fixes problem where types corresponding with resource files appeared to recompile (or not) randomly in IJ during an incremental build 
  * fixes problem where manifold fragments would also appear to randomly recompile (or not) during an incremental build 
* Fix -processorpath v. --processor-module-path involving JPMS named module(s) in a project 
* Fix preprocessor issue where a Java file would not display preprocessor directives correctly in the presence of manifold fragments 
* Fix preprocessor JAVA_N_OR_LATER behavior
 
Manifold version 2019.1.11 is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/2019.1.11/jar).
<br/><br/>



## New article at Jaxenter (5 August 2019):

<table>
 <tr>
  <td>
  <p><a href="https://jaxenter.com/manifold-preprocessor-for-java-160712.html"><img width="240" height="135" src="/images/shutterstock_1470545906.jpg" alt="a"></a></p>
  </td>
  <td>
  <p><a href="https://jaxenter.com/type-safe-jailbreak-manifold-159177.html">A Preprocessor for Java</a></p>
  <p>Discover how to build multiple targets from a single Java codebase using the new preprocessor from the Manifold
     project. In this article Scott McKinney explains how the preprocessor plugs directly into Java’s compiler to
     provide seamless conditional compilation using familiar directives.</p>
  <p><small>jaxenter.com</small></p>
  </td>
 </tr>
</table>
<br/><br/>
 


## Neuer Artikel bei Jaxenter (5. August 2019):

<table>
 <tr>
  <td>
  <p><a href="https://jaxenter.de/reflexionscode-jailbreak-manifold-typsicherheit-84641"><img width="240" height="135" src="/images/shutterstock_232482730-350x234.jpg" alt="a"></a></p>
  </td>
  <td>
  <p><a href="https://jaxenter.de/reflexionscode-jailbreak-manifold-typsicherheit-84641">Manifold: Typsicherer Reflexionscode mit @Jailbreak</a></p>
  <p>Fällt Euch die Arbeit mit Reflexionscode schwer? Der Reflexionscode an sich ist nicht typsicher, weshalb es später
     zu Problemen kommen kann. Doch keine Panik, es gibt eine Alternative! Mit @Jailbreak aus dem Manifold-Projekt kann
     man die Typsicherheit bewahren und sich der Effizienz seines Codes sicher sein.</p>
  <p><small>jaxenter.de</small></p>
  </td>
 </tr>
</table>  
<br/><br/>


 
## A Preprocessor for Java (23 July 2019)

Manifold now provides a fully integrated <b>Java Preprocessor</b>.
<br><br>
The Java Preprocessor is designed exclusively for <i>conditional compilation</i> of Java source code. It is directly
integrated into the Java compiler via the Javac Plugin API. Unlike conventional preprocessors it does <i>not</i> incur
separate build steps or additional file I/O, instead it directly contributes to the compilation pipeline.<br>
<br>
<p><img src="http://manifold.systems/images/compilerflow.png" alt="echo method" width="60%" height="60%"/></p>
<br>
The preprocessor offers a simple and convenient way to support multiple build targets with a single codebase.  It
provides advanced features such as tiered symbol definition via `build.properties` files, `-Akey[=value]` compiler
arguments, and environmental symbols such as `JAVA_9_OR_LATER` and `JPMS_NAMED`.  The preprocessor is also fully
integrated into IntelliJ IDEA using the Manifold plugin:

<br>
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();" autoplay loop>
    <source type="video/mp4" src="/images/preprocessor.mp4">
  </video>
</p>
<br>

<p>
<a href="https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-preprocessor/readme.md"><b>Learn More</b></a>
</p>
<br/><br/>



## Manifold 2019.1.8 released (23 July 2019)
New Feature
* the [Java Preprocessor](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-preprocessor/readme.md), a fully integrated preprocessor for Java
* minor fixes and improvements

Manifold version 2019.1.8 is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/2019.1.8/jar).
<br/><br/>



## Manifold 2019.1.7 released (8 July 2019)

Manifold **core** changes 
* support a **file fragment** as an r-value embedded in a Java String literal [a la F# type provider](https://fsharp.github.io/FSharp.Data/library/JsonProvider.html) 
* support Manifold fragments values in Java 13 **text blocks** 
* this change is part of the broader [file fragment](http://manifold.systems/docs.html#embedding-with-fragments) set of
changes supporting fragments in comments as type declarations and in String literals as values, to bring type-safe
resources closer to your code 
 
Manifold **JSON** changes 
* add `fromSource()` method for JSON/YAML types to enable quick usage of by-example JSON/YAML resource **data** e.g., 

```java
// Conveniently access the *data* in Preson.json directly and type-safely
Person person = Person.fromSource();
``` 
* enable JSON/YAML **fragments** e.g, as type-safe embedded comments, use `fromSource()` to gain type-safe access to resource data 
 
Manifold **Javascript** changes 
* replace the deprecated nashorn dependency with latest **rhino** 
* remove ScriptEngine usage, instead go straight to rhino 
* using a shared global scope per thread to avoid expensive js initialization 
* each program/class/template has its own scope which in turn delegates to the shared scope 
* support javascript type-safely embedded in a string literal via file **fragments** e.g., 
`int value = (int) "[>.js<] 3 + 4 + 5";` 
* this is more a **proof of concept** to demonstrate: 
1. the relative simplicity to enable any manifold resource for literal embedding 
2. GraphQL is not the only manifold that can be embedded in a literal 
3. to prepare for more languages such a **R** 
4. to get the general feature in place and ready for java 13 where **text blocks** facilitate multiline scripts (intellij's **injection editing** makes this quite attractive) 
 
Bug **fixes**: 
* Fix [#102](https://github.com/manifold-systems/manifold/issues/102) 
* other minor fixes and improvements 

<br>

><sub>Manifold release *2019.1.7* is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/2019.1.7/jar).</sub>
>
><sub>Manifold plugin for Intellij IDEA update *2019.1.7* available at [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/10057-manifold).</sub> 

<br/><br/>



## Manifold is "Trick #1" (5 July 2019)
<br>
Catch [Marco Behler's _**Five Minute Friday**_](https://www.youtube.com/watch?v=-x0QuhWJg-8) coverage of Manifold.
<p>
<iframe width="560" height="315" src="https://www.youtube.com/embed/-x0QuhWJg-8" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</p>
<br/><br/> 


## JetBrains Marketplace is LIVE with Manifold (25 June 2019):
<br>
JetBrains Marketplace is [LIVE](https://blog.jetbrains.com/platform/2019/06/jetbrains-marketplace-is-live/) today along
with [Manifold IntelliJ plugin release 2019.1.5](https://plugins.jetbrains.com/plugin/10057-manifold).
<br><br> 
<small>The Manifold IntelliJ IDEA plugin version `2019.1.5` is available directly from your IDE or visit the
[JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/10057-manifold).</small>
<br/><br/> 


## Neuer Artikel bei Jaxenter (8. Juni 2019):
<br>
<table>
 <tr>
  <td>
  <p><a href="https://jaxenter.de/rest-api-vision-mit-manifold-80931"><img width="240" height="135" src="/images/shutterstock_225924757.jpg" alt="a"></a></p>
  </td>
  <td>
  <p><a href="https://jaxenter.de/rest-api-vision-mit-manifold-80931">REST API Vision mit Manifold</a></p>
  <p>Manifold ist eine einzigartige Open-Source-Technologie, die man in jedem Java-Projekt verwenden kann, um innovative
    Sprachfunktionen wie typsichere Metaprogrammierung, Erweiterungsmethoden, Templating und strukturelle Typisierung
    nutzen zu können. Im dritten Teil unserer Artikelserie zeigt Scott McKinney, wie man Manifold einsetzen kann, um
    JSON Schema als REST API Single Source of Truth (SSoT) festzulegen. Er geht dabei auch darauf ein, wie das Framework
    JSON-Schema- und YAML-Ressourcen auf direktem Wege mit Java verbindet, ohne dabei auf Code-Generatoren, kommentierte
    POJOs oder andere Zwischenlösungen angewiesen zu sein.</p>
  <p><small>jaxenter.de</small></p>
  </td>
 </tr>
</table>
<br/><br/> 


## New article at Jaxenter (6 June 2019):
<br>
<table>
 <tr>
  <td>
  <p><a href="https://jaxenter.com/type-safe-jailbreak-manifold-159177.html"><img width="240" height="135" src="/images/shutterstock_232482730.jpg" alt="a"></a></p>
  </td>
  <td>
  <p><a href="https://jaxenter.com/type-safe-jailbreak-manifold-159177.html">Type-safe reflection code with `@Jailbreak`</a></p>
  <p>Ever overexpose fields and methods just so they can be accessed from tests? Ever write reflection code in order to
     access private class members? You can stop doing that now. Maintain integrity and type-safety with @Jailbreak from
     the Manifold project.</p>
  <p><small>jaxenter.com</small></p>
  </td>
 </tr>
</table>
<br/><br/> 


## Manifold 2019.1 is released! (5 June 2019):
<br>
Announcing Manifold's first official release -- 2019.1 is available! From *Type-safe Metaprogramming*,
*Structural Interfaces*, and *Extension Classes* to *GraphQL*, *JSON Schema*, and *Templates* and everything in between.
All ready for your imagination! 

Manifold version `2019.1.2` is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/2019.1.2/jar).
The Manifold IntelliJ IDEA plugin version `2019.1.2` is available directly from your IDE or visit the
[JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/10057-manifold).
<br/><br/> 


## A new screencast showcasing Manifold's new GraphQL support (24 May 2019):
<br>
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/graphql.mp4">
  </video>
</p>
<br>
Clone the [GraphQL sample application](https://github.com/manifold-systems/manifold-sample-graphql-app) to experiment 
with type-safe GraphQL. Don't forget to add the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold),
and while you're at it add the [JS GraphQL plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql) too; it pairs
extremely well with the Manifold plugin when developing with GraphQL.
<br/><br/> 


## Manifold 0.71-alpha released (22 May 2019)

Bug fixes and improvements 
* [#90](https://github.com/manifold-systems/manifold/issues/90): improve identifier encoding, fix JSON create/build parameter coercion
* Fix JPS compiler plugin bug involving projects without manifold dependencies
* Add Copier/Copy API support for GraphQL types and operations
* Fix race condition in IJ plugin involving internal caching
* Several minor GraphQL related fixes

Manifold version 0.71-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.71-alpha/jar).
<br/><br/>



## Manifold 0.68-alpha released (16 May 2019)

Bug fixes and improvements 
* [#87](https://github.com/manifold-systems/manifold/issues/87): handle case where a jar file has both a file entry and a directory entry as siblings with the same exact name 
* [#88](https://github.com/manifold-systems/manifold/issues/88): extension classes fix 
* [#89](https://github.com/manifold-systems/manifold/issues/89): support `graphqls` extension 
* fix NPEs related to recent changes involving manifold plugin dormancy when a project is not using manifold dependencies 

Manifold version 0.68-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.68-alpha/jar).
<br/><br/>



## Manifold 0.66-alpha released (14 May 2019)

Bug fixes and improvements 
* [#62](https://github.com/manifold-systems/manifold/issues/62): fix "never accessed" warnings for string interpolation 
* [#80](https://github.com/manifold-systems/manifold/issues/80), [#81](https://github.com/manifold-systems/manifold/issues/81): fix static ext methods re IJ usage searching, rename, etc. 
* [#84](https://github.com/manifold-systems/manifold/issues/84): manifold plugin extensions are "dumb" when project has no dependencies on manifold : they have no side effects when called 
* [#85](https://github.com/manifold-systems/manifold/issues/85): fix incremental compile/hotswap of manifold types 
* For a project with no manifold dependencies, invoking UI action to create an extension class or ManTL file produces a warning message indicating the dependencies must be added 

Manifold version 0.66-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.66-alpha/jar).
<br/><br/>



## Manifold 0.65-alpha released (8 May 2019)

Bug fixes and improvements
* Improved java source viewer on manifold resources (a debugging aid for type manifold impls in IJ)
* [#75](https://github.com/manifold-systems/manifold/issues/75): Fix stack overflow involving element refs
* [#76](https://github.com/manifold-systems/manifold/issues/76): Define a new [ManTL](http://manifold.systems/manifold-templates.html) directive: `nest`. Behaves exactly like `include` but retains and distributes the indentation whitespace immediately preceding the `nest` directive, and retains whitespace immediately following the directive.  The indentation is applied to each line in the resulting `nest`ed template or section. This behavior facilitates the code generation use-case where whitespace formatting, esp. indentation, is significant.
* [#74](https://github.com/manifold-systems/manifold/issues/74): Handle a missed use-case for checked exception suppression
* [#64](https://github.com/manifold-systems/manifold/issues/64): Provide a compile-time error indicating a method reference is not supported on a structural interface method, instead a lambda expression must be used.
* [#63](https://github.com/manifold-systems/manifold/issues/63): Fully support relative JSON refs
* Other minor changes

Manifold version 0.65-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.65-alpha/jar).
<br/><br/>



## Manifold 0.63-alpha released (1 May 2019)

Manifold 0.63-alpha introduces _schema-first_ **GraphQL** support!

With the GraphQL Manifold framework `.graphql` (SDL) files come to life in your Java project -- the entire GraphQL type
system is at your fingertips in your Java code.  Build and execute queries **type-safely**, make changes to GraphQL schema
files and automatically see and use the changes in your code _without recompiling!_  And **no code generation** steps in
your build.

Highlights
* **Schema-first** tooling for Java: `*.graphql` schema files are first-class Java types
* True centralized **single source of truth** development, *ZERO* code generation steps in your build
* **Comprehensive** schema support: queries, mutations, types, inputs, interfaces, unions, scalars, & extensions
* Simple, **type-safe** query building and execution
* Excellent **IntelliJ** support via Manifold plugin

Manifold 0.63-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.63-alpha/jar).
<br/><br/>



## Manifold 0.59-alpha released (8 April 2019)

Bug fixes
* fix jps plugin so that a source root suc as 'target/generated-sources/annotations' exists on disk before we handle incremental compilation
* fix [#60](https://github.com/manifold-systems/manifold/issues/60): regression on primitive JSON list types, ensure component type is boxed regardless of nullability
* other minor fixes

Manifold 0.59-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.59-alpha/jar).
<br/><br/>



## Manifold 0.56-alpha released (2 April 2019)

Bug fixes
* fix regression involving extension classes where a class in the same project, but a in different module may not work
* fix compilation with module that does not have a dependency on manifold (don't attempt to perform manifold incremental compilation on it)
* only warn about manifold being out of date if the project is already using manifold but is using an older version than the plugin, otherwise no warning
* other minor changes

Manifold 0.59-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.59-alpha/jar).
<br/><br/>



## Manifold 0.55-alpha released (31 March 2019)

Manifold provides a new option to **Turn Off Checked Exceptions!**

Highlights
* Simply add the `exceptions` plugin argument e.g., `-Xplugin:Manifold strings exceptions`
  * Now checked exceptions behave like unchecked exceptions! No more compiler errors, no more boilerplate try/catch, no more nonsense.

Bug fixes
* [#55](https://github.com/manifold-systems/manifold/issues/55), don't display warning message re manifold jars for a project without manifold dependencies
* other minor fixes
<br/><br/>



## Manifold 0.53-alpha released (28 March 2019)

Highlights
* **Support Java 12**
* Fix issues with "Create Extension Method Class" dialog
* Some performance improvements esp. faster manifold resource compilation with larger projects
* Other minor improvements and fixes
<br/><br/>



## Manifold 0.50-alpha released (14 March 2019)

Highlights
* Provide means to satisfy a structural interface dynamically via registered IProxyFactory service, major perf improvement
* Self type improvements

Bug fixes
* Fix regression where annotation processing libraries could cause problems with self types, structural types, etc.
* Fix completion issue. A project with multiple unrelated modules can now access extension methods from shared jar files such as Manifold's builtin extensions for String etc.
<br/><br/>



## Manifold 0.48-alpha released (12 March 2019)

Highlights
* Rewrite @Self implementation to provide comprehensive 'self' type support. Essentially, @Self is suitable as a simpler alternative to recursive generic types.
* @Self can be applied to:
  * instance method return type
  * instance method parameter type
  * instance field type
* You can override methods having @Self in a parameter and maintain the super type's signature, but also have your subclass type enforced, no bridge methods or other shenanigans otherwise present with recursive generics
* @Self is fully supported in *extension methods*
>Note the completion of @Self facilitates [#47](https://github.com/manifold-systems/manifold/issues/47) -- use @Self instead of generic methods and recursive generic types.
<br/><br/>



## Manifold 0.45-alpha released (17 February 2019)

Structural Interface improvements
* Structural interface improvements
  * Provide a solution to eliminate the first-time load/cast overhead for a structural interface. Enable a structural interface to provide its own proxy factory via new optional parameters to @Structural(factoryClass, baseClass)

JSON Schema improvements
* JSON array types are now concrete types and define a component type named Item
  * Thus a JSON array declared as "Users" has type "Users" and, if not a $ref, its nested component type is named "UsersItem"
  * The "Users" type is an interface and extends IJsonList, which in extends List
* Added `request(URL)` static method on all JSON API interfaces
  * Use to conveniently navigate an HTTP REST API with GET, POST, PUT, PATCH, & DELETE
* IJsonParser no longer wraps lists in bindings with single "value" property -- returns JSON List as-is now
* Fixed bug dealing with not preserving insertion order of oneOf/anyOf union types
* Several other bug fixes along the way
<br/><br/>



## Manifold 0.43-alpha released (7 February 2019)

Manifold core changes
* Support dynamic compilation/loading of resources in other dependency modules in a multi-module Java 11 (JPMS) project
* Eliminate "built-in" type manifolds, move them all out into separate modules:
  * `manifold-properties`
  * `manifold-image`
  * `manifold-darkj`
>Note although all built-in type manifolds are now registered and loaded separately as services, they are all still included in the `manifold-all` jar

Manifold JSON changes: 
* Support $ref paths of any kind:
``` 
$ref: "#someId/foo" 
$ref: "#/definitions/raw.name" 
etc.
``` 
* Support `/properties` in a $ref path 
* Support non-standard JSON Schema type names with proper-case, `String`, `Object`, `Array` and also support `double` as an alias for `number`. 
* Fix a bug where an errant Json list type has a null component type
* Fix a bug where a $ref to a oneOf type did not add union methods to the ref'ing type
<br/><br/>



## Manifold 0.42-alpha released (2 February 2019)

New **YAML** support with the new YAML Type Manifold.

Highlights
* Use JSON Schema and YAML interchangeably
* New JSON Schema fluent API
* Performance optimizations relating to very large scale IntelliJ projects such as IJ CE EAP
* Enhancements to @Precompile
* Manifold works with Lombok (edge release)
* Very large scale projects supported
<br/><br/>



## Manifold 0.37-alpha (17 January 2019)

Manifold JSON changes
* Support JSON Schema's many curious ways of saying a type is "nullable":
  * The type array: `"type": ["", "null"]`
  * The union type: `"oneOf": [ ..., {"type": "null"}]`
  * The enum type: `"enum": [..., null]`
  * OpenAPI's sane way: `"nullable"`

* Support OpenAPI formats:
  * `byte`, `binary` with backing classes: `Base64Encoding` and `OctetEncoding`
  * `int64` with `Long`/`long`
  * note `int32`, `float`, and `double` formats are naturally accounted for with default backing types Integer/int, Double/double, no need for special formats

* Support `readOnly` and `writeOnly`
* Support `additionalProperties` and `patternProperties`
  * both of these control whether or not a type can be treated as a general map, has methods get(key) and put(key, value)
* Support json schema cycles stemming from `oneOf` i.e., interface Foo extends Foo.InnerClass
  * such a cycle is short-circuited by directly incorporating the super interface's property methods in the extending type
  * Foo.InnerClass remains an inner class of Foo and Foo remains structurally assignable to Foo.InnerClass (JSON interfaces are structural interfaces)
* Add Builders having withXxx() methods
  * a json schema interface now has a static builder(...) method with parameters matching the create(...) method (required properties)
  * returns inner class Builder instance having withXxx(x) matching all non-required properties
* Much refactoring to better accommodate the additional `readOnly`, `additionProperties`, etc. attributes

* support nested `definitions` for JSON Schema
<br/><br/>



## Manifold 0.34-alpha (4 January 2019)

Manifold Templates (ManTL) changes
* support static imports in the `import` directive 
* change template generation to support very large template files esp. content chunks larger than 64k 
* several other template related fixes/refactors/changes 

Manifold JSON changes 
* Remove overhead of JSON dynamic proxy by removing it in favor of improving the interface API to provide its own implementation 
  * Usage of a JSON interface no longer involves a runtime delay the first time it is used
* Type-safe support for JSON Schema `enum` types, generate Java enum to correspond with any JSON `enum` including non-string values 
* Type-safe, pluggable support for JSON Schema `format` types like `date-time` etc. 
  * The JSON type manifold supplies a Java service provider API with `IJsonFormatTypeResolver`
  * Implement `IJsonFormatTypeResolver` to map Java types to your own formats
  * Manifold supports standard JSON Schema formats including `date-time`, `date`, `time` using `java.time.LocalDateTime`, `LocalDate`, and `LocalTime`
  * Manifold supports some non-standard formats too including `utc-millisec` with `java.time.Instant`
  * Additionally Manifold provides new (non-standard) formats such as `big-integer` and `big-decimal`
* Support `default` value and `required` properties type-safely by adding parameters to the JSON interface's static create() method where each parameter corresponds with a `required` property that does not have a `default` value 
* Support `const` as a single value enum type (as described in the JSON Schema) 
* Support `allOf`, `anyOf`, `oneOf` where all the component types are enums such that regardless of the all/any/one operation the resulting type is a single enum composed of all the constants in all the component enum types
<br/><br/>



## Manifold 0.33-alpha released (22 December 2018)

Manifold ext changes
* `@Jailbreak` supports using members of a class to which the JPMS otherwise prohibits access e.g., call a method on a class in a package that is not exported or open to the module of the call site
* small perf improvement on structural proxy generation, test for ICallHandler statically as well as via extension and cache the result
Manifold Templates (ManTL) changes
* fix `section` support involving params
* support lambda usage where the statement block of the lambda is used for generating content
* filter leading spaces associated with some non-content template constructs
<br/><br/>



## Manifold 0.32-alpha (11 December 2018)

* Fix some issues introduced with Jailbreak (from ver 0.30-alpha)
  * Rename `@JailBreak` to proper spelling '@Jailbreak` (doh!)
  * Fix problems related to compile error reporting from javac
  * Prohibit use of @Jailbreak in compound assignment expressions and increment/decrement expressions; it's better to use direct assignment with '='
  * Resolve https://github.com/manifold-systems/manifold/issues/33
<br/><br/>



## Manifold 0.30-alpha (7 December 2018)

New `@JailBreak` feature.

Gain direct, type-safe access to otherwise inaccessible classes/methods/fields.  Use `@JailBreak` to avoid the drudgery and vulnerability of Java reflection:
```java
@JailBreak Foo foo = getFoo();
foo.privateMethodOnFoo();
foo.privateFieldOnFoo = value;
```
Added `jailbreak()` extension method to Object.  Use it like `@JailBreak` but directly in an expression:
```java
foo.jailbreak().privateMethodOnFoo()
```
Bug fixes
* Fixed [#31](https://github.com/manifold-systems/manifold/issues/31)
<br/><br/>



## Manifold 0.28-alpha released (22 November 2018)

Highlights
* Support the [Self type](http://manifold.systems/docs.html#the-self-type) via the new `@Self` feature
* Fix modifier code gen where public was used instead of the original modifier
* Fix ReflectUtil method invocation to unwrap InvocationTargetException and rethrow original exception
* Other minor changes
<br/><br/>

