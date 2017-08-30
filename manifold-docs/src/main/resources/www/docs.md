## Manifold in a Nutshell

At its core Manifold is a unique framework for dynamically and transparently extending Java's type system.

Building on this framework Manifold provides constructive features we see in other languages and platforms 
like:

* **Extension Methods**
* **Metaprogramming**
* **Structural Typing**

Leveraging these features Manifold delivers a powerful set of Java extensions including **JSON** 
integration, transparent **JavaScript** accessibility, **Structural typing**, extensions to Java's 
runtime classes, and type-safe access to raw **SQL**.

At a high level these features are classified as **Extension Manifolds** and **Type Manifolds**.

### Extension Manifolds

Extension Manifolds let you augment existing Java classes including Java's own runtime classes such as 
`String`. You can add new methods, annotations, and interfaces to any class your project uses.

Let's say you want to make a new method for `String` so you can easily echo the string's value to the 
console. With plain Java you might write a "Util" library like this:

<pre class="prettyprint lang-java"> public class MyStringUtil {
    public static void echo(String value) {
      System.out.println(value);
    }
  }</pre>

And you'd use it like this:

<pre class="prettyprint lang-java">  MyStringUtil.echo("Manifold");</pre>

Instead with Manifold you create an _**Extension Class**_:

<pre class="prettyprint">  @Extension
  public class MyStringExtension {
    public static void echo(@This String thiz) {
      System.out.println(thiz);
    }
  }</pre>

Here we've added a new `echo()` method to `String`, so we use it like this:

<pre class="prettyprint">  "Manifold".ehco();</pre>

This is a powerful feature. Extensions eliminate a lot of intermediate code in the form of "Util" libraries
and the like. They also clean up code by making it easier to read and write since the methods are object 
oriented. Arguably the most beneficial aspect of extensions is how much easier it is to discover and use 
them in your IDE:

##todo: screenshot of code completion showing the echo() method on String

There's a lot more to extensions including the addition of **Structural Interfaces** similar to types in 
the [Go language](https://golang.org/). We'll cover more later in this guide.

### Type Manifolds

Type Manifolds simplify and standardize type-safe access to a variety of data sources. For instance, 
normally you access Java properties resources like this:

<pre class="prettyprint">  Properties myProperties = new Properties();
  myProperties.load( getClass().getResourceAsStream( "/abc/MyProperties.properties" ) );
  String myMessage = myProperties.getProperty( "my.message" );</pre>

This is a lot of code, but since properties files are foreign to Java's type system there is no direct, 
type-safe access to them...

Until now:

<pre class="prettyprint">  String myMessage = MyProperties.my.message;</pre>

Concise and type-safe!

Any type of data source accessible to Java is a potential Type Manifold. These include resource files, 
schemas, queries, database definitions, templates, spreadsheets, programming languages, etc. Currently 
Manifold supports:

*   JSON and JSON Schema
*   JavaScript
*   Properties resource files
*   Image resource files
*   BareBones templates (*)
*   DDL and SQL (*)

### Benefits

Manifold's core technology is a dramatic departure from conventional Java tooling. There is no code 
generation step in the build, no extra .class files to manage, no annotation processors, and no extra 
class loaders engaged at runtime.

Benefits of this approach include:

*   **Zero turnaround** -- live, type-safe access to data; make, discover, and use changes instantly
*   **Lightweight**, direct integration with standard Java, requires no special compilers, annotation 
processors, or runtime agents
*   **Efficient, dynamic** -- Manifold only produces types as they are needed
*   **Simple, open API** -- you can build your own Manifolds
*   **No code generation step**
*   **Rich IDE experience** with IntelliJ IDEA

Additionally, Manifold is just a JAR file you can drop into your existing project -- you can begin using 
it incrementally without having to rewrite classes or conform to a new way of doing things.

## Setup

blah blah blah

## Extension Classes

Similar to other languages such as C# and Kotlin, with Manifold you can define methods and other features 
as logical extensions to existing Java classes. This is achieved using _extension classes_. An extension 
class is a normal Java class you define as a container for features you want to apply to another class, 
normally to one you can't modify directly, such as `java.lang.String`:

<pre class="prettyprint">// package format: extensions.<qualified-name-of-extended-class>
package extensions.java.lang.String;

import manifold.ext.api.*;

@Extension // identifies this class as an extension class
public class MyStringExtension {

  // Extension methods must be static and non-private
  //
  // For an instance method the receiver parameter must have the same
  // type as the extended class and must be annotated with @This
  //
  public static void echo(@This String thiz) {
    System.out.println(thiz);
  }

  @Extension // identifies a static extension method
  public static String lineSeparator() {
    return System.lineSeparator();
  }
}
</pre>

All extension classes must be rooted in the `extensions` package. In addition the remainder of the package 
name must be the qualified name of the extended class. Thus, as the example illustrates, an extension 
class on `java.lang.String` must reside directly in package `extensions.java.lang.String`. Note this 
convention facilitates fast access to extension classes and avoids the overhead and ceremony of 
alternative means such as annotation processors.

In addition an extension class must be annotated with `manifold.ext.api.Extension`, which distinguishes 
extension classes from other classes that may reside in the same package.

### <a class="anchor" id="extensions-basics"></a>Extension Method Basics

An extension method must be declared `static` and non-`private`. As the receiver of the call, the first 
parameter of an extension _instance_ method must have the same type as the extended class. The 
`MyStringExtension` example illustrates this; the first parameter of instance method `echo` is 
`java.lang.String`. In addition the parameter must be annotated with `manifold.ext.api.This` to distuish 
it from regular methods in the class.

That's all there is to it. You can use extensions just like normal methods on the extended class:

<pre class="prettyprint">String name = "Manifold";
name.echo();</pre>

You can also define `static` extension methods. Since static methods don't have a receiver, the method 
itself must be annotated with `manifold.ext.api.Extension`:

<pre class="prettyprint">@Extension
public static String lineSeparator() {
  return System.lineSeparator();
}</pre>

Call static extensions just as if they were on the extended class:

<pre class="prettyprint">String.lineSeparator()</pre>

### <a class="anchor" id="extensions-generics"></a>Generics

You can extend generic classes too and define generic methods. This is how Manifold extension libraries 
work with collections and other generic classes. For example, here is the `first()` extension method on 
`Iterable`:

<pre class="prettyprint">public static <T> T first(@This Iterable<T> thiz, Predicate<T> predicate) {
  for (T element: thiz) {
    if (predicate.test(element)) {
      return element;
    }
  }
  throw new NoSuchElementException();
}</pre>

Notice the method is implemented as a generic method with the same type variable designation as the 
extended class: `T`. Since extension methods are static this is how we convey type variables from the 
extended class to the extension method. Note type variable names, must match the extended type's type 
variables and must be declared in the same order.

To define a generic extension method you append the type variables of the method to the list of the 
extended class' type variables. Manifold's `map()` extension illustrates this format:

<pre class="prettyprint">public static <E, R> Stream<R> map(@This Collection<E> thiz, Function<? super E, R> mapper) {
  return thiz.stream().map(mapper);
}</pre>

Here `map` is a generic extension method having type variable `R` and conveying `Collection`'s type 
variable `E`.

### <a class="anchor" id="extensions-static"></a>Static Dispatching

An extension class does not physically alter its extended class; the methods defined in an extension are 
not really inserted into the extended class. Instead the Java compiler and Manifold cooperate to make a 
call to a static method in an extension look like an instance method call on the extended class. As a 
consequence extension calls dispatch **statically**.

So unlike a virtual method call an extension call is always made based on the extended type declared in 
the extension, not the runtime type of the left hand side of the call. To illustrate:

<pre class="prettyprint">public class Tree {
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
tree.bark();</pre>

At compile-time `tree` is of type `Tree`, therefore it transforms to a static invocation of `bark(Tree)`, 
which prints "rough".

Another consequence of static dispatching is that an extension method can receive a call even if the value 
of the extended object is null at the call site. Manifold extension libraries exploit this feature to 
improve readability and null-safety. For example, `isNullOrEmpty()` on `CharSequence` compares the 
receiver's value to null so you don't have to:

<pre class="prettyprint">public static boolean isNullOrEmpty(@This CharSequence thiz) {
  return thiz == null || thiz.length() == 0;
}

String name = null;
if (name.isNullOrEmpty()) {
  println("empty");
}
</pre>

Here the example doesn't check for null and instead shifts the burden to the extension.

### <a class="anchor" id="extensions-accessibility"></a>Accessibility and Scope

An extension method never shadows or overrides a class method; when an extension method's name and 
parameters match a class method, the class method always has precedence over the extension. For example:

<pre class="prettyprint">public class Tree {
  public void kind() {
    println("evergreen");
  }
}

public static void kind(@This Tree thiz) {
  println("binary");
}</pre>

The extension method never wins, a call to `kind()` always prints "evergreen". Additionally, if at 
compile-time `Tree` and the extension conflict as in the example, the compiler warns of the conflict 
in the extension class.

An extension method may overload a class method where the method names are the same, but the parameter 
types are different:

<pre class="prettyprint">public class Tree {
  public void harvest() {
    println("nuts");
  }
}

public static void harvest(@This Tree thiz, boolean all) {
  println(all ? "wood" : thiz.harvest());
}</pre>

A call to `tree.harvest(true)` prints "wood".

Since extension method references resolve at compile-time, you can limit the compile-time accessibility 
of an extension class simply by limiting the scope of the JAR file containing the extension. For example, 
if you're using Maven the scope of an extension matches the dependency relationship you assign in your 
pom.xml file. Similarly in module-aware IDEs such as IntelliJ IDEA, an extension's scope is the same as 
the module's.

### Annotation Extensions

Extension classes are not limited to extension methods. Other features can be projected onto the extended 
class, these include _annotations_. At present annotation extensions are limited to the extended class; 
you can't yet add annotations to members of the extended class.

Annotations added to an extension class are logically applied to the extended class. However, it should 
be noted as with all Manifold features the effects are compile-time manifestations; extension annotations 
are limited to a compile-time existence. Therefore, even if an annotation has `RUNTIME` retention, it will 
only be accessible on the extended class at compile-time. This feature is most useful when using 
annotation processors and you need to annotate classes you otherwise can't modify.

### Extension Libraries

An extension library is a logical grouping of functionality defined by a set of extension classes. 
Manifold includes several useful extension libraries for commonly used classes.

*   **Collections**

    examples...

*   **Text**

    examples...

*   **I/O**

    examples...

*   **Concurrent**

    examples...

*   **Web**

    examples...

## Structural Interfaces

Java is a _nominally_ typed language, meaning types are assignable based on the names declared in their 
type definitions. For example:

<pre class="prettyprint">
public class Foo {
  public void hello() {
    println("hello");
  }
}

public interface Greeting {
  void hello();
}

Greeting foo = new Foo(); // error
</pre>

This does not compile because `Foo` does not explicitly implement `Greeting` by name in its `implements` 
clause.

By contrast a structurally typed language has no problem with this example.  Basically, structural typing
requires only that a class implement interface _methods_, there is no need for a class to declare that it 
implements an interface.

Although nominal typing is arguably more sound and easier for both people and machines to digest, in some 
circumstances the flexibility of structural can be more suitable. Take the following classes:

<pre class="prettyprint">
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
</pre>

Let's say we're tasked with sorting instances of these according to location in the coordinate plane, say 
as a `Comparator` implementation. Each class defines methods for obtaining X, Y coordinates, but these 
classes don't implement a common interface. We don't control the implementation of the classes, so we're 
faced with having to write three distinct, yet nearly identical, Comparators.

This is where we the flexibility of structural interface could really help. If Java supported it, we'd 
declare a structural type with the `getX()` and` getY()` methods and write only one `Comparator`:

<pre class="prettyprint">
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
</pre>

Of course Java is not happy with this because `List<Point>` is not a `List<Coordinate>`

This is where Manifold steps in with structural interfaces:

<pre class="prettyprint">
@Structural
public interface Coordinate {
  double getX();
  double getY();
}
</pre>

Adding `@Structural` to `Coordinate` effectively changes it to behave _structurally_ -- Java no longer 
requires classes to implement it by name, only its methods must be implemented.

###Variance

Essentially a type is assignable to a structural inteface if it provides compatible versions of all the 
methods declared in the interface. The use of the term _compatible_ here instead of _identical_ is 
deliberate. The looser term concerns the notion that a structural inteface method is variant with respect 
to the types in it’s signature. Specifically its parameter types are contravariant and its return type is 
covariant. For example:

<pre class="prettyprint">
@Structural
public interface Capitalizer {
  CharSequence capitalize(String s);
}

public static class MyCapitalizer {
  public String capitalize(CharSequence s) {
    return s.isNullOrEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
</pre>

At first glance it looks like `MyCapitaizer` does not satisfy the structure of `Capitalizer`, neither the 
parameter type nor the return type of the method match the ineterface. After careful inspection, however, 
it is clear the methods are call-compatible from the perspective of `Capitalizer`:

<pre class="prettyprint">
Capitalizer cap = (Capitalizer) new MyCapitalizer();
CharSequence properName = cap.capitalize("tigers");
</pre>

`MyCapitalizer`’s method can be called with `Capitalizer`’s `String` parameter because `MyCapitalizer`’s 
`CharSequence` parameter is assignable from `String` – _contravariant_ parameter types support 
call-compatibility. Similarly we can accept `MyCapitaizer`’s `String` return type because it is 
assignable to `Capitalizer`’s `CharSequence` return type – _covariant_ return types support 
call-compatibility. Therefore, even though their method signatures aren’t identical, `MyCapitalizer` is 
structurally assignable to `Capitalizer` because it is safe to use in terms of `Capitalizer`’s methods.

Note signature variance also involves primitive types.  You may have spotted this in the `Component` 
class referenced earlier in the `Coordinate` example where `Component.getX()` returns `int`, not `double` 
as declared in `Coordinate.getX()`. Because `int` coerces to `double` with no loss of precision 
the method is call-compatible. As a result signature variance holds for primitive types as well as 
reference types.

###Implementation by Field

Call-compatibility is not limited to methods.  A class can implement a structural interface property 
method with a public field:

<pre class="prettyprint">
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
</pre>

Basically a field can serve as a property method if it matches the property method's declared name, minus
the get/set prefixes. Note a field can match a property method in different ways.  For example, `Person`'s 
field could be any of the following: `name`, `_name`, or `Name`.

###Implementation by Extension


 
###Dynamic Typing

blah blah


##todo: cover details about type assignability and other stuff

## Type Manifolds

fuckin' manifolds, how do they work?

