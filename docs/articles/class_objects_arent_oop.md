# Class is not fully OOP, should it be?

<br>
Ever want Java Class objects to behave more OOP-like? This happens often enough for me that I thought I'd finally jot
down a short "wishlist" with proposed solutions everyone will hate because there are perhaps nicer alternatives. This is
by no means a dig at Java. Quite the opposite, this is just me expressing my curiosity about a language I appreciate and
enjoy using nearly every day. Anyhow, here's my list (and contrived examples).

### Virtual static methods callable from a Class object
     
```java
public int words( Class<? extends Number> cls ) {
  return cls.bits() / 32;  // compile error
}

public class Long extends Number {
  @Override
  public static int bits() {return 2 * 32;}
}
public abstract class Number {
  public static int bits() {return 32;}
}
``` 
The problem with this example is that it implies meta and static features share the same space. For example, the
Class#getName() method prevents a class from defining its own, unrelated getName(). Perhaps add a 'static' qualifier
to fix that.

```java
class Shape {
  // clashes with Class#getName()
  public static String getName() {...}
}

Class<? extends Shape> clazz = ...;
clazz.getName(); // calls Class#getName()
clazz.static.getName(); // calls Shape#getName()

public static Shape create(Class<? extends Shape> clazz) {
  log("Creating a " + clazz.static.getName()); // Class#static qualifies user-defined static features
  ...
}
```

### Define/override abstract static methods

```java
public abstract class Tree {
  public abstract static Image samplePhoto(); // abstract static   
}

public class Maple extends Tree {
  private static final Image PHOTO = loadSamplePhoto();

  @Override
  public static Image samplePhoto() {
    return PHOTO;
  }
  ...
}

List<Class<? extends Tree>> treeClasses = loadTreeCatalog();
...
List<Image> photos = treeClasses.stream().map(c -> c.samplePhoto());
```

### Statically implement an interface
```java
public class Rectangle extends Shape implements static Sides { // note, "implements static"
  @Override // static implementation of Sides
  public static int getSides() {
    return 4;
  }
  ...
}

public interface Sides {
  int getSides();
}

if (clazz implements Sides) {...} // handy

Class<? extends Sides> type = preferredShapeType();
int sides = type.getSides();
```
Perhaps better example of this is an interface as a factory:

```java
public interface TaggedObjectFactory {
  Object create(Tag tag);
  Object create(Tag tag, LocalDateTime timestamp);
}

public class MyObject implements static TaggedObjectFactory {
  @Override
  public static Object create(Tag tag) {
    return new MyObject(tag);
  }
  @Override
  public static Object create(Tag tag, LocalDateTime timestamp) {
    return new MyObject(tag, timestamp);
  }
  ...
}

Class<?> serviceClass = loadServiceClass();
if (serviceClass implements TaggedObjectFactory) {
  service = ((TaggedObjectFactory) serviceClass).create(tag, timestamp);
}
else {
  service = serviceClass.newInstance(); // resort to some type of reflective construction
}
```
>See [@ThisClass](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-ext/README.md#smart-static-methods-with-thisclass)
for an approximation of this feature.

## That's about it
In my view none of these are critical, there are certainly workarounds for all of them. It's just a bit peculiar
to me that Class objects do not already behave this way. Since generics are pretty much required for the practical use
of these features, perhaps it was difficult for the Sun/Oracle guys to wedge this in with generics? Or maybe this would
involve a new bytecode instruction "InvokeStaticVirtual" that was deemed not worth the trouble? Or maybe none of this
was ever considered? In any case I do find myself bumping into this every so often and wonder, why?