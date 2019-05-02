---
layout: default
---

# News
<br/><br/>


## Manifold 0.62-alpha released (1 May 2019)

Manifold 0.62-alpha introduces _schema-first_ **GraphQL** support!

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

Manifold 0.62-alpha is available for download on [Maven Central](https://search.maven.org/artifact/systems.manifold/manifold-all/0.62-alpha/jar).
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

