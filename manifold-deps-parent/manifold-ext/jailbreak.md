# Type-safe Reflection via `@Jailbreak`

>Annotate the type of any variable with `@Jailbreak` to gain direct, type-safe access to private fields, methods, and
types.
>
>```java
>@Jailbreak Foo foo = new Foo(1);
>foo.privateMethod();
>foo.privateMethod("hey");
>foo._privateField = 88;
>```


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
  
  private void callme() {...}
}
```

```java
// casting with hidden type
((com.abc. @Jailbreak SecretClass) foo).callme();
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

[Learn more](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)