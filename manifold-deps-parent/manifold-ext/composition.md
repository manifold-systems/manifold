_work in progress_

This is a proposal to add comprehensive language support for composition.

## Rationale
Interface composition offers a flexible object-oriented model that a lot of developers prefer over class inheritance, but
with little to no language support it is impractical as a general alternative to inheritance. Specifically, the Java language
does not provide a construct for interface delegation nor does it supply the means for a class to specialize as an interface
delegate. Without these key language elements composition entails reams of error-prone boilerplate to manage interface delegation.
As a consequence composition has taken a backseat to inheritance in real-world Java app development. Therefore, adding
language support to Java will at least provide equal footing in terms of accessibility.

## Interface delegation
Interface delegation is the primary function of the compositional model. It should be integrated as a concise, declarative
language construct so that composing a class with interfaces is natural and straightforward. As such, a class utilizing
this new construct is void of boilerplate delegation code.

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
  @delegate final Sample basicSample;
  
  public MySample(Sample sample) {
    basicSample = sample;
  }  
  
  // interface delegation is handled via @delegate
}

Sample basicSample = new BasicSample("composition");
MySample sample = new MySample(basicSample);
sample.jot();   // prints "composition"
sample.ditto(); // prints "composition"

```
* `@component` is a class that implements one or more interfaces strictly for use as a delegate
* `@delegate` is a declared member of a class that delegates interface implementation to a component class

Restrictions on component classes and delegates are detailed in the next section.

## The self problem
Another critical aspect of compositional design concerns shared identity between the delegating class and the component
class, or lack thereof. Often called  _the self problem_, the delegating class instance and the component class instances
have separate identities; their `this` references are all different.  While this is critical for a compositional design,
a problem arises when a component class calls an interface method where the delegating class overrides the method. Since,
their identities are separate, the delegating class override is never called, thereby breaking the composition contract.

Left as-is, if `MySample` overrides `jot()` it is never called from `BasicSample#ditto()`.
```java
public class MySample implements Sample {
  @delegate final Sample basicSample;

  public MySample(Sample sample) {
    basicSample = sample;
  }

  @Override
  public void jot() {
    out.println( "hello" );
  }

  // interface delegation is handled via @delegate
}

Sample basicSample = new BasicSample("composition");
MySample sample = new MySample(basicSample);
sample.jot();   // prints "hello"
sample.ditto(); // prints "composition"  Yikes!
```
As such language support for delegation should not only provide delegation from the delegating class to the component, but
also from the component back to the delegating class. The following additional requirements must be met to achieve this
behavior.

1. A delegating class interface override must in all cases override the component class implementation
<ul>
As such, `this` references within a component class definition must be replaced with references to the delegating class
instance for interface method invocation, argument passing, and method return statements. This will be handled with AST
processing.
</ul>

2. A component class constructor is invoked exclusively from a delegate declaration or as a super call from a subclass.
<ul>
This restriction is necessary not only given <i>#</i>1, but also to not burden the component class designer with use-cases
outside the delegate one.
</ul>

3. A component class may extend only other component classes.
<ul>
Otherwise, the super class' `this` references defy <i>#</i>1.  Note, a stricter rule would prohibit class inheritance altogether.
But composition v. inheritance is not an either/or proposition. Both models have their place and forcing a purely a compositional
design is impractical, particularly within Java's dominant class inheritance world.
</ul>

Having met these three requirements the previous example works as expected.
```java
sample.jot();   // prints "hello"
sample.ditto(); // prints "hello"
```    
Other restrictions will be enforced by applying compiler errors as necessary.

## Abstract components

Sometimes a component class must leave a portion of its interface methods unimplemented with the intention of having the
delegating class implement them. As such the class must be declared abstract. The problem here is that the delegating
class cannot construct an abstract class, so the component class typically implements the methods as stubs and relies on
documentation for the stubbed methods to be implemented by the delegating class, leading the runtime errors instead of compile-time
errors.

An intermediate component class could extend the base component and implement the necessary interface methods to satisfy
the delegating class. However, that step is not obvious and, worse, amounts to boilerplate that can be avoided.

To address this use-case we will support delegation to abstract components. This will be implemented by:
- rewriting `new AbstractComponent();` to an anonymous version: `new AbstractComponent() { missing stubs here }`
- only generating delegate methods in the delegating class for implemented AbstractComponent methods, allowing the compiler
to police the delegating class for unimplemented methods

## Further boilerplate reduction
Since component class members require different accessibility defaults from normal classes, it may be worthwhile to support
access modifier defaults that are more suitable for component classes. For instance, constructors and interface method
overrides default to `public`, everything else defaults to `private`. One problem, however, is the Java default _package_
access will not be possible. Package access is by far the least needed of the four, perhaps not having to read or write
a ton of access modifiers is a decent trade-off? 