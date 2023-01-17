Occasionally, albeit mostly for generated code, I run into cases where "smart" static methods would be helpful. Most recently to access empty arrays.

For instance, it's common to return an empty array as a default value, instead of returning null.
```java
interface Foo {
  default String[] brands() { return new String[0]; }
}
```
We don't want to construct a new empty array each time the method is called, instead we create a constant.
```java
interface Foo {
  String[] EMPTY_STRING_ARRAY = new String[0];

  default String[] brands() { return EMPTY_STRING_ARRAY; }
}
```
This is a common Java idiom.

The code I want to type:
```java
interface Foo {
  default String[] brands() { return String.emptyArray(); }
}
```
Where `emptyArray()` is a "smart" static method on `Object`. Something like:
```java
public class Object {
  public static This[] emptyArray() {
    return new This[0];
  }
}
```
`This` is the static counterpart to `this` and can be used as a type reference, much like a generic type variable, as if `Object` were recursive.
```
public class Object<This extends Object<This>> { ... }
```
With the added bonus of a reified `This`.

Along the same lines virtual static methods could be useful too. Just a thought.

> See [`@ThisClass`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext) for an experimental version of smart static methods for Java.