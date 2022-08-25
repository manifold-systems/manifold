# Unit Expressions
>**⚠ _Experimental Feature_**

The manifold project seamlessly plugs into the Java compiler to
provide Unit Expressions. In a nutshell unit expressions provide a powerfully concise syntax combining concatenative and
object-oriented paradigms. Instead of composing _functions_ as with concatenative programming languages, unit
expressions compose operations on juxtaposed _objects_. The result is naturally expressive, easy to read and write
syntax.

Units tend to be simple identifiers. Normally you import predefined unit constants like the ones provided in
`UnitConstants` from the `manifold.science.util` package:
```java
import static manifold.science.util.UnitConstants.kg;
import static manifold.science.util.UnitConstants.hr;
import static manifold.science.util.UnitConstants.mph;
. . .
```
Using imported constants such as `kg` for `Kilogram`, `hr` for `Hour`, `mph` for `Mile/Hour`, etc. you can begin working
with unit expressions:

**Naturally concise syntax**
```java
Time t = 3 hr;

Length distance = 100 mph * 3 hr;
```
**Type-safe**
```java
Force force = 5kg * 9.807 m/s/s; // 49.035 Newtons
```
**Logically equivalent units are equal**
```java
var force = 49.035 kg m/s/s;
force == 49.035 N // true
```
**Maintain integrity with different units**
```java
Mass m = 10 lb + 10 kg; 
```
**Make Ranges with the `to` constant from [`RangeFun`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-collections/src/main/java/manifold/collections/api/range/RangeFun.java)**
```java
for( Mass m: 10kg to 100kg ) {...}
```
**Conveniently work with Money**
```java
Money payment = 1.5M USD; 
Money vat = 162k EUR;
Money total = payment + vat; // naturally works with multiple currencies
``` 
>Note, unit expressions and *operator overloading* are often used together, read more about [operator overloading](#operator-overloading).

## How does it work?
Normally a *binary* expression in Java and most other languages involves two operands separated by an operator such as
`+` for addition:
```java
int sum = a + b;
```

But with a unit expression the operands are directly adjacent without an operator separating them:
```java
Mass m = 10 kg;
```

The operation is _declared_ in an operand's type with one of the following methods:
```java
public R prefixBind(T rhs);
public R postfixBind(T lhs);
``` 
Where either the left operand defines `prefixBind(T rhs)` or the right operand defines `postfixBind(T lhs)`.

In the example, `10` is a literal value of type `int` and `kg` is a variable of type `MassUnit`. Since `kg` is on the
right-hand side of `10` and the `MassUnit` class defines the method:
```java
public Mass postfixBind(Number magnitude) {...}
``` 
the compiler translates the expression as the method call `kg.postfixBind(10)` resulting in type `Mass`.

Note, `postfixBind()` and `prefixBind()` do not belong to a class or interface you implement. Instead, you implement
them *structurally* simply by defining a method with the same name, parameter, and non-void return type. This is
necessary because a type may implement multiple versions of the same method, which is otherwise not possible with Java's
nominal type system.

## Operator Precedence

The empty or "binding" operator has a *phased* precedence. Lexically, its precedence lies between addition and
multiplication, thus during the compiler's parsing phase it produces an untyped AST reflecting this order.  However,
in the course of the compiler's type attribution phase the compiler restructures the AST to reflect binding operator
methods `prefixBind()` and `postfixBind()` declared in the operand types, during which the compiler considers the
binding operator as having a precedence *equal to* multiplication.

To illustrate consider the following expression:
```java
a b * c
```

The binding operator, having a lexical precedence less than multiplication, parses like this:
```java
a (b * c)
```

In a later stage when operand types are available the expression may restructure if:
1. `a` and `b` have a binding relationship declared with `A.postfixBind(B)` or `B.prefixBind(A)` and
2. the type of the resulting `(a b)` expression implements multiplication with the type of `c`
```java
(a b) * c
``` 

For example, the expression `5 kg * 2` reflects this example exactly.

As you can see unit expressions demand a level of flexibility beyond that of conventional compilers such as Java's. But
Java is flexible enough in its architecture so that Manifold can reasonably plug in to augment it with this capability.

## Type-safe and Simple

There is nothing special about a unit, it is just a simple expression, most of the time just a variable. You can easily
define your own aliases for units like the ones defined in `manifold.science.util.UnitConstants`.
```java
LengthUnit m = LengthUnit.Meter;
Length twoMeters = 2 m;
``` 

## More Than Units

What makes unit expressions work is simple, just a pair of methods you can implement on any types you like:
```java
public R postfixBind(T lhs);
public R prefixBind(T rhs);
``` 
If your type implements either of these, it is the basis of a potential "unit" expression. Thus, the application of
these methods goes beyond just units. To illustrate, let's say you want to make date "literal" expressions such as:
```java
LocalMonthDay d1 = May 15;
LocalYearMonth d2 = 2019 May;
LocalDate d3 = 2019 May 15;
```
Binding expressions easily accommodate this use-case.  Something like:
```java
package com.example;

public enum Month {
  January,
  February,
  March,
  April,
  May,
  ... // etc.
  
  public LocalMonthDay prefixBind(Integer date) {
    return new LocalMonthDay(this, date);
  }
  
  public LocalYearMonth postfixBind(Integer year) {
    return new LocalYearMonth(this, date);
  }
}
```
In turn `LocalYearMonth` can define `LocalDate prefixBind(Integer)`. That's all there is to it. Now you have type-safe
date expressions:
```java
import static com.example.Month.*;
...
LocalDate date = 2019 October 9;
```

Essentially you can implement binding expressions to make use of juxtaposition wherever your imagination takes you.

## Science & Ranges
As some of the examples illustrate, unit expressions are particularly well suited as the basis for a library
modeling physical dimensions such as length, time, mass, etc. Indeed, check out the [manifold-science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
component.

Another application of units involves the [Range API](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#ranges)
provided by the [manifold-collections](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
component. By importing the static constants from `RangeFun` you can begin working with ranges:
```java
IntegerRange range = 1 to 5;
```
```java
for (Rational csr: 5.2r to 15.7r step 0.3r) {...}
```
```java
for (Mass mass: 10kg to 100kg unit lb) {...}
```
```java
if ("le matos" inside "a" to "m~") {...}
``` 
             
## Learn more
Unit expressions are an experimental part of the [manifold-ext](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
component within [Manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext),
an open source project available on github.