# Finally, Properties for Java

Java has done fairly well balancing stability with demand for new features. One longstanding hole, however, is the
absence of a _property_ language construct. Properties eliminate boilerplate code otherwise present in the form of
Java's conventional getter/setter methods. Perhaps more importantly, the feature formalizes a property as a single
declared entity; it is accessed and assigned by name alone. Sadly, properties don't appear to be on Java's roadmap for
the foreseeable future.

That's fine, because the new `manifold-props` compiler plugin from the Manifold project offers a comprehensive solution.
Incorporate `manifold-props` into your build to begin using properties like this:
```java
public interface Book {
  @var String title; // no more boilerplate code!
}
public class BookImpl implements Book {
  @override @var String title; // provides private backing field, public getter, and public setter
}
// refer to it directly by name
book.title = "Daisy";     // calls setter
String name = book.title; // calls getter 
book.title += " chain";   // calls getter & setter
```
                                                           
## @var, @val, and @set

Properties come in three flavors: 
* `@var`: read-write, may be accessed and assigned 
* `@val`: read-only, may be accessed, but not assigned
* `@set`: write-only, may be assigned, but not accessed
                            
All properties are `public` by default and may be declared with other modifiers as you would apply them to getter/setter
methods. Additionally, they provide parameters to add arbitrary annotations to the provided getter/setter methods and
parameters.

The full syntax for declaring a `@var` property:

```bnf
[modifiers] @var[(<options>)] [<@get(<options>)>] [<@set(<options>)>] <type> <name> [= <initializer>];
[<getter>]
[<setter>]
```

This example illustrates how `protected`, `abstract`, and `final` can be used to configure properties:
```java 
public abstract class Account {
  final @var String name;
  protected abstract @var int rate;
}
```
Compiles as:
```java 
public abstract class Account {
  private String name;

  public final String getName() {return name;}
  public final void setName(String value) {this.name = value;}

  protected abstract int getRate();
  protected abstract void setRate(int value);
}
```

This example demonstrates how to apply annotations to a property and how to specify stricter visibility for the setter.  
```java
// public `name` property adds @MyAnnotation to generated getter/setter methods and makes the setter `protected`
@var(annos=@MyAnnotation) @set(Protected) String name;
```

## Inferred Properties

Additionally, the feature automatically _**infers**_ properties, both from your existing source files and from
compiled classes your project uses. Reduce property use from this:
```java
Actor person = result.getMovie().getLeadingRole().getActor();
Likes likes = person.getLikes();
likes.setCount(likes.getCount() + 1);
```
to this:
```java
result.movie.leadingRole.actor.likes.count++;
``` 

## IDE Support
                                    

Properties are fully integrated into both **IntelliJ IDEA** and **Android Studio**. Use the IDE's features to create new
properties, verify property references, access properties with code completion, quickly navigate to/from declarations,
and more.
<p><img src="http://manifold.systems/images/properties.png" alt="properties" width="50%" height="50%"/></p>

## More

There's more to learn about `manifold-props` and the Manifold project in general. I hope I've covered enough ground here
to pique your interest. Check it out at manifold.systems.