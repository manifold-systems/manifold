_work in progress_

This is a proposal to add comprehensive language support for composition.

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
  @delegate final Sample basicSample = new BasicSample("composition");
  
  // Sample implementation is handled via @delegate, no boilerplate!
}

MySample sample = new MySample();
ditto.jot();   // prints "composition"

```
* `@component` is a class that implements one or more interfaces strictly for use as a delegate
* `@delegate` is a declared member of a class that delegates interface implementation to a component class instance
                                                               
A class having a field annotated with `@delegate` will have the corresponding interface methods generated where the implementation
of the methods all delegate to the field.

A class annotated with `@component` is subject to restrictions detailed in the following section. These mostly concern
preserving the overriding identity of the delegating class when control is passed to a component class.

## The self problem
Another critical aspect of compositional design concerns shared identity between the delegating class and the component
class, or lack thereof. Often called _the self problem_, the delegating class instance and the component class instances
have separate identities; their `this` references are all different.  While this is critical for a compositional design,
a problem arises when a component class calls an interface method where the delegating class overrides the method. Since
their identities are separate and the component class knows nothing of the delegating class, its override is never called,
thereby breaking the composition contract.

Unremedied, if `MySample` overrides `jot()` it is never called from `BasicSample#ditto()`.
```java
public class MySample implements Sample {
  @delegate final Sample basicSample = new BasicSample("composition");

  @Override
  public void jot() {
    out.println( "hello" );
  }

  // interface delegation is handled via @delegate
}

MySample sample = new MySample();
sample.ditto(); // prints "composition"  YIKES!
```
This example illustrates why language support for delegation should not only delegate from the delegating class to the component,
but also from the component back to the delegating class. This behavior is aptly referred to as _true delegation_.

Thus, toward achieving true delegation, if a delegating class overrides an interface method, it must _in all cases_ override
the component class implementation; the component class implementation should never be called unless delegated to by the
delegating class.

This means a `this` reference within a component class definition must be context-sensitive. Effectively, `this` is compiled
as a reference to the delegating class instance when `this` is:
* the receiver of an interface method call
* an argument to a method call
* a return statement value

`this` replacements apply only when a component class is operating as a delegate instance i.e., when its constructor is
invoked from a delegate member or as a super call from a subcomponent. Otherwise, if the component class is constructed
as a non-delegate, `this` references remain as-is. More specifically, `this` will be replaced with `$self` where `$self`
is a generated final field and will reference the delegating class instance if non-null, otherwise it will reference `this`.
                         
To initialize `$self` another set of private constructors will be generated in the component class reflecting its declared
constructors, but the new constructors will have an additional parameter prepended to pass the delegating class instance.
Accordingly, constructor call sites will be rewritten to prepend the calling delegating class instance argument, or null
if the component class is not constructed within a delegate context. Note, the since the new constructors are necessarily
private they will be invoked internally via method handles.

As a consequence of these restrictions it is impossible for the component class instance to escape the scope of the component
class when used as a delegate. This is, of course, by design so that in a delegate context, beyond interface delegation calls,
a component class instance is never directly invoked.

With these changes in hand the previous example works as expected.
```java
sample.ditto(); // prints "hello"
```    
The restrictions detailed here will be enforced by applying compiler errors as necessary.

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

## Further boilerplate reduction
Since component class members require different accessibility defaults from normal classes, it may be worthwhile to support
access modifier defaults that are more suitable for component classes. For instance, constructors and interface method
overrides default to `public`, everything else defaults to `private`. One problem, however, is the Java default _package_
access will not be possible. Package access is by far the least needed of the four, perhaps not having to read or write
a ton of access modifiers is a decent trade-off? 