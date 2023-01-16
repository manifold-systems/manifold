# Language support for interface composition

>_work in progress_

This is an informal design proposal to add comprehensive language support for interface composition via the manifold project.
I'm mostly using this as a place to collect and refine my thoughts, so I can decide whether this is a good idea. But feel
free to comment on the [feature request](https://github.com/manifold-systems/manifold/issues/413).


## Rationale
Interface composition offers a flexible object-oriented model that a lot of developers prefer over class inheritance, but
with little to no language support it is impractical as a general alternative to inheritance. Specifically, the Java language
does not provide a construct for interface delegation nor does it supply the means for a class to specialize as an interface
delegate. Without these key language elements composition entails reams of error-prone interface implementation boilerplate
and other pitfalls relating to delegation. As a consequence composition has taken a backseat to inheritance in real-world
Java app development.

The main idea here is to use manifold as a foundation for supplementing the Java language with features necessary for
developers to apply interface composition concisely and reliably in their projects.

## Goals

Provide language features necessary so that interface composition is at an equal footing with class inheritance regarding
ease of use, IDE accessibility, etc. Toward that goal these features should:

* eliminate boilerplate delegation code
* provide true delegation (solve the _self_ problem)
* support runtime assignment of delegates (constructor injection etc.)   
* integrate into IntelliJ and Android plugins for manifold


## Interface delegation

Interface delegation is the primary function of the compositional model presented here. It will be integrated as a concise,
declarative language construct so that composing a class with interfaces is natural and straightforward. As such, a class
utilizing this feature is void of boilerplate delegation code.

```java
public interface Sample {
  void jot();
  void ditto();
}

@component class BasicSample implements Sample {
  private final String message;
  
  public BasicSample(String message) {
    this.message = message;
  }
  
  public void jot() { out.println( message ); }
  public void ditto() { jot(); }
}

public class MySample implements Sample {
  @delegate final Sample sample = new BasicSample("composition");
  
  // Sample implementation is handled via @delegate, no boilerplate!
}

MySample sample = new MySample();
ditto.jot();   // prints "composition"

```
* `@component` is a class that implements one or more interfaces for use as a delegate
* `@delegate` is a declared member of a class that delegates interface implementation to a component class instance
                                                               
A class having a field annotated with `@delegate` automatically delegates interface implementation to the field. In the
example the compiler generates the following methods in `MySample`.
```java
  @Override public void jot() {
    sample.jot();
  }
  @Override public void ditto() {
    sample.ditto();
  }
```

If the field's declared type is not an interface, specific interfaces may be provided as parameters to `@delegate`.
```java
@delegate(Sample.class) BasicSample basicSample;
```
Otherwise, the delegated interface is assumed to be the field's declared type.
```java
@delegate Sample sample;
```
Finally, if the field's type is a component type and `@delegate` does not provide interface arguments, the delegated interfaces
are assumed to be the intersection of the component class's interfaces and the delegating class's interfaces.
```java
@delegate BasicSample basicSample;
```
The intersection of `MySample` interfaces and `BasicSample` interfaces is `Sample`. 
                                                                                      
A class annotated with `@component` will be processed to preserve the overriding identity of the delegating class, the _self_.
Details are covered in the following section.

## The self problem
Another critical aspect of delegation concerns the collective identity consisting of the delegating class and its components.
Often called _the self problem_, the delegating class instance and the component class instances have separate identities,
where the delegating class is aware of its delegated components, but not the other way around.

A problem arises when a component class calls an interface method that the delegating class overrides. Since the delegating
class is unknown to the component class, the delegating class's override is ignored.

If `MySample` overrides `jot()` it is never called from `BasicSample#ditto()`.
```java
public class MySample implements Sample {
  @delegate final BasicSample basicSample = new BasicSample("composition");

  @Override
  public void jot() {
    out.println( "hello" );
  }

  // interface delegation is handled via @delegate
}

MySample sample = new MySample();
sample.ditto(); // prints "composition"  YIKES!
```
This example illustrates why simple delegation techniques are ineffective as a serious alternative for class inheritance.
A more practical strategy, called _true delegation_, establishes a collective identity, or _self_, between the delegating
class and its components to enable consistent interface method call dispatching.

Toward achieving true delegation, if a delegating class overrides an interface method, it must _in all cases_ override the
component class implementation, thus **the component class implementation should _never_ be called directly outside the 
delegating class.**

This means a `this` reference within a component class must be context-sensitive. Effectively, `this` must be compiled as
a reference to the delegating class instance when `this` is any of the following: 
* the receiver of an interface method call that is delegated to
* an argument to a method call, unless the parameter type is an interface that is not delegated to, ALL other types require substitution (including Object etc.)  
* a return statement value, ^^ditto regarding the return type

`this` substitutions apply only when a component class is operating as a delegate instance. Otherwise, if the component
class is used as a non-delegate, `this` references remain as-is.

To facilitate substitutions a private `$self` field will be generated on component classes and will be initialized with
a reference to the delegating class instance immediately following assignment to a `@delegate` field.

Self/this substitution is a little more involved than just shoving `$self` in there. Determining whether an interface is
delegated to is a _runtime_ check. So the conditions listed above require something like: `Utils.delegates($self, Foo.class) ? $self : this`.
Some compile-time shortcuts can eliminate the check. For instance, if there is only one interface on the component. Additionally,
the result will be cached by type of `$self` and interface type, to reduce the complexity of the check to constant time.

Immediately following assignment to a `@delegate` field, the component class's `$self` will be set to the delegating class
instance.

Because `$self` is not assigned until after a component is instantiated, constructors will be statically checked to warn
against interface method calls on `this`, otherwise these calls break the override precedence of the delegating class (`$self`). Perhaps
there should be an `initialize()` method all components can override to accommodate such calls? This method would be called
immediately after setting the `$self` field. _todo_

With these changes in hand the previous example works as expected.
```java
sample.ditto(); // prints "hello"
```    

The restrictions detailed here will be enforced by applying compiler errors as necessary.

>Note, private constructors could be generated instead of reflectively setting `$self`, however this strategy assumes the
>components will always be constructed in the context of a `@delegate` declaration. But this is not the case when the delegate
>field is configurable, for example, as a constructor parameters where components are created separately and passed to the
>delegating class.
>```java
>public class MySample implements Sample {
>  @delegate final Sample sample;
>
>  public MySample(Sample sample) {
>    this.sample = sample;
>  }
>}
>```
>In this  case since the component is created separate from the `@delegate` declaration, the `$self` field must be initialized
>directly via reflection.

## Abstract components

Sometimes a component class must leave a portion of its interface methods unimplemented with the intention of having the
delegating class implement them. As such the class must be declared abstract. The problem here is that the delegating
class cannot construct an abstract class, so the component class typically implements the methods as stubs and relies on
documentation for the stubbed methods to be implemented by the delegating class. This inevitably leads to confusing runtime
errors instead of obvious compile-time errors.

An intermediate component class could extend the base component and implement the necessary interface methods to satisfy
the delegating class. However, that step is not obvious and, worse, amounts to an extra layer of clutter that can be avoided.

To address this use-case we will support delegation directly to abstract components. This will be implemented by:
- rewriting `new AbstractComponent();` to an anonymous version: `new AbstractComponent() { missing stubs here }`
- only generating delegate methods in the delegating class for implemented AbstractComponent methods, allowing the compiler
to police the delegating class for unimplemented methods
               
## Delegation with non-components

Since many existing classes may be suitable enough for delegation, but aren't declared with @component. Should they be usable
as virtual components. In this case the delegating class will delegate _specified_ interfaces. Additionally, no attempt
will be made to resolve the self problem as described above because the existing class cannot or should not be processed
for delegation.
```java
public class MyFoo implements Foo {
  @delegate(Foo.class) FooImpl foo = new FooImpl(); // FooImpl is not a @component
}
```
It is unclear at this time whether this use-case should be supported.

## Further boilerplate reduction
Since component class members require different accessibility defaults from normal classes, it may be worthwhile to support
access modifier defaults that are more suitable for component classes. For instance, constructors and interface method
overrides default to `public`, everything else defaults to `private`. One problem, however, is the Java default _package_
access will not be possible. Package access is by far the least needed of the four, perhaps not having to read or write
a ton of access modifiers is a decent trade-off?
