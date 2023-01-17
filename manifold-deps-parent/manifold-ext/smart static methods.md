Occasionally, albeit mostly for generated code, I run into cases where "smart" static methods would be helpful. Most recently
to access empty arrays.

With the Java language it's common to return an empty array as a default value to avoid returning null.
```java
interface Foo {
  default String[] brands() { return new String[0]; }
}
```
But we don't want to construct a new empty array each time the method is called, instead we create a constant.
```java
interface Foo {
  String[] EMPTY_STRING_ARRAY = new String[0];

  default String[] brands() { return EMPTY_STRING_ARRAY; }
}
```
But we also don't want to have multiple versions of the same empty array, so we put this constant where it can be reused.
```java
public class EmptyArrays {
  public static final String[] EMPTY_STRING_ARRAY = new String[0];
}

interface Foo {
  default String[] brands() { return EmptyArrays.EMPTY_STRING_ARRAY; }
}
```
This is a common Java idiom.

The code I _want_ to write:
```java
interface Foo {
  default String[] brands() { return String.emptyArray(); }
}
```
Where `emptyArray()` is a "smart" static method on `Object`. Something like:
```java
public class Object {
  public static This[] emptyArray() {
    return new This[0]; // cached in real-life
  }
}
```
The imagined `This` is the static counterpart to `this` and can be used as a type reference, much like a generic type variable
with the added bonus of reification.

Along the same lines virtual static methods could be useful too. Just a thought.

>See [extension classes](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext) from
the manifold project for access to experimental features like this for Java.