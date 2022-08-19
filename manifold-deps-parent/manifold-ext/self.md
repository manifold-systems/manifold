# The *Self* Type with `@Self`

The *Self* type provides a way to _statically_ express the "type of this" and is most useful in situations where a
method return type or parameter type in a base type reflects a subtype.

Consider the case where `equals()` is symmetric, only objects of the declaring class can be equal.  
```java
public class MyClass {
  @Override
  public boolean equals(Object obj) { // Object?!
    ...
    if (!(obj instance MyClass)) // runtime check, should be compile-time check!
    ...
  }
}

MyClass myClass = new MyClass();
myClass.equals("nope"); // sadly, this compiles! :(
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
Now `MyClass` enforces compile-time symmetry. 
```java
MyClass myClass = new MyClass();
myClass.equals("notMyClass"); // Compile Error. YES!!!
```
Note, `equals()` must still guard against asymmetric calls dispatched from base classes.   
```java
((Object)myClass).equals("notMyClass"); // equals still requires an instanceof check 
``` 

## Alternative to recursive generics
Although Java does not provide a Self type, it does provide some of its capabilities with *recursive generic types*.
But this feature is notoriously difficult to understand and use, and the syntax it imposes is often unsuitable for class
hierarchies and APIs. Additionally, it is ineffective for cases like `equals()` -- it otherwise requires `Object` to be
a recursive generic class: `public class Object<T extends Object<T>>`, which pollutes the entire Java class hierarchy.   

The `@Self` annotation provides a simpler, more versatile alternative to most use-cases involving recursive generics.
Use it precisely where it is needed on method return types, parameter types, field types, and generic type arguments.

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
  .withWings(1)  // ERROR!
```

`withWheels()` returns `VehicleBuilder`, not `AirplaneBuilder`.  This is a classic example where we want to statically
express the "type of this" using the Self type:

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

Annotate with `@Self` to statically express the "type of this" in a method return type, parameter type, field type, or
generic type argument.

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
    checkAssignable(this, child);
    children.add(child);
  }
}

public class MyNode extends Node {
  ...
}
```

Here `Node` annotates the component type of `List` with `@Self` so the `getChildren` method is type-safe from subtypes:

```java
MyNode myNode = findMyNode();
List<MyNode> = myNode.getChildren(); // wunderbar! 
```

## Self + Extensions

`@Self` may be used with [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension)
too.  Here an extension method conveniently enables call chaining to `Map` while preserving its static subtype:

```java
public static <K,V> @Self Map<K,V> add(@This Map<K,V> thiz, K key, V value) {
  thiz.put(key, value);
  return thiz; // returns Self for type-safe call chaining 
}

HashMap<String, String> map = new HashMap<>() 
  .add("nick", "grouper") // chain calls to add()
  .add("miles", "amberjack");
  .add("alec", "barracuda"); // preserves HashMap type
```

[Learn more](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)