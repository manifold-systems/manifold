# Java class objects aren't OOP, should they be?

<small>Scott McKinney</small>
<br>

At times, I've wanted more from Java class objects than is available. This happens often enough that I thought I'd
jot down some details concerning where I think they lack. Generally, I suppose the list argues for better OOP support
with Class objects.

In many cases there are workarounds that offer perhaps a better alternative. Or are we just conditioned to think this
way?

### Why '.class' when assigning/passing a class literal as a Class?
A minor nitpick.                                                            
```java
Class clazz = MyClass; // compile error, needs MyClass.class
```

### Can't call a static method from a Class object
     
```java
public int words( Class<? extends Number> cls ) {
  return cls.bits() / 32;  // compile error
}

public static class Long extends Number {
  public static int bits() {return 2 * 32;}  // see virtual class methods below
}
public abstract class Number {
  public static int bits() {return 32;}
}
``` 
This shared namespace presents ambiguity between Class and user-defined static members. But a
simple compromise could fix that:

```java
class MyClass {
  // clashes with Class#getName()
  public static String getName() {...}
}
Class<MyClass> clazz = MyClass.class;
clazz.static.getName(); // Class#static qualifies user-defined static features
```
### Can't define/override abstract static methods (static can't be virtual)

```java
public abstract class Tree {
  public abstract static Image samplePhoto();    
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
List<Image> photos = treeClasses.stream().map( c -> c.samplePhoto() );
```

### Can't statically implement an interface (no virtual class methods)
```java
public class Integer implements static Bounds { // note, "implements static"
  @Override
  public static double getMaxValue() {
    return MAX_VALUE;
  }
  ...
}

public interface Bounds {
  double getMaxValue();
  double getMinValue();
}

if( clazz implements Bounds ) {...}

Class<? extends Bounds> type = preferredNumberType();
Range range = new Range( type.getMinValue(), type.getMaxValue() );
```
Maybe better example of this is an interface as a factory:

```java
public interface TaggedObjectFactory {
  Object create( Tag tag );
  Object create( Tag tag, LocalDateTime timestamp );
}

public class MyObject implements static TaggedObjectFactory {
  @Override
  public static Object create( Tag tag ) {
    return new MyObject( tag );
  }
  @Override
  public static Object create( Tag tag, LocalDateTime timestamp ) {
    return new MyObject( tag, timestamp );
  }
  ...
}

Class serviceClass = loadServiceClass();
if( serviceClass implements TaggedObjectFactory ) {
  service = ((TaggedObjectFactory)serviceClass).create( tag, timestamp );
}
else {
  service = serviceClass.newInstance(); // resort to some type of reflective construction
}
```

## That's about it
In my view none of this stuff is critical. There are workarounds for all of it, most of them perhaps better than my
sketchy proposals. Maybe I wrote this list simply to offload it? Anyhow, it felt good, like throwing away junk in your
garage.

Feel free to discuss this and similar ideas on the [Manifold slack channel](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg).