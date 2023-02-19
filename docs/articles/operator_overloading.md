#### Synopsis

Finally you can use `BigDecimal` and other Java types directly in arithmetic and relational operations by incorporating
a feature called [*operator overloading*](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
into your project. In this article Scott McKinney walks you through this new capability provided by the
[Manifold](https://github.com/manifold-systems/manifold) project. Along the way he discusses other interesting features
built atop operator overloading including [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions).

# Operator Overloading for Java

The [Manifold extension](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
dependency plugs into Java to provide seamless operator overloading capability. You can type-safely provide arithmetic,
relational, and [unit](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
operators for any class by implementing one or more predefined *operator methods*. You can implement operator methods
directly in your class or use [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension)
to implement operators for classes you don't otherwise control. For example, using extension methods Manifold provides
operator implementations for `BigDecimal` so you can write code like this:

```java
BigDecimal result = bigValue1 + bigValue2;
```
With unit expressions it gets even better where you can work with `BigDecimal` and other types more easily:
```java
BigDecimal result = 3.14bd * 10.75bd; // `bd` makes BigDecimals
```
Precise measurements via units and operator overloading:
```java
Length distance = 65 mph * 3.2 hr;
HeatCapacity kBoltzmann = 1.380649e-23r J/dK;
```

## Arithmetic and Negation Operators

Any type can support arithmetic operators by implementing one or more of the following operator methods: 

**Arithmetic**

| Operation | Method       |
|:----------|:-------------|
| `a + b`   | `a.plus(b)`  |
| `a - b`   | `a.minus(b)` |
| `a * b`   | `a.times(b)` |
| `a / b`   | `a.div(b)`   |
| `a % b`   | `a.rem(b)`   |

**Negation**

| Operation | Method           |
|:----------|:-----------------|
| `-a`      | `a.unaryMinsu()` |

Note operator methods do not belong to a class or interface you implement. Instead you implement them *structurally*
simply by defining a method with the same signature. Note you can implement several different versions of the same
method differing by parameter type. 

Here's a simple example demonstrating how to implement the `+` operator:
```java
public class Point {
  public final int x, y;
  public Point(int x, int y) {this.x = x; this.y = y;}
  
  public Point plus(Point that) {
    return new Point(x + that.x, y + that.y);
  }
}

var a = new Point(1, 2);
var b = new Point(3, 4);

var sum = a + b; // Point(4, 6)
```

Since operator methods are structural, you can define *multiple* `plus()` methods:
```java
public Point plus(int[] coord) {
  if(coord.length != 2) {
    throw new IllegalArgumentException();
  }
  return new Point(x + coord[0], y + coord[1]);
}
```
   
## Relational Operators

You can implement relational operators using a combination of the `ComparableUsing` and/or `Comparable` interfaces.

### `manifold.ext.rt.api.ComparableUsing`

Relational operators can be implemented all together with the `ComparableUsing` interface, which extends `Comparable`
to provide an operator-specific API.                           
```java
boolean compareToUsing( T that, Operator op );
```
Where `Operator` is an `enum` which specifies constants for relational operators.

| Operation | ComparableUsing Impl      | Comparable Impl       |
|-----------|---------------------------|-----------------------|
| `a > b`   | `a.compareToUsing(b, GT)` | `a.compareTo(b) > 0`  |
| `a >= b`  | `a.compareToUsing(b, GE)` | `a.compareTo(b) >= 0` |
| `a < b`   | `a.compareToUsing(b, LT)` | `a.compareTo(b) < 0`  |
| `a <= b`  | `a.compareToUsing(b, LE)` | `a.compareTo(b) <= 0` |

`ComparableUsing` provides a default implementation for `compareToUsing()` that delegates to `Comparable`'s
`compareTo()` implementation for the `>`, `>=`, `<`, `<=` subset of relational operators.  For the `==` and `!=` subset
`ComparableUsing` delegates to the type's `equals()` method (more on equality later).  This behavior is suitable for
most types, so normally you only need to add `ComparableUsing` to your type's `implements` or `extends` clause and
implement just `Comparable` as you normally would. Thus adding relational operator support to the `Point` example we
have:

```java
public class Point implements ComparableUsing<Point> {
  public final int x, y;
  public Point(int x, int y) {this.x = x; this.y = y;}
  
  public Point plus(Point that) {
    return new Point(x + that.x, y + that.y);
  }
  
  public int compareTo(Point that) {
    return x - that.x;
  }
}
```
Now you can easily compare `Point` values like this:
```java
if (pt1 >= pt2) ...
```

### `java.lang.Comparable`

If you're not interested in supporting `==` and `!=` and your type implements the `Comparable` interface, it
automatically supports the `>`, `>=`, `<`, `<=` subset of relational operators. For example, both `java.lang.String` and
`java.time.LocalDate` implement the `compareTo()` method from `Comparable`, which means they can be used in relational
expressions:

```java
String name1;
String name2;
...
if (name1 > name2) {...}
```   

```java
LocalDate date1;
LocalDate date2;
...
if (date1 > date2) {...}
```

## Equality Operators

To implement the `==` and `!=` subset of relational operators you must implement the `ComparableUsing` interface. By
default `ComparableUsing` delegates to your type's `equals()` method, but you can easily override this behavior by
overriding the `equalityMode()` method in your `CopmarableUsing` implementation. The `EqualityMode` enum provides the
available modes:     

```java
/**
 * The mode indicating the method used to implement {@code ==} and {@code !=} operators.
 */
enum EqualityMode
{
  /** Use the {@code #compareTo()} method to implement `==` and `!=` */
  CompareTo,

  /** Use the {@code equals()} method to implement `==` and `!=` (default) */
  Equals,

  /** Use {@code identity} comparison for `==` and `!=`, note this is the same as Java's normal {@code ==} behavior } */
  Identity
}
```

Based on the `EqualityMode` returned by your implementation of `CompareToUsing#equalityMode()`, the `==` and `!=`
operators compile using the following methods: 

| Operation | `Equals` <small>(default)</small> | `CompareTo`| `Identity` |
|:----------|:-------------------|:--------------------------|:-----------|
| `a == b`  | `a.equals(b)`      | `a.compareToUsing(b, EQ)` | `a == b`   |
| `a != b`  | `!a.equals(b)`     | `a.compareToUsing(b, NE)` | `a != b`   |

Note Manifold generates efficient, **null-safe** code for `==` and `!=`. For example, `a == b` using `Equals` mode
compiles as:
```java
a == b || a != null && b != null && a.equals(b)
``` 

If you need something more customized you can override `compareToUsing()` with your own logic for any of the operators,
including `==` and `!=`.
 
To enable `==` on `Point` more effectively, you can accept the default behavior of `ComparableUsing` and implement
`equals()`:
 
```java
public boolean equals(Object that) {
  return this == that || that != null && getClass() == that.getClass() && 
         x == ((Point)that).x && y == ((Point)that).y;
}
```
>Note always consider implementing `hashCode()` if you implement `equals()`, otherwise your type may not function
>properly when used with `Map` and other data structures:
>```java
>public int hashCode() {
>  return Objects.hash(x, y); 
>}
>```

Sometimes it's better to use the `CompareTo` mode.  For instance, the `==` and `!=` implementations for `Rational`,
`BigDecimal`, and `BigInteger` use the `CompareTo` mode because in those classes `compareTo()` reflects equality in
terms of the *face value* of the number they model e.g., 1.0 == 1.00, which is desirable behavior in many use-cases. In
that case simply override `equalityMode()` to return `CompareTo`:
```java
@Override
public EqualityMode equalityMode() {
  return CompareTo;
}
```
 
## Unit Operators

Unit or "binding" operations are unique to the Manifold framework. They provide a powerfully concise syntax and can be
applied to a wide range of applications. You implement the operator with the `prefixBind()` and `postfixBind()` methods:

| Operation  | Postfix Bind       | Prefix Bind       |
|------------|--------------------|-------------------|
| `a b`      | `b.postfixBind(a)` | `a.prefixBind(b)` |

If the type of `a` implements `R prefixBind(B)` where `B` is assignable from the type of `b`, then `a b` compiles as the
method call `a.prefixBind(b)` having type `R`. Otherwise, if the type of `b` implements `R postfixBind(A)` where `A` is
assignable from the type of `a`, then `a b` compiles as the method call `b.postfixBind(a)` having type `R`.

This feature enables expressions such as:
```java
Mass m = 5 kg;
Force f = 5 kg * 9.8 m/s/s;
for (int i: 1 to 10) {...}
```
Read more about [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions).
 
## Operators by Extension Methods 

Using [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension)
you can provide operator implementations for classes you don't otherwise control. For instance, Manifold provides
operator extensions for [`BigDecimal`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-ext/src/main/java/manifold/ext/extensions/java/math/BigDecimal/ManBigDecimalExt.java)
and [`BigInteger`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-ext/src/main/java/manifold/ext/extensions/java/math/BigInteger/ManBigIntegerExt.java).
These extensions are implemented in the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
dependency.  

Here's what the `+` extension for `BigDecimal` looks like:
```java
@Extension
public abstract class ManBigDecimalExt implements ComparableUsing<BigDecimal> {
  /** Supports binary operator {@code +} */
  public static BigDecimal plus(@This BigDecimal thiz, BigDecimal that) {
    return thiz.add(that);
  }
  ...
}
```
Now you can perform arithmetic and comparisons using operator expressions:
```java
if (bd1 >= bd2) {
  BigDecimal result = bd1 + bd2;
  . . .
}
```

>Note the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
and [`manifold-collections`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)
modules use operator overloading and unit expressions extensively.


##Update: _2020/11/13_
>
>Release 2023.1.3 adds additional operator overloading support for:
>
>* infix/postfix increment/decrement operators  `++` and `--`
>* compound assignment: operators `+=`, `-=`, `*=`, `/=`, `%=`, 
>* index operator `[]`, both access and assignment
>
>Please see [Operator Overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading) documentation for details.

   
