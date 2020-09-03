# Manifold: Part I

This is the first in a series of articles covering [Manifold](http://manifold.systems/), a unique, open source technology you can use 
in any Java project to enable cutting edge language features such as type-safe metaprogramming, extension 
methods, templating, and structural typing.  In this segment I discuss Manifold's reinvention of code generators, namely
_Type Manifolds_.


# ☠ Death to Code Generators ☠

While Manifold provides a broad host of high-level features, its primary focus is to eliminate the gap separating source 
code from metadata. Software developers overwhelmingly use code generators to bridge the gap, however this decades-old 
tactic is notorious for slowing or impeding progress and is not well suited for many contemporary software architectures. 
If a project you develop involves one or more code generators, perhaps you are familiar with the disadvantages. Read on 
for a more productive alternative.

## The Metadata Disconnect

Our modern lives are replete with structured information, or metadata.  It is _everywhere_ and it is produced by near 
everything with a power cord. As a consequence the software industry has become much less code-centric and much more 
information-centric. Despite this transformation the means by which our software consumes metadata has remained 
virtually unchanged for half a century. Whether it's JSON, XSD/XML, WSDL, CSV, DDL, SQL, JavaScript, XLS, or any one of 
a multitude of other metadata sources, most modern languages, including Java, do very little to connect them with your code:

**../abc/Person.json**
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "title": "Person",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string"
    },
    "lastName": {
      "type": "string"
    },
    "age": {
      "type": "integer",
      "minimum": 0
    }
  },
  "required": ["firstName", "lastName"]
}
```
                                           
As Java developers we want to use metadata in a type-safe manner -- we want to use the `Person` JSON schema file as a 
`Person` Java class:

```java
class Foo {
  private abc.Person person; // ERROR: Cannot resolve symbol 'abc.Person'
}
```  

Of course the Java compiler has no idea what to do with `abc.Person`, so we resort to running a code generator in a 
separate build step to generate _all_ our JSON classes beforehand so the compiler can readily use them. The effects of 
this build step on the development lifecycle range from mild irritation to utter devastation, depending on the rate of metadata
change, number and size of metadata files, density of usage in project, number of developers, etc. The problems include:
* stale generated classes
* long build times
* code bloat esp. with large metadata domain
* changes to structured data don't invalidate generated code
* no support for incremental compilation, all or nothing
* can't navigate from code reference to corresponding element in the structured data
* can't find code usages of elements from the structured data  
* can't refactor / rename structured data elements 
* complicated custom class loader issues, generated classes loaded in separate loader
* concurrency problems with the shared thread context loader
* generated code is often cached and shared, which leads to stale cache issues
* customers often need to change metadata, which requires access to code generators

## The Manifold Framework

The Manifold framework represents a rethinking of code generators.  It altogether avoids the many disadvantages often
involved with them by directly integrating with the Java compiler via the [javac plug-in mechanism](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html). 
Implementations of the Type Manifold API, called _type manifolds_, establish a type supplier relationship with the Java 
compiler -- the Manifold framework hooks into the compiler so that as the compiler encounters type names the type 
manifolds contribute toward resolving them, generating code in memory as needed.  As such your application code 
can reference metadata sources directly by name as Java types, effectively enabling the prior example to work:
```java
class Foo {
  private abc.Person person; // OK :)
}
```

Think of a type manifold as a new domain of types for the compiler to access.  As such the Manifold framework serves as 
a gateway between javac and type manifolds, effectively expanding Java's type system to include whole new domains of 
types.  Any number of type manifolds can operate in concert; they can also cooperate so that the types contributed from 
one can feed into the next and so on, forming a type building pipeline. 

<p><img src="http://manifold.systems/images/manifold_diagram.png" alt="echo method" width="60%" height="60%"/></p>

As the diagram illustrates a type manifold contributes to the definition of types in its domain. For example, the [JSON
type manifold](http://manifold.systems/docs.html#json-and-json-schema) produces types defined in JSON files.  A type manifold can contribute toward a type in different ways. Most
often a type manifold registers as a `primary` contributor, it supplies the main body of the type.  The JSON type manifold 
is a primary contributor because it supplies the full type definition according to a JSON Schema file or JSON sample file.
Alternatively, a type manifold can be a `partial` or `supplementary` contributor.  The [Extension type manifold](http://manifold.systems/docs.html#extension-classes), for instance, 
is a supplementary contributor because it augments an existing type with additional methods, interfaces, and other features.  Thus 
both the JSON and Extension type manifolds can contribute to the same type, where the JSON manifold supplies the main 
body of the type and the Extension type manifold contributes methods and other features provided by extension classes 
(I'll cover Extensions in a later article).   

Altogether this strategy eliminates many problems plaguing conventional code generators and metadata access in general.
In essence the Type Manifold API redefines what it means to be a code generator. Benefits include:
* **Zero turnaround** – live, type-safe access to metadata; make, discover, and use changes instantly
* **Lightweight** – direct integration with standard Java, requires no special compilers, annotation processors, class loaders, or runtime agents
* **Efficient, dynamic** – Manifold only produces types as they are needed by the compiler
* **Simple, open API** – you can build your own type manifolds
* **No code generation build step** – eliminates code generators from your development build process (when dynamic mode is used) 
* **IntelliJ IDEA** – comprehensive IDE support: incremental compilation, code completion, navigation, usage searching, refactoring, debugging, etc.

Further, the Type Manifold API unifies code generator architecture by providing much needed structure and consistency 
for developers writing code generators. It puts an end to "lone wolf" code generator projects only one developer fully understands.
Moreover, you don't have to invest in one-off IDE integration projects; the [Manifold plugin for IntelliJ](http://manifold.systems/docs.html#working-with-intellij) handles everything 
for you, from incremental compilation to usage searching to refactoring.  Finally, even if you've already invested in an 
existing code generator, you can still recycle it as a wrapped type manifold -- the wrapper can delegate 
source production to your existing framework.  Learn more about implementing type manifolds [here](http://manifold.systems/docs.html#build-your-own-manifold).


## Synergy

Perhaps the most refreshing benefit from using Manifold is the synergy resulting from its presence in all stages of development.  With Manifold you
can define and use metadata that best suits your needs without having to concern yourself with build implications or 
IDE integration; you can create a metadata file, use it directly as a type, modify it, and access the changes 
immediately in your code; no awkward build/compilation steps involved, no caches to update.  With comprehensive IDE 
support, you can readily navigate to and from metadata elements, find usages from metadata, refactor, etc.  
Finally metadata has first-class representation in the Java development lifecycle!  [View it in action](http://manifold.systems/images/JsonDemo.mp4).

## Using Manifold

### Setup

Using Manifold in your Java project is easy:

* Add the Manifold jar as a plugin argument to java**c**
* Add the Manifold jar to your java classpath (optional)

That's all.

Manifold fully supports [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), [Java 9](http://www.oracle.com/technetwork/java/javase/downloads/jdk9-downloads-3848520.html), and [Java 10](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html).

Manifold works well with Maven and Gradle too.  Learn more about adding Manifold to your project [here](http://manifold.systems/docs.html#setup).

### Working with IntelliJ

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

**Install**

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<iframe frameborder="none" width="245px" height="48px" src="https://plugins.jetbrains.com/embeddable/install/10057">
</iframe>

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>


**New Project**

Creating a new project with Manifold support is easy.  [Check it out](http://manifold.systems/images/NewProject.mp4).

**Add Manifold to Existing Module**

You can add manifold to module[s] of an existing project too. [Check it out](http://manifold.systems/images/ManifoldModule.png).

**Sample Project**

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project).


## Conclusion

As a long time Java developer I've personally worked on several projects involving heavy code generation.  I've seen
the sometimes devastating effects of its use: build times measured in hours at customer sites, dev lifecycle demoralization, 
code generator development and maintenance consuming precious time, etc.  It's about time for a better solution and I think Manifold
makes good progress toward that goal.

There's much more to cover.  Future articles in this series will address:
* Using the [JSON type manifold](http://manifold.systems/docs.html#json-and-json-schema)
* The Extension Manifold and writing [Extension Classes](http://manifold.systems/docs.html#extension-classes)
* [Structural Interfaces](http://manifold.systems/docs.html#structural-interfaces)
* [Templates](http://manifold.systems/docs.html#templating)
* The SQL type manifold

As a bonus for reading this far, I'll touch on one of Manifold's latest features...

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

Learn more [here](http://manifold.systems/docs.html#templating).
 