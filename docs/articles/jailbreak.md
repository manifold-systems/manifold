# Type-safe Reflection with `@Jailbreak`

Ever overexpose fields and methods just so they can be accessed from tests? Ever write reflection code to access
private class members? You can stop doing that now. Maintain integrity and type-safety with `@Jailbreak` from the
[Manifold project](https://github.com/manifold-systems/manifold).

```java
@Jailbreak MyWidget widget = new MyWidget();
String value = widget.privateField; // type-safely access private fields and methods 
```

## The Problem with Reflection

We're not supposed use reflection to access or modify otherwise inaccessible class members, right?  Well, that depends on the
alternatives. There are situations where reflection is the lesser of two evils or the difference between winning customers
and losing them. The fact is reflection is an indispensable JVM feature we all benefit from.  But if you still feel
uneasy about using it, that's a good sign!  Because reflection code is itself nasty stuff primarily because it totally
escapes **type-safety**.

Let's say you need brand information from `MyWidget`:
```java
public class MyWidget implements Widget {
  private String brand;
  ...
}
```  

Since `brand` is private and `MyWidget` is not your class to modify, you must resort to reflection:
```java
MyWidget myWidget = (MyWidget) widget;
...
Field field = MyWidget.class.getDeclaredField("brand");
field.setAccessible(true);
String brand = (String) field.getValue(myWidget);
```

Its use of Strings and casting make reflection not just difficult to read and error prone to write, but worse it escapes
type-safety. The compiler can neither verify your access to the `brand` field nor its `String` type.  If `MyWidget` 2.0
changes any of this, your build will not detect the break. Thus the real trouble with reflection code is that it is
statically unverifiable. 


## `@Jailbreak`

Given the difficulty reading and writing reflection code and its lack of type-safety, there should be a better way. Why
not provide a language construct to declare the *intent* to subvert encapsulation and then leverage the compiler's
strengths to verify your code.  This is precisely what the `@Jailbreak` annotation achieves:

```java
@Jailbreak MyWidget myWidget = (MyWidget) widget;
...
String brand = myWidget.brand;
```

As you can see `@Jailbreak` lets you access otherwise inaccessible members of `MyWidget` from the `myWidget` variable.

`@Jailbreak` mitigates the issues with reflection.  Use it to leverage the convenience and type-safety of the Java
compiler and let Manifold generate reliable, efficient reflection code for you.

## Basic Use

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

## Using the `jailbreak()` Extension

Similar to `@Jailbreak` you can call the `jailbreak()` [extension method](http://manifold.systems/docs.html#extension-classes)
from any expression to gain type-safe access to private fields, methods, and types.

```java
Foo foo = new Foo();
foo.jailbreak().privateMethodOnFoo();
```
This method is especially handy when you have a chain of member access expressions and you want to use them concisely:

```java
something.foo().jailbreak().bar.jailbreak().baz = value;
``` 

>Note reflection code is also horribly inefficient if not properly cached and accessed. Manifold fixes all that too.

## IntelliJ IDEA

Use `@Jailbreak` with the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA. The
plugin provides comprehensive support for `@Jailbreak` including code completion, navigation, usage searching, etc.

Visit the [Manifold project](https://github.com/manifold-systems/manifold) on github to learn more.