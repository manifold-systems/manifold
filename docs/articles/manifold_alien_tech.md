# Manifold: Alien Technology


## Incident Report

**Place of Occurrence**: CLASSIFIED    **Date**: 24/11/17    **Time**: 22:15Z

**Programmer**: CLASSIFIED    **Language**: Java    **Tooling**: IntelliJ IDEA

**Details of Incident**:

Witnessed subject create a JSON file, `Person.json`, as a Java resource in package `com.abc`.  
Subject immediately began using the file from Java classes in the project as if the JSON file 
were a Java class of type `com.abc.Person`.  As subject added and modified properties in the 
JSON file, the changes were immediately available elsewhere in the project using code completion 
etc.  Subject later performed a rename refactor on the JSON file, renaming it from `Person` to 
`Contact`.  All references to `Person` were immediately changed to `Contact`.  Likewise, subject
frequently performed several usage searches and refactors on JSON properties and types, all 
behaving as if the JSON were Java.  
 
Importantly, there were _no generated class files on disk_ corresponding with the JSON file after 
subject compiled the project -- no extra build steps, no artifacts created. Subject created 
several tests for the project and ran them without incident. The JVM appeared to use the JSON file
as a Java class file.  No extra class loaders were involved, no runtime agents were used.  Only a 
single jar file in the classpath was present: **manifold.jar**.   

Witnessed similar events involving Javascript files, Properties files, and several others.  This
technology appears to be capable of working with any type of data source.  Essentially, the 
technology somehow extends and enhances Java's type system at will.

More recently observed subject perform other bizarre activities, such as declare and use 
_Extension Methods_ as defined in C# and use _Structural Typing_ similar to TypeScript and Go languages.
More to follow regarding these accounts.


**Conclusion**:

Incident demonstrates beyond-next-generation technology.  Recommend further investigation.

## Earthly Origin?

As dreamy as the "incident report" appears, it is 100% factual.  Manifold is real and available as
an open source project from [Manifold Systems](http://manifold.systems/); whether or not
it is reverse-engineered alien technology is yet to be determined.  In any case you can begin using
it with your new or existing Java project.

Using Manifold is easy, simply add it as a dependency to your project. Manifold is designed to work 
with IntelliJ IDEA, which is available free from JetBrains.  Visit the Manifold 
[Setup](http://manifold.systems/docs.html#setup) instructions for information about using Manifold with
your project and build tools.  

Manifold provides three high-level features:

* **Type-safe Metaprogramming** -- similar in concept to F# type providers
* **Extension Methods** -- comparable to the same feature in Kotlin and C#
* **Structural Typing** -- much like interfaces in TypeScript and Go 

## Type-safe Metaprogramming

_Metaprogramming_, a term usually reserved for dynamic languages like Javascript, Ruby, and Python, is a powerful 
feature used for dynamic type creation that exploits the lack of design-time type-safety in 
these languages -- all the metaprogramming hocus pocus is a runtime phenomenon.  While flexible, metaprogramming 
presents a challenge for programmers using it because there is no design-time type information available for 
 them to readily discover and use; you just have to "know".

The allure of Manifold is squarely centered on its ability to perform _compile-time_ as well as runtime 
metaprogramming.  This capability is achieved via _Type Manifolds_.
 
Bridging the worlds of information and programming, type manifolds act as adapters 
to automatically connect schematized data sources with Java.  More specifically, 
a type manifold transforms a data source into a data _type_ directly accessible in 
your Java code eliminating code generation build steps involved with conventional tools. 
Manifold automatically keeps types in sync with data sources as you make changes.
In essence with Manifold a data source **_is_** a data type.

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
*   Dark Java
*   ManTL (Manifold Template Language)
*   DDL and SQL (work in progress)

Discover more about type manifolds here: http://manifold.systems/docs.html#what_is_a_type_manifold


## Extensions

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
extended class: http://manifold.systems/images/ExtensionMethod.mp4

Extensions provide a lot more capability, learn more here: http://manifold.systems/docs.html#extension_classes


## Structural Typing

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

By contrast a _structurally_ typed language like TypeScript or Go has no problem with this example. 
Basically, structural typing requires only that the interface _methods_ are implemented, there is no 
need for a class to declare that it implements a structural interface.

Manifold provides this capability via the `@Structural` annotation:

```java
@Structural
public interface Greeting {
  void hello();
}
```  
Adding `@Structural` to `Greeting` effectively changes it to behave _structurally_ -- Java no longer
requires classes to implement it by name, only its methods must be implemented.

Note a class can still implement a structural interface nominally. Doing so helps both people and tooling 
comprehend your code faster. The general idea is to use an interface structurally when you otherwise can't 
use it nominally or doing so overcomplicates your code.

Learn more about Manifold structural interfaces here: http://manifold.systems/docs.html#structural_interfaces


## Benefits

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

## Conclusion

Finally, with Manifold, Java bridges the gap separating it from many of the features previously granted 
exclusively to dynamic languages. What's more, Manifold delivers these features with type-safety intact. 
Meta-programming, extension classes, and structural typing are readily available and fully integrated in 
IntelliJ IDEA.  While this may sound impossible or far fetched, you can verify it first-hand -- explore
Manifold at [Manifold Systems](http://manifold.systems/).  The truth is out there!