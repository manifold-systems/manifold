# Manifold : Java Extensions

## Table of Contents
* [Extension classes](#extension-classes-via-extension) via `@Extension`
  * [The `extensions` Package](#the-extensions-package)
  * [Basics](#extension-method-basics)
  * [Generics](#generics)
  * [Inner Classes](#inner-classes)
  * [Arrays](#extending-arrays)
  * [Manifold Types](#extending-manifold-types)
  * [Static Dispatching](#static-dispatching)
  * [Accessibility & Scopes](#accessibility-and-scope)
  * [Adding Annotations](#annotation-extensions)
  * [Adding Interfaces](#extension-interfaces)
  * [Extension Libraries](#extension-libraries)
  * [Generating Extensions](#generating-extension-classes)
* [Operator Overloading](#operator-overloading)
  * [Arithmetic Operators](#arithmetic-operators)
  * [Relational Operators](#relational-operators)
  * [Equality Operators](#equality-operators)
  * [Index Operator](#index-operator)
  * [Unit Operators](#unit-operators)
  * [Operators by Extension Methods](#operators-by-extension-methods)
* [Unit Expressions](#unit-expressions)
  * [How does it work?](#how-does-it-work)
  * [Operator Precedence](#operator-precedence)
  * [Type-safe & Simple](#type-safe-and-simple)
  * [More Than Units](#more-than-units)
  * [Science & Ranges](#science--ranges)
* [Structural interfaces](#structural-interfaces-via-structural) with `@Structural`
  * [Assignability and Variance](#type-assignability-and-variance)
  * [Implementation by Field](#implementation-by-field)
  * [Implementation by Extension](#implementation-by-extension)
  * [Implementation by Proxy](#implementation-by-proxy)
  * [Dynamic Typing](#dynamic-typing-with-icallhandler)
* [Type-safe reflection](#type-safe-reflection-via-jailbreak) with `@Jailbreak`
  * [Basics](#using-the-jailbreak-extension)
  * [Using `jailbreak()`](#using-the-jailbreak-extension)
* [The *Self* type](#the-self-type-via-self) via `@Self`
  * [Builders](#builders)
  * [Self + Generics](#self--generics)
  * [Self + Extensions](#self--extensions)
  * [Overriding Methods](#overriding-methods)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

Add the `manifold-ext` dependency to your project to enable a broad set of functionality to improve your development
experience with Java.
Use [extension classes](#extension-classes-via-extension) to add new methods and other features to existing classes.
Enable types to participate as operands in arithmetic and other expressions with [operator overloading](#operator-overloading).
Experiment with [unit expressions](#unit-expressions) as a new way to improve readability and to avoid costly
unit-related errors.
Escape the rigidity of nominal typing with [structural interfaces](#structural-interfaces-via-structural).
Avoid the tedium and error-prone nature of Java reflection using [type-safe reflection](#type-safe-reflection-via-jailbreak).
Utilize the [self](#the-self-type-via-self) type as a simple alternative to recursive generic types.
 
# Extension Classes via `@Extension`

Similar to other languages such as [C#](https://docs.microsoft.com/en-us/dotnet/csharp/csharp),
[Kotlin](https://kotlinlang.org/), and [Gosu](https://gosu-lang.github.io/), with Manifold you can define methods and
other features as logical extensions to existing Java classes. This is achieved using _extension classes_. An extension
class is a normal Java class you define as a container for features you want to apply to another class, normally to one
you can't modify directly, such as `java.lang.String`:

```java
// package name ends with "extensions." + extended class name
package extensions.java.lang.String;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

// Defines methods logically added to String
@Extension
public class MyStringExtension {
  // Add print() instance method to String via @This
  public static void print(@This String thiz) {
    System.out.println(thiz);
  }

  // Add lineSeparator() static method to String via @Extension
  @Extension
  public static String lineSeparator() {
    return System.lineSeparator();
  }
}
```
The `MyStringExtension` extension methods are directly available on `String`:
```java
"Hello World!".print();

String separator = String.lineSeparator();
```                       

## The `extensions` Package

All extension classes must be sub-rooted in the `extensions` package where the remainder of the package
must be the qualified name of the extended class. As the example illustrates, an extension
class on `java.lang.String` must reside directly in a package equal to or ending with `extensions.java.lang.String`. Note this
convention facilitates the extension discovery process and avoids the overhead and ceremony of
alternative means such as annotation processors.

With Java 9 or later, because a package must reside in a single module, you should prepend your module name to the extension package
name to avoid illegal sharing of packages between modules.  For example, if your module were named `foo.widget` you
should define your extension class in package `foo.widget.extensions.java.lang.String`.  In Java 8 all extension classes
can be directly rooted in the `extensions` package, however it is still best to qualify extension classes with your
project or module name to prevent naming collisions.

Additionally, as the example illustrates, an extension class must be annotated with `manifold.ext.rt.api.Extension`, which distinguishes extension
classes from other classes that may reside in the same package.

## Extension Method Basics

An extension method must be declared `static` and non-`private`. As the receiver of the call, the first
parameter of an extension _instance_ method must have the same type as the extended class. The
`MyStringExtension` example illustrates this; the first parameter of instance method `print` is
`java.lang.String`. Note the parameter name _thiz_ is conventional, you can use any name you like.
Finally, the receiver parameter must be annotated with `manifold.ext.rt.api.This` to distinguish it from 
regular methods in the class.

That's all there is to it. You can use extensions just like normal methods on the extended class:

```java
String name = "Manifold";
name.print();
```

You can define `static` extension methods too. Since static methods don't have a receiver, the method
itself must be annotated with `manifold.ext.rt.api.Extension`:

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

## Generics

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


## Inner Classes

Extend an inner class by first creating an extension on the outermost class enclosing the inner class. Then add a 
nest of inner classes to match the inner class you want to extend. You add extension methods etc. to the inner class
just as you would a top-level class.

Here's an example adding an extension method to `Map.Entry`.
```java
package myproject.extensions.java.util.Map;

import java.util.Map;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class MyMapExt
{
  public static <K,V> String myMapMethod(@This Map<K,V> thiz) {
    return "myMapMethod";
  }

  public static class Entry {
    public static <K,V> String myEntryMethod(@This Map.Entry<K,V> thiz) {
      return "myEntryMethod";
    }
  }
}
```     

With this extension in place you can call the `myEntryMethod` directly on the `Map.Entry` interface:
```java                  
Map<String, String> map = getMap();
for(Entry<String, String> entry: map.entrySet()) {
  entry.myEntryMethod();
}
```

## Extending Arrays

Java has no base type for the array class; there's no "java.lang.Array" to add extension methods to. Therefore, Manifold
provides a substitute type for the sole purpose of adding extension methods, namely `manifold.rt.api.Array`. Extension
classes extending this class effectively extend Java's array class.

```java
package myproject.extensions.manifold.rt.api.Array;

import java.lang.reflect.Array;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class MyArrayExtension {
  public static final String myMethod(@This Object array) {
    return "Size of array: " + Array.getLength(array);
  }
}

// usage
String[] strings = new String[] {"a", "b", "c"};
strings.myMethod();
```

Manifold provides the builtin Array extension class, `ManArrayExt`, which provides the following methods:
```java
  List<@Self(true) Object> toList()
  boolean isEmpty()
  boolean isNullOrEmpty()
  @Self Object copy()
  @Self Object copy(int newLength)
  @Self Object copyTo(@Self Object to)
  @Self Object copyRange(int from, int to)
  @Self Object copyRangeTo(int from, int to, @Self Object target, int targetIndex)
  Stream<@Self(true) Object> stream()
  void forEach(IndexedConsumer<? super @Self(true) Object> action)
  Spliterator<@Self(true) Object> spliterator()
  int binarySearch(@Self(true) Object key)
  int binarySearch(int from, int to, @Self(true) Object key)
  int binarySearch(@Self(true) Object key, Comparator<? super @Self(true) Object> comparator)
  int binarySearch(int from, int to, @Self(true) Object key, Comparator<? super @Self(true) Object> comparator)
  int hashCode()
  boolean equals(@Self Object that)
```

Note the use of `@Self` in many of the extension methods. It provides type-safe access to the array's component type as
well as to the array type itself. For instance, the `toList()` method provides type inference and enforces the array's
component type as the `List` type argument:
```java
String[] array = {"a", "b", "c"};
List<String> list = array.toList();
```
Note also the use of type `Object` instead of an array type. Using `Object` annotated with `@Self` supports both
reference arrays and primitive arrays with type inference,.
```java
// reference array
String[] array = {"a", "b", "c"};
String[] copy = array.copy();

// primitive array
int[] array = {1, 2, 3};
int[] copy = array.copy();
```
## Extending Manifold Types

Types produced from type manifolds such as the GraphQL and JSON Manifolds can be extended too. For instance, a GraphQL
file called `movies.graphql` in the `abc/res` resource directory results in a type named `abc.res.movies`. Likewise, a
`Person` type defined in the file is an inner class of `movies` with name `abc.res.movies.Person`. Thus an extension on the
`Person` inner type uses the same technique explained in the previous section on Inner Classes.   

```java
package myproject.extensions.abc.res.movies;

import abc.res.movies.Person;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class MyMoviesExt
{
  public static class Person {
    public static String myPersonMethod(@This Person thiz) {
      return "myPersonMethod";
    }
  }
}
``` 

>Note, this type of extension facilitates [Domain Model](https://en.wikipedia.org/wiki/Domain_model) design principles.
>You can add behavior to data objects such as GraphQL types in the form of extension methods. As such both data and
>behavior are logically incorporated.
                                    

## Static Dispatching

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

## Annotation Extensions

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
@Structural // makes the Widget interface structural (within your project)
public class MyWidgetExtension {
}
```
This extension effectively changes the `abc.Widget` _nominal_ interface to a _structural_ interface. In the context
of your project classes no longer have to declare they implement it nominally. This is particularly desirable when a class
you cannot modify should implement a third-party interface in the context of your application. Making the interface
structural avoids undesirable conventional strategies such as wrappers and proxies, which introduce readability issues
and lose object identity in the process.

See [Structural Interfaces](#structural-interfaces-via-structural) later in this guide for fuller coverage of the topic.

## Extension Interfaces

An extension class can logically add structural interfaces to its extended class.  This feature helps with a variety of
use-cases.
  
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
implement `Hello`.  We can do that with an extension interface:

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
compatible with `Hello`. It's less convenient, though, because you otherwise have to cast `Foo` to `Hello` --
a purely structural relationship in Manifold requires a cast. Basically extension interfaces save you
from casting. This not only improves readability, it also prevents confusion in cases involving type inference where it
may not be obvious that casting is necessary.

It's worth pointing out you can both add an interface _and_ implement its methods by extension:
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
This example extends `Shy` to nominally implement `Hello` _and_ provides the `Hello` implementation. Note the `abstract`
modifier. This is necessary because the extension class can't really implement the interface with static methods, but
the extensions result in the extended class logically implementing the interface.

>You can also use extension interfaces to *extract* interfaces from classes you don't control to, say, expose a safer
> API.


## Extension Libraries

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

*   **Web/JSON**
 
    Defined in module `manifold-json` this library extends:
    - java.net.URL
    - manifold.rt.api.Bindings

> 
> **&#x1f6c8;** **IMPORTANT!**  
> You can create your own custom extension libraries.  There's nothing special about a "library", it's just a normal
> dependency in a project.  However for manifold to recognize extensions, as a performance measure, the library must
> declare it has extensions to process. Do that using the `Contains-Sources` manifest entry.
>
> With **Maven** use the `maven-jar-plugin` to add the `Contains-Sources` manifest entry to your Jar file:
>```xml
><build>
>  <plugins>
>    <plugin>
>      <groupId>org.apache.maven.plugins</groupId>
>      <artifactId>maven-jar-plugin</artifactId>
>      <configuration>
>        <archive>
>          <manifestEntries>
>            <!--class files as source must be available for extension method classes-->
>            <Contains-Sources>java,class</Contains-Sources>
>          </manifestEntries>
>        </archive>
>      </configuration>
>    </plugin>
>  </plugins>
></build>
>```
> Similarly with **Gradle** add the `Contains-Sources` manifest attribute:
>```groovy
>jar {
>  manifest {
>    attributes('Contains-Sources':'java,class')
>  }
>}
>```

## Generating Extension Classes

Sometimes the contents of an extension class reflect metadata from other resources.  In this case rather 
than painstakingly writing such classes by hand it's easier and less error-prone to produce them via 
type manifold.  To facilitate this use-case, your type manifold must implement the `IExtensionClassProducer`
interface so that the `ExtensionManifold` can discover information about the classes your type
manifold produces. For the typical use case your type manifold should extend `AbstractExtensionProducer`.

See the `manifold-ext-producer-sample` module for a sample type manifold implementing `IExtensionClassProvider`.

# Operator Overloading

The Manifold extension framework plugs into Java to provide seamless operator overloading capability. You can
type-safely provide arithmetic, relational, index, and [unit](#unit-expressions) operators for any class by implementing
one or more predefined operator methods. You can implement operator methods directly in your class or use [extension methods](#extension-classes-via-extension)
to implement operators for classes you don't otherwise control. For example, using extension methods Manifold provides
operator implementations for `BigDecimal` so you can write code like this:
```java
BigDecimal result = bigValue1 + bigValue2;
```

>Note the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
and [`manifold-collections`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
projects use operator overloading and unit expressions extensively.

## Arithmetic Operators

Any type can support arithmetic operators by implementing one or more of the following operator methods: 

**Arithmetic**

| Operation | Method       |
|:----------|:-------------|
| `a + b`   | `a.plus(b)`  |
| `a - b`   | `a.minus(b)` |
| `a * b`   | `a.times(b)` |
| `a / b`   | `a.div(b)`   |
| `a % b`   | `a.rem(b)`   |

**Compound assignment**

| Operation | Method           |
|:----------|:-----------------|
| `a += b`  | `a = a.plus(b)`  |
| `a -= b`  | `a = a.minus(b)` |
| `a *= b`  | `a = a.times(b)` |
| `a /= b`  | `a = a.div(b)`   |
| `a %= b`  | `a = a.rem(b)`   |

**Negation**

| Operation | Method           |
|:----------|:-----------------|
| `-a`      | `a.unaryMinus()` |

**Increment and decrement**

| Operation | Method           |
|:----------|:-----------------|
| `a++`     | `a.inc()`        |
| `a--`     | `a.dec()`        |
| `++a`     | `a.inc()`        |
| `--a`     | `a.dec()`        |

Implementations of `inc()` and `dec()` simply return the result of adding or subtracting `a` and "one". The compiler
plugin takes care of both assigning the result to `a` and the particulars regarding prefix and postfix operations. For
instance, `a++` is generated like this:
```java
var temp = a;
a = a.inc();
temp; // result
```                                                                         

>Note, operator methods do not belong to a class or interface you implement. Instead, you implement them *structurally*
>simply by defining a method with the same signature. Note you can implement several versions of the same
>method differing by parameter type. 
>
>Here's a simple example demonstrating how to implement the `+` operator:
>```java
>public class Point {
>  public final int x, y;
>  public Point(int x, int y) {this.x = x; this.y = y;}
>  
>  public Point plus(Point that) {
>    return new Point(x + that.x, y + that.y);
>  }
>}
>
>var a = new Point(1, 2);
>var b = new Point(3, 4);
>
>var sum = a + b; // Point(4, 6)
>```
>
>Since operator methods are structural, you can define *multiple* `plus()` methods:
>```java
>public Point plus(int[] coord) {
>  if(coord.length != 2) {
>    throw new IllegalArgumentException();
>  }
>  return new Point(x + coord[0], y + coord[1]);
>}
>```
   
## Relational Operators

You can implement relational operators using a combination of the `ComparableUsing` and/or `Comparable` interfaces.

### `manifold.ext.rt.api.ComparableUsing`

Relational operators can be implemented all together with the `ComparableUsing` interface, which extends `Comparable`
to provide an operator-specific API.                           
```java
boolean compareToUsing( T that, Operator op );
```
Where `Operator` is an `enum` which specifies constants for relational operators.

| Operation | ComparableUsing Impl      | Comparable Impl       |
|-----------|---------------------------|-----------------------|
| `a > b`   | `a.compareToUsing(b, GT)` | `a.compareTo(b) > 0`  |
| `a >= b`  | `a.compareToUsing(b, GE)` | `a.compareTo(b) >= 0` |
| `a < b`   | `a.compareToUsing(b, LT)` | `a.compareTo(b) < 0`  |
| `a <= b`  | `a.compareToUsing(b, LE)` | `a.compareTo(b) <= 0` |

`ComparableUsing` provides a default implementation for `compareToUsing()` that delegates to `Comparable`'s
`compareTo()` implementation for the `>`, `>=`, `<`, `<=` subset of relational operators.  For the `==` and `!=` subset
`ComparableUsing` delegates to the type's `equals()` method (more on equality later).  This behavior is suitable for
most types, so normally you only need to add `ComparableUsing` to your type's `implements` or `extends` clause and
implement just `Comparable` as you normally would. Thus adding relational operator support to the `Point` example we
have:

```java
public class Point implements ComparableUsing<Point> {
  public final int x, y;
  public Point(int x, int y) {this.x = x; this.y = y;}
  
  public Point plus(Point that) {
    return new Point(x + that.x, y + that.y);
  }
  
  public int compareTo(Point that) {
    return x - that.x;
  }
}
```
Now you can easily compare `Point` values like this:
```java
if (pt1 >= pt2) ...
```

### `java.lang.Comparable`

If you're not interested in supporting `==` and `!=` and your type implements the `Comparable` interface, it
automatically supports the `>`, `>=`, `<`, `<=` subset of relational operators. For example, both `java.lang.String` and
`java.time.LocalDate` implement the `compareTo()` method from `Comparable`, which means they can be used in relational
expressions:

```java
String name1;
String name2;
...
if (name1 > name2) {...}
```   

```java
LocalDate date1;
LocalDate date2;
...
if (date1 > date2) {...}
```

## Equality Operators

To implement the `==` and `!=` subset of relational operators you must implement the `ComparableUsing` interface. By
default `ComparableUsing` delegates to your type's `equals()` method, but you can easily override this behavior by
overriding the `equalityMode()` method in your `CopmarableUsing` implementation. The `EqualityMode` enum provides the
available modes:     

```java
/**
 * The mode indicating the method used to implement {@code ==} and {@code !=} operators.
 */
enum EqualityMode
{
  /** Use the {@code #compareTo()} method to implement `==` and `!=` */
  CompareTo,

  /** Use the {@code equals()} method to implement `==` and `!=` (default) */
  Equals,

  /** Use {@code identity} comparison for `==` and `!=`, note this is the same as Java's normal {@code ==} behavior } */
  Identity
}
```

Based on the `EqualityMode` returned by your implementation of `CompareToUsing#equalityMode()`, the `==` and `!=`
operators compile using the following methods: 

| Operation | `Equals` <small>(default)</small> | `CompareTo`| `Identity` |
|:----------|:-------------------|:--------------------------|:-----------|
| `a == b`  | `a.equals(b)`      | `a.compareToUsing(b, EQ)` | `a == b`   |
| `a != b`  | `!a.equals(b)`     | `a.compareToUsing(b, NE)` | `a != b`   |

Note Manifold generates efficient, **null-safe** code for `==` and `!=`. For example, `a == b` using `Equals` mode
compiles as:
```java
a == b || a != null && b != null && a.equals(b)
``` 

If you need something more customized you can override `compareToUsing()` with your own logic for any of the operators,
including `==` and `!=`.
 
To enable `==` on `Point` more effectively, you can accept the default behavior of `ComparableUsing` and implement
`equals()`:
 
```java
public boolean equals(Object that) {
  return this == that || that != null && getClass() == that.getClass() && 
         x == ((Point)that).x && y == ((Point)that).y;
}
```
>Note always consider implementing `hashCode()` if you implement `equals()`, otherwise your type may not function
>properly when used with `Map` and other data structures:
>```java
>public int hashCode() {
>  return Objects.hash(x, y); 
>}
>```

Sometimes it's better to use the `CompareTo` mode.  For instance, the `==` and `!=` implementations for `Rational`,
`BigDecimal`, and `BigInteger` use the `CompareTo` mode because in those classes `compareTo()` reflects equality in
terms of the *face value* of the number they model e.g., 1.0 == 1.00, which is desirable behavior in many use-cases. As
such override `equalityMode()` to return `CompareTo`:
```java
@Override
public EqualityMode equalityMode() {
  return CompareTo;
}
```

## Index Operator

The index operator can be overloaded to provide more concise syntax for ordered or keyed data structures such as `List`,
`Map`, `CharSequence`, and others. 
 
| Operation   | Method           |
|:------------|:-----------------|
| `a[b]`      | `a.get(b)`       |
| `a[b] = c`  | `a.set(b, c)`    |
  
The indexed assignment expression `a[b] = c` follows the Java language rule that an assignment expression's
value is equal to the assigned value.
```java
var value = a[b] = c;
```
Here `value` is equal to `c`, regardless of what the `set(b, c)` operator method returns. 

>Note, Manifold provides convenient extension methods for indexed access to `List`, `Map`, `String`, and other data
>structures.
>```java          
>String name = "Fred";
>char c = name[0];
> 
>List<String> list = ...;
>String first = list[0];
>
>Map<String, String> map = ...;
>map[key] = value;
>```                           

## Unit Operators

Unit or "binding" operations are unique to the Manifold framework. They provide a powerfully concise syntax and can be
applied to a wide range of applications. You implement the operator with the `prefixBind()` and `postfixBind()` methods:

| Operation  | Postfix Bind       | Prefix Bind       |
|------------|--------------------|-------------------|
| `a b`      | `b.postfixBind(a)` | `a.prefixBind(b)` |

If the type of `a` implements `R prefixBind(B)` where `B` is assignable from the type of `b`, then `a b` compiles as the
method call `a.prefixBind(b)` having type `R`. Otherwise, if the type of `b` implements `R postfixBind(A)` where `A` is
assignable from the type of `a`, then `a b` compiles as the method call `b.postfixBind(a)` having type `R`.

For instance, the unit operator enables concise, type-safe expressions of physical quantities: 
```java
Mass weight = 65 kg;
Length distance = 70 mph * 3.5 hr;
```
Read more about [unit expressions](#unit-expressions) later in this document.
 
## Operators by Extension Methods 

Using [extension methods](#extension-classes-via-extension) you can provide operator implementations for classes you
don't otherwise control. For instance, Manifold provides operator extensions for
[`BigDecimal`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-ext/src/main/java/manifold/ext/extensions/java/math/BigDecimal/ManBigDecimalExt.java)
and [`BigInteger`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-ext/src/main/java/manifold/ext/extensions/java/math/BigInteger/ManBigIntegerExt.java).
These extensions are implemented in the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
dependency.  

Here's what the `+` extension for `BigDecimal` looks like:
```java
@Extension
public abstract class ManBigDecimalExt implements ComparableUsing<BigDecimal> {
  /** Supports binary operator {@code +} */
  public static BigDecimal plus(@This BigDecimal thiz, BigDecimal that) {
    return thiz.add(that);
  }
  ...
}
```
Now you can perform arithmetic and comparisons using operator expressions:
```java
if (bd1 >= bd2) {
  BigDecimal result = bd1 + bd2;
  . . .
}
```

>Note the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
and [`manifold-collections`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
projects use operator overloading and unit expressions extensively.
   

# Unit Expressions
>Warning: **Experimental Feature**

Extending [operator overloading](#operator-overloading) further, Manifold seamlessly plugs into Java to provide Unit
(or *Binding*) Expressions.  This feature is unique to the Manifold framework and provides a powerfully concise syntax
that can be applied to a wide range of applications.

Units are just normal identifiers, you can define them anywhere with fields and local variables and use them directly.
Normally you import predefined unit constants like the ones provided in `UnitConstants` from the
`manifold.science.util` package:
```java
import static manifold.science.util.UnitConstants.kg;
import static manifold.science.util.UnitConstants.hr;
import static manifold.science.util.UnitConstants.mph;
. . .
```
Using imported constants such as `kg` for `Kilogram`, `hr` for `Hour`, `mph` for `Mile/Hour`, etc. you can
begin working with unit expressions:

**Simple and easy to read syntax**
```java
Length distance = 100 mph * 3 hr;
```
**Type-safe**
```java
Force force = 5kg * 9.807 m/s/s; // 49.035 Newtons
```
**Logically equivalent units are equal**
```java
var force = 49.035 kg m/s/s;
force == 49.035 N // true
```
**Maintain integrity with different units**
```java
Mass m = 10 lb + 10 kg; 
```
**Easily make Ranges with the `to` constant from [`RangeFun`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-collections/src/main/java/manifold/collections/api/range/RangeFun.java)**
```java
for( Mass m: 10kg to 100kg ) {...}
```
**Conveniently work with Money**
```java
Money payment = 1.5M USD; 
Money vat = 162k EUR;
Money total = payment + vat; // naturally works with multiple currencies
``` 
>Note unit expressions and *operator overloading* are often used together, read more about [operator overloading](#operator-overloading).

## How does it work?
Normally a *binary* expression in Java and most other languages involves two operands separated by an operator such as
`+` for addition:
```java
int sum = a + b;
```

But with a unit expression the operands are directly adjacent without an operator separating them:
```java
Mass m = 10 kg;
```

The operation is _declared_ in an operand's type with one of the following methods:
```java
public R prefixBind(T rhs);
public R postfixBind(T lhs);
``` 
Where either the left operand defines `prefixBind(T rhs)` or the right operand defines `postfixBind(T lhs)`.

In the example, `10` is a literal value of type `int` and `kg` is a variable of type `MassUnit`. Since `kg` is on the
right-hand side of `10` and the `MassUnit` class defines the method:
```java
public Mass postfixBind(Number magnitude) {...}
``` 
the compiler translates the expression as the method call `kg.postfixBind(10)` resulting in type `Mass`.

Note `postfixBind()` and `prefixBind()` do not belong to a class or interface you implement. Instead you implement
them *structurally* simply by defining a method with the same name, parameter, and non-void return type. This is
necessary because a type may implement multiple versions of the same method. This level of flexibility is otherwise not
supported with Java's name-based type system. 

## Operator Precedence

The empty or "binding" operator has a *phased* precedence. Lexically, its precedence lies between addition and
multiplication, thus during the compiler's parsing phase it produces an untyped AST reflecting this order.  However,
in the course of the compiler's type attribution phase the compiler restructures the AST to reflect binding operator
methods `prefixBind()` and `postfixBind()` declared in the operand types, during which the compiler considers the
binding operator as having a precedence *equal to* multiplication.

To illustrate consider the following expression:
```java
a b * c
```

The binding operator, having a lexical precedence less than multiplication, parses like this:
```java
a (b * c)
```

In a later stage when operand types are available the expression may restructure if:
1. `a` and `b` have a binding relationship declared with `A.postfixBind(B)` or `B.prefixBind(A)` and
2. the type of the resulting `(a b)` expression implements multiplication with the type of `c`
```java
(a b) * c
``` 

For example, the expression `5 kg * 2` reflects this example exactly.

As you can see unit expressions demand a level of flexibility beyond that of conventional compilers such as Java's. But
Java is flexible enough in its architecture so that Manifold can reasonably plug in to augment it with this capability.
    
## Type-safe and Simple

There is nothing special about a unit, it is just a simple expression, most of the time just a variable. You can easily
define your own aliases for units like the ones defined in `manifold.science.util.UnitConstants`.
```java
LengthUnit m = LengthUnit.Meter;
Length twoMeters = 2 m;
``` 

## More Than Units

What makes unit expressions work is simple, just a pair of methods you can implement on any types you like: 
```java
public R postfixBind(T lhs);
public R prefixBind(T rhs);
``` 
If your type implements either of these, it is the basis of a potential "unit" expression. Thus, the application of
these methods goes beyond just units. To illustrate, let's say you want to make date "literal" expressions such as:   
```java
LocalMonthDay d1 = May 15;
LocalYearMonth d2 = 2019 May;
LocalDate d3 = 2019 May 15;
```
Binding expressions easily accommodate this use-case.  Something like:
```java
package com.example;

public enum Month {
  January,
  February,
  March,
  April,
  May,
  ... // etc.
  
  public LocalMonthDay prefixBind(Integer date) {
    return new LocalMonthDay(this, date);
  }
  
  public LocalYearMonth postfixBind(Integer year) {
    return new LocalYearMonth(this, date);
  }
}
```
In turn `LocalYearMonth` can define `LocalDate prefixBind(Integer)`. That's all there is to it. Now you have type-safe
date expressions:
```java
import static com.example.Month.*;
...
LocalDate date = 2019 October 9;
```

Essentially you can implement binding expressions to make use of juxtaposition wherever your imagination takes you.

## Science & Ranges
Of course, as some of the examples illustrate, unit expressions are especially well suited as the basis for a library
modeling physical dimensions such as length, time, mass, etc. Indeed, check out the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
dependency.

Another application of units involves the [Range API](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#ranges)
provided by the [`manifold-collections`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
dependency. Simply by importing the static constants from `RangeFun` you can easily work with ranges:
```java
IntegerRange range = 1 to 5;
```
```java
for (Rational csr: 5.2r to 15.7r step 0.3r) {...}
```
```java
for (Mass mass: 10kg to 100kg unit lb) {...}
```
```java
if ("le matos" inside "a" to "m~") {...}
``` 

# Structural Interfaces via `@Structural`

Java is a _nominally_ typed language -- types are assignable based on the names declared in their definitions. For
example:

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


## Type Assignability and Variance

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

At first glance it looks like `MyCapitalizer` does not satisfy the structure of `Capitalizer`, neither the
parameter type nor the return type of the method match the interface. After careful inspection, however,
it is clear the methods are call-compatible from the perspective of `Capitalizer`:

```java
Capitalizer cap = (Capitalizer) new MyCapitalizer();
CharSequence properName = cap.capitalize("tigers");
```

`MyCapitalizer`'s method can be called with `Capitalizer`'s `String` parameter because `MyCapitalizer`'s
`CharSequence` parameter is assignable from `String` -- _contravariant_ parameter types support
call-compatibility. Similarly we can accept `MyCapitalizer`'s `String` return type because it is
assignable to `Capitalizer`'s `CharSequence` return type -- _covariant_ return types support
call-compatibility. Therefore, even though their method signatures aren't identical, `MyCapitalizer` is
structurally assignable to `Capitalizer` because it is safe to use in terms of `Capitalizer`'s methods.

Signature variance also supports primitive types.  You may have spotted this in the `Component`
class referenced earlier in the `Coordinate` example where `Component.getX()` returns `int`, not `double`
as declared in `Coordinate.getX()`. Because `int` coerces to `double` with no loss of precision
the method is call-compatible. As a result signature variance holds for primitive types as well as
reference types.

## Implementation by Field

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


## Implementation by Extension

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
public abstract class MyVectorExtension implements Coordinate {
  public static double getX(@This Vector thiz) {
    return thiz.getMagnitude() * Math.cos(thiz.getDirection()); 
  }
  public static double getY(@This Vector thiz) {
    return thiz.getMagnitude() * Math.sin(thiz.getDirection()); 
  }
}
```

`Vector` now structurally implements `Coordinate` and can be used with `coordSorter`.

Generally _implementation by extension_ is a powerful technique to provide a common API for classes your 
project does not control.

>Note, if you'd rather not add extension methods to `Vector`, or the extension class strategy is unsuitable for
your use-case e.g., the `Comparable<T>` interface sometimes makes this impossible, you can instead go a more direct
route and implement your own proxy factory...

## Implementation by Proxy

You can provide your own proxy implementations the compiler can use to delegate structural calls. Consider the 
`Coordinate` structural interface earlier.
```java
Coordinate coord = (Coordinate) new Vector(4,5);
double x = coord.getX();
double y = coord.getY();
```
If you don't want to implement an extension class, say because you don't want to add methods to Vector, you can provide
your own proxy implementation ahead of time via the `IProxyFactory` service.
```java
public class Vector_To_Coordinate implements IProxyFactory<Vector, Coordinate> {
  @Override
  public Coordinate proxy(Vector v, Class<Coordinate> cls) {
    return new Proxy(v);
  }

  public static class Proxy implements Coordinate
  {
    private final Vector _delegate;

    public Proxy(Vector v) {
      _delegate = v;
    }

    @Override
    public double getX() {
      return _delegate.getMagnitude() * Math.cos(_delegate.getDirection());
    }
    
    @Override
    public double getY() {
      return _delegate.getMagnitude() * Math.sin(_delegate.getDirection());
    }
  }
}
```
The compiler discovers and uses this proxy factory to make `Vector` calls through `Coordinate`.

Your proxy factory must be registered as a service in `META-INF` directly like so:
```
src
-- main
---- resources
------ META-INF
-------- services
---------- manifold.ext.rt.api.IProxyFactory
```
Following standard Java [ServiceLoader protocol](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html)
you create a text file called `manifold.ext.rt.api.IProxyFactory` in the `service` directory under your `META-INF` directory.
The file should contain the fully qualified name of your proxy factory class (the one that implements `IProxyFactory`) followed
by a new blank line:
```
com.abc.Vector_To_Coordinate

```

### Using `factoryClass`
If you are the declarer of the structural interface, you can skip the Java service and specify a proxy factory
directly in the `@Structural` call site:

```java
@Structural(factoryClass = Vector_To_Coordinate.class)
public interface Coordinate {
  ...
}
```

Manifold inspects the `facotryClass` to see whether it is appropriate for a given proxy.  For instance, from
the super class declaration `IProxyFactory<Vector, Coordinate>` Manifold determines `Vector_To_Coordinate` is exclusively
a proxy factory for `Vector`, other classes go through the default dynamic proxy generation/compilation.  
 
 
## Dynamic Typing with `ICallHandler`

Manifold supports a form of dynamic typing via `manifold.ext.rt.api.ICallHandler`:  

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

# Type-safe Reflection via `@Jailbreak`

Sometimes you have to use Java reflection to access fields, methods, and types that are not directly accessible from
your code. But writing reflection code is not only tedious and error-prone, it also loses type-safety in the process. 
Manifold mitigates these issues with the `@Jailbreak` annotation and the `jailbreak()` extension method.  Use them to
leverage the convenience and type-safety of the Java compiler and let Manifold generate reliable, efficient reflection 
code for you.

## Using `@Jailbreak`

Annotate the type of any variable with `@Jailbreak` to gain direct, type-safe access to private fields, methods, and
types.

>Note, `@Jailbreak` is ideal for use within tests. It saves you from losing type-safety that is otherwise the case with
reflection code and it enables you to maintain private methods and fields.

### Basic Use

```java
@Jailbreak Foo foo = new Foo(1);
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

### Use With Static Members

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

### Use With Types and Constructors

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

### Break JPMS Barriers

Access fields, methods, and constructors from packages otherwise prohibited for use in your module by the JPMS:
```java
jdk.internal.misc. @Jailbreak VM vm = null;
String[] args = vm.getRuntimeArguments();
```
        
## Using the `jailbreak()` Extension

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

# The *Self* Type via `@Self`

The *Self* type is a common term used in the language community to mean *"the subtype of `this`"* and is most useful
in situations where you want a return type or parameter type of a method in a base type to reflect the subtype
i.e., the *invoking* type.  For instance, consider the `equals()` method. We all know it suffers from Java's lack of a
Self type:
```java
public class MyClass {
  @Override
  public boolean equals(Object obj) { // why Object?!
    ...
  }
}

MyClass myClass = new MyClass();
myClass.equals("nope"); // this compiles! :(
```
What we want is to somehow override `equals()` to enforce `MyClass` symmetry:
```java
public boolean equals(MyClass obj) {
  ...
}
```
But Java does not support covariance in parameter types, and for good reason. It would break if we called it like this:
```java
((Object)myClass).equals("notMyClass"); // BOOM! String is not assignable to MyClass
```

Manifold's **`@Self`** type provides an elegant solution:
```java
public boolean equals(@Self Object obj) {
  ...
}
```
Now we have the behavior we want:
```java
MyClass myClass = new MyClass();
myClass.equals("notMyClass"); // Compile Error. YES!!!
```

> Note although Java does not provide a Self type, it does provide some of its capabilities with *recursive generic types*.
But this feature can be difficult to understand and use, and the syntax it imposes is often unsuitable for class
hierarchies and APIs. Additionally, it is ineffective for cases like `equals()` -- it requires that we change the base
class definition! E.g., `public class Object<T extends Object<T>>`... oy!

But as you'll see Manifold's `@Self` annotation altogether removes the need for recursive generics. It provides Java with
a simpler, more versatile alternative.  Use it on method return types, parameter types, and field types to enforce
*"the subtype of `this`"* where suitable.

## Builders

A common use-case for the Self type involves fluent APIs like the *Builder* pattern:

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
  .withWheels(3) // returns VehicleBuilder :(
  .withWings(1)  // ERROR
```

`withWheels()` returns `VehicleBuilder`, not `AirplaneBuilder`.  This is a classic example where we want to return the 
*"the subtype of `this`"*.  This is what the self type accomplishes:

```java
  public @Self VehicleBuilder withWheels(int wheels) {
    _wheels = wheels;
    return this; // returns THIS
  }
```

Now with the return type annotated with `@Self` the example works as desired:

```java
Airplane airplane = new AirplaneBuilder()
  .withWheels(2) // returns AirplaneBuilder :)
  .withWings(1)  // GOOD!
``` 

Annotate with `@Self` to preserve the *"the subtype of `this`"* anywhere on or in a method return type, parameter type,
or field type.

## Self + Generics

You can also use `@Self` to annotate a _type argument_.  A nice example of this involves a typical graph or tree
structure where the nodes in the structure are homogeneous:

```java
public class Node {
  private List<Node> children;
  
  public List<@Self Node> getChildren() {
    return children;
  }

  public void addChild(@Self Node child) {
    children.add(child);
  }
}

public class MyNode extends Node {
  ...
}
```

Here you can make the component type of `List` the Self type so you can use the `getChildren` method type-safely from
subtypes of node:

```java
MyNode myNode = findMyNode();
List<MyNode> = myNode.getChildren(); // wunderbar! 
```

## Self + Extensions

You can use `@Self` with [extension methods](#extension-classes-via-extension) too.  Here we make an extension method as
a means to conveniently chain additions to `Map` while preserving its concrete type:

```java
public static <K,V> @Self Map<K,V> add(@This Map<K,V> thiz, K key, V value) {
  thiz.put(key, value);
  return thiz;
}

HashMap<String, String> map = new HashMap<>()
  .add("nick", "grouper")
  .add("miles", "amberjack");
  .add("alec", "barracuda")
```

## Overriding Methods

Using @Self in a method return type or parameter type has _no_ effect on the method's override characteristics or binary
signature:
```java
public class SinglyNode {
  private @Self SinglyNode next;
  
  public void setNext(@Self SinglyNode next) {
    this.next = next;
  }
}

public class DoublyNode extends SinglyNode {
  private @Self DoublyNode prev;

  public void setNext(@Self SinglyNode next) {
    if(next instanceof DoublyNode) {
      super.setNext(next);
      next.prev = this;
    }
    else {
      throw new IllegalArgumentException();
    }
  }
}
```
Of particular interest is the `@Self SinglyNode` parameter in the `DoublyNode` override. As mentioned earlier Java does not
permit covariant parameter overrides -- we can't override the method using a `DoublyNode` parameter type.  To make up for
this `@Self` informs the compiler that the parameter is indeed a `DoublyNode` when invoked from a `DoublyNode`:
```java
doublyNode.setNext(singlyNode); // Compile Error :)
doublyNode.setNext(doublyNode); // OK :)
```
This is precisely the arrangement we want.  Type-safety is enforced at the call site.  But, equally important, the
subclass handles the parameter as a base class.  Why?  Because this:
```java
((SinglyNode)doublyNode).setNext(singlyNode);
```
Here `setNext()`, although invoked as a `SinglyNode`, dispatches to the `DoublyNode` override.  Thus the `SinglyNode` parameter
type enforces that a `SinglyNode` cannot be mistaken for `DoublyNode`, hence the necessity of the `instanceof` check in
`setNext()`.

# IDE Support 

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access extension methods. Create extension methods using a convenient user interface. Make
changes to your extensions and use the changes immediately, no compilation! Use extensions provided by extension library
dependencies. Find usages of any extension. Use structural interfaces, `@Jailbreak`, `@Self`, etc. Perform rename
refactors to quickly and safely make project-wide changes.

# Setup

## Building this project

The `manifold-ext` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-ext` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 17.

This project consists of two modules:
* `manifold-ext`
* `manifold-ext-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a default scoped dependency on `manifold-ext-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-ext` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 17), the script takes care of the rest.  
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

configurations {
    // give tests access to annotationProcessor dependencies
    testImplementation.extendsFrom annotationProcessor
}

dependencies {
    implementation 'systems.manifold:manifold-ext-rt:2022.1.4'

    testCompile 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-ext', version: '2022.1.4'
}

if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
    sourceSets.main.allJava.files.any {it.name == "module-info.java"}) {
    tasks.withType(JavaCompile) {
        // if you DO define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold', '--module-path', it.classpath.asPath]
    }
} else {
    tasks.withType(JavaCompile) {
        // If you DO NOT define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold']
    }
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyExtProject'
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-ext-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Java Extension App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2022.1.4</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-ext-rt</artifactId>
            <version>${manifold.version}</version>
        </dependency>                                                                                                     
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-ext</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Javadoc agent

See [Javadoc agent](http://manifold.systems/javadoc-agent.html) for details about integrating specific language extensions
with javadoc.


# Javadoc 

`manifold-ext`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-ext/2022.1.4/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-ext/2022.1.4)

`manifold-ext-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-ext-rt/2022.1.4/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-ext-rt/2022.1.4)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
