# Language support for composition

>_work in progress_

This is an informal design proposal to add comprehensive language support for interface composition via the manifold project.
I'm mostly using this as a place to collect and refine my thoughts, so I can decide whether this is a good idea. Writing
stuff down has a way of providing more clarity to my tired brain. But if you are not me, and you've stumbled on this, perhaps
you have something to say? Feel free to comment on the [feature request](https://github.com/manifold-systems/manifold/issues/413).


## Rationale
Interface composition offers a flexible object-oriented model that a lot of developers prefer over class inheritance, but
with little to no language support it is impractical as a general alternative to inheritance. Specifically, the Java language
does not provide a construct for interface delegation nor does it supply the means for a class to specialize as an interface
delegate. Without these key language elements composition entails reams of error-prone interface implementation boilerplate
and other pitfalls relating to delegation. As a consequence composition has taken a backseat to inheritance in real-world
Java app development.

Considering Oracle does not appear to have a plan on the table to address this veritable missing link, this proposal
lays out such a plan, albeit as a compiler plugin. The main idea is to provide comprehensive language support for interface
composition using a form of _true delegation_.

## Interface delegation
Interface delegation is the primary function of the compositional model. It should be integrated as a concise, declarative
language construct so that composing a class with interfaces is natural and straightforward. As such, a class utilizing
this feature is void of boilerplate delegation code.

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
  @delegate final BasicSample basicSample = new BasicSample("composition");
  
  // Sample implementation is handled via @delegate, no boilerplate!
}

MySample sample = new MySample();
ditto.jot();   // prints "composition"

```
* `@component` is a class that implements one or more interfaces for use as a delegate
* `@delegate` is a declared member of a class that delegates interface implementation to a component class instance
                                                               
A class having a field annotated with `@delegate` will have the field's interface methods generated such that the implementation
of the methods all delegate to the field.
```java
  @Override public void jot() {
    basicSample.jot();
  }
  @Override public void ditto() {
    basicSample.ditto();
  }
```
All of the interfaces declared on the `@component` class of the field must be declared in the delegating class, either directly
in its implements clause or indirectly via inheritance. This is to say that a component class is _atomic_ -- it may not be
partially delegated to. See [partial delegation](#partial-delegation).

A class annotated with `@component` will be processed to preserve the overriding identity of the delegating class, the _self_.
Details are covered in the following section.

## The self problem
Another critical aspect of compositional design concerns shared identity between the delegating class and the component
class. Often called _the self problem_, the delegating class instance is unknown to the component class instance. While
having separate instances is critical for a compositional design, a problem arises when a component class calls an interface
method that the delegating class overrides. Since delegating class is unknown to the component class, its override is ignored,
thereby breaking the composition contract.

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
This example illustrates why simple delegation techniques are ineffective as a replacement for class inheritance. A more
suitable strategy allows the delegating class and its components to cooperate so that the delegating class can override
behavior _consistently_ across all its components. This behavior is aptly referred to as _true delegation_.

Toward achieving true delegation, if a delegating class overrides an interface method, it must _in all cases_ override the
component class implementation, thus the component class implementation should _never_ be called unless delegated to by
the delegating class.

This means a `this` reference within a component class definition must be context-sensitive. Effectively, `this` is compiled
as a reference to the delegating class instance when `this` is any of the following:
* the receiver of an interface method call
* an argument to a method call 
* a return statement value

`this` replacements apply only when a component class is operating as a delegate instance. Otherwise, if the component class
is used as a non-delegate, `this` references remain as-is. More specifically, `this` will be replaced with `$self` where `$self`
is a generated final field and will reference the delegating class instance it operating in a delegation context, otherwise
it will reference `this`.
                         
The `$self` field will be initialized reflectively. While it is preferable to add a hidden parameter to constructors for
this, the JVM is not amenable to this behavior; instead it would have to be wrangled in the compiler plugin, which is not
worth the effort at this time.

Note, private constructors could be generated instead of reflectively setting the field, however this strategy assumes the components
will always be constructed in the context of a `@delegate` declaration. But this is not the case, for example, when a delegating
class is configurable via constructor parameters where components are created separately and passed to the delegating class.
```java
public class MySample implements Sample {
  @delegate final Sample sample;

  public MySample(Sample sample) {
    this.sample = sample;
  }
}
```
In this  case since the component is created separate from the `@delegate` declaration, the `$self` field must be initialized
directly via reflection.

As a consequence of these restrictions it is impossible for the component class instance to escape the scope of the component
class when used as a delegate. This is, of course, by design so that in a delegate context, beyond interface delegation calls,
a component class instance is never directly invoked.

With these changes in hand the previous example works as expected.
```java
sample.ditto(); // prints "hello"
```    
The restrictions detailed here will be enforced by applying compiler errors as necessary.
   
## Partial delegation?

Should a delegating class be able to delegate only a subset of a component's interfaces? I'll call this _partial delegation_.
```java
public interface Foo {...}
public interface Bar {...}

@component class FooBar implements Foo, Bar {...}

public class MyFoo implements Foo {
  @delegate FooBar foo = new FooBar();  
}
```
`MyFoo` implements `Foo`, but not `Bar`. Yet, it delegates `Foo` to the `FooBar` component, which implements `Foo` and `Bar`.

Part of me says, yes, partial delegation adds more flexibility, therefore it is better. The rest of me says, no, partial
delegation adds more complexity, therefore it is worse.

The complexity muddies the concept of 'self' described earlier. An interface method call within a component class from `this`
would involve testing whether the delegating class, `$self`, delegates the interface to `this`. This complicates the notion
of self and just feels wrong.  

Additionally, saying 'no' to partial delegation, promotes higher cohesion because it forces designers to build more focused
component classes.
* `FooBar` should be designed and used as a `FooBar`, no more, no less
* `MyFoo` must either implement `Foo` itself or define a separate, reusable `Foo` component.

In my judgment, partial delegation is not worth the trouble. The delegation model is simpler without it and naturally promotes
high cohesion by prohibiting it.

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