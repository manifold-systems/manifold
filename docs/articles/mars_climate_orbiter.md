# Type-safe Unit Expressions for Java (and you)

>Units of measure are often overlooked in the context of type-safety. We need look no further than the infamous unit
>error involving NASA's Mars Climate Orbiter to illustrate why type-safety around units matters. Here we model the failure
>in simple terms to show how a Java API can be vulnerable in its use of physical quantities. This article briefly
>demonstrates how the Manifold framework's [extension](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
>and [science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
>libraries can be applied toward a general solution using type-safe units and quantities. 

<p><img src="http://manifold.systems/images/m98-14-stg3ign.jpg" alt="echo method" width="60%" height="60%"/></p>

No other example of unit related failure quite measures up to the 1999 incident involving NASA's Mars Climate Orbiter (MCO).
Most folks know the spacecraft missed its big red target by miles... er kilometers or
something related to the metric system. Indeed, the investigation led by the [MCO Mishap Investigation Board](https://llis.nasa.gov/llis_lib/pdf/1009464main1_0641-mr.pdf)
concluded the orbiter missed its mark due to:

>_The output from the SM_FORCES application code as required by a MSOP Project Software Interface Specification (SIS) was
to be in metric units of Newtonseconds (N-s). Instead, the data was reported in English units of pound-seconds (lbf-s)._

Whoops! The report also reveals several missed opportunities that could have otherwise prevented the mishap. Ultimately,
gaps in communication were viewed as central to the point of failure. But one must wonder, if the problem manifested in
software, why isn't the focus on software? 

To rehash, one system provided quantities of linear momentum as pound seconds (lbf s) for another system expecting
Newton seconds (N s). That failure could only have happened if at some point in the interface between the two systems,
quantities were specified as raw, unitless data, perhaps as floating point primitives.[<sup>1</sup>](#1)
A very simplified illustration:
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
Perhaps this is the real tragedy? Sure, the documentation requires SI units, but the documentation doesn't compile code.
Javac will make darn sure callers pass a double, but it knows not of linear momentum or SI units (unless you tell it). 

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
A quantity of `Momentum` is expressed in terms of `MomentumUnit`. Further, `Momentum` stores its value in terms of SI
units, while remembering the type of unit used to create an instance. As such, all calculations are inherently
unit-independent. We can now call `performManeuver()` with our preferred unit of measure:
```java
Momentum momentum = new Momentum(50, MomentumUnit.POUND_SECONDS);
orbiter.performManeuver(momentum);
// or
Momentum momentum = new Momentum(222.41108075988, MomentumUnit.NEWTON_SECONDS);
orbiter.performManeuver(momentum);
```
Both calls apply the same quantity of Momentum, which is a big win. But how do we *work* with Momentum? One advantage of
using primitive types is that they're easy use. Manifold maintains ease of use with type-safe [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions):
```java
Momentum momentum = 50 lbf s;
// or
Momentum momentum = 222.41108075988 N s;
```
Additionally, with the aid of Manifold-provided [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
we have the convenience of type-safe arithmetic across the entire spectrum of physical dimensions. For instance, the
science library defines the product of `Force` and `Time` as a quantity of `Momentum`.
```java
Momentum momentum = 5 lbf * 10 s;
// or
Momentum momentum = 22.241108075988 N * 10 s;
```
Equality and relational expressions are equally handy:
```java
out.println(50 lbf s > 222 N s); // true
```
Eventually, physical calculations lead to interfacing with the outside world. Whether that's displaying results on a
screen or interacting with a spacecraft, the software must produce _specific_ quantities to share. Manifold's physical
dimension classes provide a common interface for unit conversions and data access:
```java
log("Burning: ${momentum.to(N s)}");
Time duration = 10 s;
Force force = momentum / duration;
device.burn(force.fpValue(), duration.fpValue());
```

>It's worth mentioning the [Manifold Plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold)
provides comprehensive support for all of the framework's features. Download it free for Community Edition and there's
a free trial for Ultimate Edition.

There's much more to Manifold's treatment of physical quantities, but the intent here is to briefly demonstrate how to
use the framework to strengthen an API with type-safety around physical quantity usage. Of course, you can use the
framework anywhere in your code where type-safe quantities could improve your development experience. Hopefully you'll
never have to experience a unit failure of the Mars Climate Orbiter caliber!  
<br>
<br>
<a class="anchor" id="1"></a>
_<sup>1</sup>The receiving system's API (SIS) was defined in terms of raw data to be deserialized from a file. The
format of the file was not type-safe and did not include a unit of measure._ 