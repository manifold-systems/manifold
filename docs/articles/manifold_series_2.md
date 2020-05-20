#### Innovative language features for Java - Part 2

This is the second in a series of articles covering [Manifold](http://manifold.systems/), a new breed of tooling for
Java. [Part 1](https://jaxenter.com/manifold-code-generators-150738.html) introduces the *Type Manifold API*, a powerful 
alternative to conventional code generation.  This segment explores *Extension Classes*, an innovative feature that 
enables you to supplement a class with your own methods, interfaces, and other features without subclassing and without
changing the original class. 

## Extension Methods for _Java?!_

Quick! Write some code to read the contents of a `File` into a `String`.  Ready, go!

As a pragmatic developer you hope for something like this:
```java
  String contents = file.readText();
```   
Regrettably, you type `file.` in your IDE and quickly discover no such method exists.  Next you search *stackoverflow*
for boilerplate solutions and find a useful snippet. You want to save yourself and others from duplicating this effort, 
so you wrap the boilerplate snippet in a *Util* library:

```java
public class MyFileUtil {
  public static String readText(File file) {
    // boilerplate code...
  }
} 
```   

Now you can write:
```java
String contents = MyFileUtil.readText(file);
```

Is this as good as it gets?

You deserve a friendlier, more practical `File` API -- `readText()` is better suited and more easily discoverable 
as an instance method *directly* on `File`.  This is where a feature commonly known in the language community as 
*Extension Methods* makes all the difference.  This is also where Manifold picks up where Java leaves off. 

Manifold fully implements Extension Methods for Java via [Extension Classes](http://manifold.systems/docs.html#extension-classes):

```java
package extensions.java.io.File;

import manifold.ext.api.*;

@Extension
public class MyFileExtension {
  public static String readText(@This File thiz) {
    // boilerplate code...
  }
}
``` 

`MyFileExtension` supplements `File` with `readText()` as an instance method:
```java
String contents = file.readText();
```

Which is exactly what the pragmatist in you wants!

What's more, [IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold) provides comprehensive support for 
Manifold. You can use features such as code completion to easily discover and use extension methods:

![completion](https://dl.dropbox.com/s/nnnv3juw14j7z79/manifold_article_je_p2_completion.png) 

See it in action. Create new extension classes, refactor them, find usages, and more:
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="http://manifold.systems/images/ExtensionMethod.mp4">
  </video>
</p>

## Anatomy of an Extension Class

Extension classes are easy to implement using simple conventions and annotations:

```java 
package extensions.java.io.File;
```
An extension class' package name must end with `extensions.<extended class name>`.  With Java 8 you may
root all extension classes in the `extensions` package.  In Java 9 or later, if you are using explicit modules, you must prepend 
the name of the module to the package eg., `package foo.extensions.java.io.File`, where `foo` is the name of the module.
To maintain unique names across dependencies, it's generally good practice to further qualify extension classes anyway.

```java
@Extension
public class MyFileExtension {
```
An extension class must be annotated with `@Extension`, this helps Manifold quickly identify extension classes in
your project.

```java
public static String readText(@This File thiz) {
```
All extension methods must be declared `static`, more on this later. As the receiver of the call the first parameter of
an extension *instance* method must be the same type as the extended class, in this case `File`.  The parameter name
`thiz` is conventional, you can use any name you like.

That's basically it for the common case.

## Static Methods

You can define `static` extension methods like this:

```java
@Extension
public static FileSystem getLocalFileSystem() {
  return FileSystems.getDefault();
}
```
Since static methods don’t have a receiver, the method itself must be annotated with `@Extension` so Manifold can 
identify it as such.

Call it as if it were a normal static method on `File`:
```java
File.getLocalFileSystem()
```

## Generics

You can make extensions for generic classes too and define generic extension methods. This is how Manifold extension 
libraries work with collections and other generic classes. For example, here is the `first()` extension method on
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
extended class: `T` from `Iterable<T>`. Since extension methods are static this is how you convey type
variables from the extended class to the extension method.

To define a generic extension method you append the type variables of the *method* to the list of the
extended class' type variables. Manifold's `map()` extension illustrates this format:

```java
public static <E, R> Stream<R> map(@This Collection<E> thiz, Function<? super E, R> mapper) {
  return thiz.stream().map(mapper);
}
```
Here `map` is a generic extension method having type variable `R` and conveying `Collection`'s type
variable `E`.

## Static Dispatching

An extension class does not physically alter its extended class; the methods defined in an extension are
not really inserted into the extended class. Instead the Java compiler and Manifold cooperate to make a
call to a static method in an extension look like a call to an instance method on the extended class. As a
consequence extension calls dispatch *statically*. Thus unlike a virtual method call an extension call is 
always made based on the *compile-time* type of the receiver.

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

## Accessibility and Scope

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

The extension method never wins, a call to `kind()` always prints `"evergreen"`. Additionally, if at
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


## Extension Libraries

An extension library is a logical grouping of functionality defined by a set of extension classes.
Manifold includes several extension libraries for commonly used classes, many of which are adapted
from Kotlin extensions.  Each library is available as a separate module or Jar file you can add 
to your project separately depending on your needs.

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
    - manifold.rt.api.Bindings

## Summary

Extension methods offer a powerful alternative to *Util* libraries.  The feature is widely supported in modern languages
including C#, Scala, and Kotlin.  Now with Manifold you can use extension methods with Java. Use it to boost
developer productivity with APIs and to benefit from Manifold's built in extension libraries on common classes.  The
easiest and best way to begin using extension methods and other Manifold features is via the Manifold plugin for 
[IntelliJ IDEA](https://www.jetbrains.com/idea/download/).  

**More to come:**
Later in the series I'll cover [Structural Typing](http://manifold.systems/docs.html#structural-interfaces), 
a powerful abstraction similar to interfaces in TypeScript and Go.  Combined with extension classes, structural 
typing makes possible several exciting features including [Extension Interfaces](http://manifold.systems/docs.html#extension-interfaces)
and [Implementation by Extension](http://manifold.systems/docs.html#implementation-by-extension).
  

 

