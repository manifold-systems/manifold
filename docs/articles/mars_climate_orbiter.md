# Type-safe, Error-proof Unit Expressions for Java (and you)

<p><img src="http://manifold.systems/images/m98-14-stg3ign.jpg" alt="echo method" width="70%" height="70%"/></p>

>Units of measure are often overlooked in the context of type-safety. This article uses the NASA Mars Climate Orbiter
>mishap as a backdrop to help illustrate where Java APIs can be vulnerable in their use of physical quantities and how
>the [Manifold framework](https://github.com/manifold-systems/manifold) works toward a solution. In particular, this
>article focuses on Manifold's [operator overlaoding](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
>and [science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science) dependencies
>as a refreshing alternative to conventional means. 

No other example of unit related failure quite measures up to the 1999 incident involving NASA's Mars Climate Orbiter (MCO).
It's a familiar story, most folks know the spacecraft missed its big red target by miles... er kilometers or
something related to the metric system. Indeed, the investigation led by the [MCO Mishap Investigation Board](https://llis.nasa.gov/llis_lib/pdf/1009464main1_0641-mr.pdf)
reported the orbiter missed its mark due to:

>_The output from the SM_FORCES application code as required by a MSOP Project Software Interface Specification (SIS) was
to be in metric units of Newtonseconds (N-s). Instead, the data was reported in English units of pound-seconds (lbf-s)._

Whoops! The report also reveals several missed opportunities that could have otherwise prevented the mishap. Ultimately,
gaps in communication were viewed as central to the point of failure. But one must wonder, if the problem manifested in
software, why isn't the focus on software? 

To rehash, one system provided quantities of linear momentum as pound seconds (lbf s) for another system expecting
Newton seconds (N s). That failure could only have happened if at some point in the interface between the two systems
quantities were specified as raw, unitless data, perhaps as floating point primitives. A very simplified illustration
with Java:
```java
/**
 * Position the orbiter for optimal trajectory. Don't mess this up!
 * 
 * @param momentum Amount of thrust to apply. And uh, one more thing, use SI units here. 
 */
public void performManeuver(double momentum) {
    // fire thrusters and stuff
}
```  
Perhaps this the real tragedy. Sure, the documentation requires SI units, but the documentation doesn't compile code. Javac
will make darn sure callers pass a double, but it knows not of linear momentum or SI units. 

As a general rule, if the compiler can enforce what otherwise must be documented, favor the compiler! So let's replace
the double with a type-safe abstraction for momentum.    
```java
/**
 * Position the orbiter for optimal trajectory. You *can't* mess this up!
 * 
 * @param momentum Amount of thrust to use. 
 */
public void performManeuver(Momentum momentum) {
    // fire thrusters and stuff
}
```  
The `Momentum` type is taken from the Manifold framework's [Science library](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science).
Quantities of `Momentum` are expressed in terms of `MomentumUnit`. Further, `Momentum` stores its value in terms of SI
units, while remembering the type of unit used to create an instance. As such all calculations are inherently
unit-independent. Now we can call `performManeuver()` with any unit of momentum we like:
```java
Momentum momentum = new Momentum(50, MomentumUnit.POUND_SECONDS);
orbiter.performManeuver(momentum);
// or
Momentum momentum = new Momentum(222.41108075988, MomentumUnit.NEWTON_SECONDS);
orbiter.performManeuver(momentum);
```
Both calls apply the same quantity of Momentum, which is a big win. But how do we work with Momentum? One advantage of
using primitive types is that they're easier use. We want the best of both worlds. Manifold's delivers via operator
overloading using the [Extension library](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext).
lets us do this:
```java
Momentum momentum = 50 lbf s;
// or
Momentum momentum = 222.41108075988 N s;
```
We also want convenient, type-safe expressions across the spectrum of physical dimensions. For instance, a product of
`Force` and `Time` is a quantity of `Momentum`. The Science library provides a comprehensive support:
```java
Momentum momentum = 5 lbf * 10 s;
// or
Momentum momentum = 22.241108075988 N * 10 s;
```
Equality asd relational expressions are also handy:
```java
out.println(50 lbf s > 222 N s); // true
```
Eventually, physical calculations lead to interfacing with the outside world. Whether that's displaying results on a
screen or interacting with a spacecraft, the software must produce _specific_ quantities to share. Thus, we need simple,
type-safe unit conversions and numeric data access. All the physical dimension classes provide a common interface:
```java
log("Burning: ${momentum.to(N s)}");
Time duration = 10 s;
Force force = momentum / duration;
device.burn(force.fpValue(), duration.fpValue());
```
There's a lot more to Manifold's treatment of physical quantities, but the intent here is to briefly demonstrate how to
use the framework to strengthen an API with type-safety around physical quantity usage. You can use the framework
anywhere in your code where type-safe quantities could improve your development experience. Hopefully you'll never have
to experience a unit failure of the Mars Climate Orbiter caliber!  