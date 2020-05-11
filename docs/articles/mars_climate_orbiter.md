# Type-safe Unit Expressions for Java (and you)

<small>Scott McKinney</small>
<br>

>Unit type-safety, or lack thereof, can have real consequences, sometimes disastrous ones. This article
>briefly demonstrates how the [Manifold](https://github.com/manifold-systems/manifold) framework's [extension](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
>and [science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
>libraries can be applied toward a general solution using type-safe units and quantities. 

<br>
<p><img src="http://manifold.systems/images/mco.jpg" alt="echo method" width="60%" height="60%"/></p>
<br>

No other example of unit related failure quite measures up to the 1999 incident involving NASA's Mars Climate Orbiter (MCO).
Those familiar with the story know the spacecraft missed its big red target by miles... er kilometers or something related to the metric
system. Indeed, the inquiry led by the [MCO Mishap Investigation Board](https://llis.nasa.gov/llis_lib/pdf/1009464main1_0641-mr.pdf)
concluded the orbiter missed its mark due to:

>_The output from the SM_FORCES application code as required by a MSOP Project Software Interface Specification (SIS) was
to be in metric units of Newtonseconds (N-s). Instead, the data was reported in English units of pound-seconds (lbf-s)._

Whoops! The report also reveals several missed opportunities that could have otherwise prevented the accident. Ultimately,
gaps in communication were viewed as central to the point of failure. But one must wonder, if the problem manifested in
software, why wasn't more attention placed there? 

To rehash, one system provided quantities of linear momentum as pound-seconds (lbf s) for another system expecting
newton-seconds (N s). The receiving system's API, consisting of a data format, failed to model the quantities
type-safely. Instead, the API accepted raw numeric data. A simplified illustration:
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
Perhaps this is the real tragedy? Sure, the documentation specifies SI units, but the documentation doesn't compile code.
Javac will make darn sure callers pass a double, but it knows not of linear momentum or SI units, unless you tell it.

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
The `Momentum` type is taken from the Manifold framework's [Science library](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science),
which models physical dimensions as a class library of type-safe units and quantities. A quantity of `Momentum` is
expressed in terms of `MomentumUnit`. Internally, the dimension classes maintain quantities as SI units, thus all
physical calculations are inherently unit-independent. As a result, we can now call `performManeuver()` with the
unit of our choosing:
```java
Momentum momentum = new Momentum(50, MomentumUnit.POUND_SECONDS);
orbiter.performManeuver(momentum);
// or
Momentum momentum = new Momentum(222.41108075988, MomentumUnit.NEWTON_SECONDS);
orbiter.performManeuver(momentum);
```
Although they are defined with different units, both values are identical quantities of Momentum. Big win! But how do we
*work* with Momentum? One advantage of using a unitless type like `double` is simplicity. Manifold maintains and extends
this level of convenience with type-safe [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
which allow us to rewrite the code above like this:
```java
Momentum momentum = 50 lbf s;
orbiter.performManeuver(momentum);
// or
Momentum momentum = 222.41108075988 N s;
orbiter.performManeuver(momentum);
```
Additionally, with the aid of Manifold-provided [operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
we have the utility of type-safe arithmetic across the entire spectrum of physical quantities. For instance, the science
library defines the product of `Force` and `Time` as a quantity of `Momentum`.
```java
Momentum momentum = 5 lbf * 10 s;
// or
Momentum momentum = 22.241108075988 N * 10 s;
```
Equality and relational expressions are also powerfully concise:
```java
out.println(50 lbf s > 222 N s); // true
```
Eventually, physical calculations lead to interfacing with the outside world. Whether that's displaying results on a
screen or interacting with a spacecraft, the software delivers _specific_ values. Manifold's physical dimension
classes provide a common interface for unit conversions and data access. You can also use string interpolation to easily
format output:
```java
Momentum momentum = 222.41108075988 N s;
Time duration = 10 s;
Force force = momentum / duration;
out.println("$duration burn at $force");
out.println("$duration burn at ${force.to(lbf)}");
```
>Note, the [Manifold Plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold) provides
>comprehensive support for all of the framework's features. It is free for use with IDEA Community Edition.

There's much more to Manifold's treatment of physical quantities. The intent here is to point out the significance of
type-safety with respect to API usage of physical quantities and how to apply Manifold toward making improvements. Of
course, you can use the framework anywhere in your code where type-safe quantities could improve your development
experience. Hopefully, you'll never have to experience a unit failure of the Mars Climate Orbiter caliber!

> The sample code from this article is [available on github](https://github.com/manifold-systems/mars-orbiter-article).
>
> Visit [Manifold](http://manifold.systems/) to learn more. 