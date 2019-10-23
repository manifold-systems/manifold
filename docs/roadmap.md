---
layout: default
---

# Roadmap
 
 ## Table of Contents
 * [Long Term Goals](#long-term-goals)
 * [Short Term Goals](#short-term-goals)
 * [On the workbench](#features-on-the-workbench)
 * [On the pile](#features-on-the-pile-in-no-particular-order)
 
# Long Term Goals

## Toward Data Science with Java and Manifold

Currently, Java lags far behind other languages in the field of data science. One reason, perhaps the primary reason, the
data science community prefers other languages has less to do with Java and more to do with the general limits imposed by
static typing.  Since Java is a *statically* typed language types must be known up front so the compiler
can verify the integrity of source code where types and features of types are used.  While the benefits of type-safety are many, it comes
with a significant downside if your goal is to dynamically and fluently access structured data. For instance, if you want
to access data corresponding with JSON Schema, GraphQL, or XSD, Java's type system forces you to choose between
pitching type-safety in the dirt and using a string-based static library or wedging a code generator into your build
process and living with [the consequences](https://jaxenter.com/manifold-code-generators-150738.html).

With this limitation in mind, what makes *dynamic* typing so attractive to data science programmers can be expressed in one word: <b>Metaprogramming</b>. This
is the magic that lets a Ruby or Python programmer write code to access data files using concise, easy to read syntax. With
these languages, because types aren't checked in source code, metaprogramming can play fast
and loose with structured data because it hooks up all the types *later* as the code runs. Ah, but _**there's the rub**_.
Not knowing types as you write code can make coding more difficult, indeed much more difficult with metaprogramming
because there is no type information to help you (and your dev environment) discover what all that magic can do for you 
-- *"you just have to know"*. As a consequence metaprogramming can be difficult to learn and use and often leads to
unproductive trial-and-error coding. Nonetheless it is a trade-off hordes of data science programmers have made.

Does type-safety and metaprogramming really have to be an either/or proposition?  What if metaprogramming could happen
at _compile-time_?  What if say Java's type system were *open* in such a way where types corresponding with structured
data such as JSON and GraphQL could materialize as the compiler resolves type names?  This is Manifold's core function.
The heart of Manifold consists of a [Java compiler plugin](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html)
that seamlessly opens Java's type system to structured data plugins that participate as first-class type suppliers called
_Type Manifolds_.  Essentially a type manifold plugs in to the Java compiler enabling it to resolve types from sources other
than `.java` files -- a _**just-in-time code generator**_, if you will.  

Suddenly the tables are turned in terms of the metaprogramming advantage. With type manifolds in hand Java's type-safety is no
longer a hindrance, but is instead valuable asset to the Python programmer struggling to master the dark art of <fill in the blank> meteprogramming.
Since type manifolds are effectively built into Java's compiler as well as IDE tooling, you have the best of both worlds:
concise, simple syntax *and* type-safety, which together enable powerful capabilities such as deterministic code completion.
You can discover available methods and properties immediately as you write code. You can use type-sensitive navigation to
jump between your code and the data resources you code against. You can make changes to structured data and *immediately*
see and use the effects of the changes *without* a compilation or build step. This type of functionality can make the
difference between amazing developer productivity and project failure. Suddenly Java is a capable data science language.

**In effect the _**type manifold**_ represents a breakthrough in programming language design as the convergence of dynamic
metaprogramming and compile-time type-safety. This is the bedrock on which Manifold builds its data science framework for
Java.**

## Data Science (near) Future

Over the past couple of years Manifold has steadily produced one "seemingly impossible" feature after another to
extend the Java platform's reach. At a distance these features may appear unrelated and that's fine, each feature
can be used independently. But there is a method to the madness -- together the features aim Java's reach toward a
common goal. 

One of manifold's more recent developments hints at some semblance of order -- the *manifold-science* project. It points in the general
direction Manifold is heading, which is to provide a data science framework for Java.  Now that most of the building
blocks are in place like type manifolds, extension methods, operator overloading, unit expressions, science libraries,
etc. the stage is set to begin building out higher level features. These include new type manifolds targeting popular
data science formats. So in addition to the existing JSON and GraphQL manifolds, more are on the way for SQL, CSV, XLS, MD5,
and others. Other type manifolds are on the drawing board targeting *languages* such as the R-language to provide
seamless interop with Java.

## Formats (schemas)

* GraphQL (complete)
* JSON (complete)
* SQL (in progress, on hold)
* XSD (todo)
* CSV (in progress)
* XLS (todo)

## Languages

* Javascript (complete)
* R Language (todo)
* Python? (maybe todo)

## Operator Overloading (complete)

Provided with the [`manifold-ext`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
dependency. Overload existing Java arithmetic and relational operators.

```java
BigDecimal rate = 9.247 bd;
BigDecimal damage = calc(vehicle) * rate;
```

## Dimensions (complete)

The [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
framework provides a comprehensive library of measures and units e.g., Length, Mass, Force, Energy, etc.

```java
Velocity rate = 100 km/hr;
Time time = 1 min + 7.2 s; 
Length distance = rate * time;
```

## Unit Expressions (complete)

The [`manifold-ext`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
dependency implements Unit Expressions for the Java language, directly and seamlessly implemented via the javac plugin.

```java
Length distance = 70 mph * 3.2 hr;
Energy energy = 50 kg m/s/s * 22 m;
``` 

## Ranges (complete)

Ranges are used extensively in data science. Manifold provides [concise Range syntax and API](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#ranges)
with the `manifold-collections` dependency.

```java
for (Mass mass: 10kg to 100kg step 2r) {
  ...
}

if (location inside Altanta radius 5 km ) {
  ...
}
```
  
## Rational Number (complete)

Rational numbers can be more convenient than BigDecimal. Provided with the [`manifold-science`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
dependency.
 
```java
Rational third = 1r/3;
Rational exactlyOne = third * 3r;
```

## Statistical library integration (todo)
[todo]

## Analytics library integration (todo)
[todo]

# Short Term Goals

## Features on the workbench
*These are features currently being built or refined*

* Working on the [fragments](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#embedding-with-fragments-experimental),
getting [manifold templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates) to work in Java 13 text blocks
and with IntelliJ language injection.

### Vote
* Vote for features you'd like to see next, thumb up (or add) an [issue](https://github.com/manifold-systems/manifold/issues).

## Features on the pile (in no particular order)
*These are features that may be implemented real soon now*

### Operator overloading enhancements
* https://github.com/manifold-systems/manifold/issues/126

### New schema type manifolds 
* https://github.com/manifold-systems/manifold/issues/113
* https://github.com/manifold-systems/manifold/issues/112
* https://github.com/manifold-systems/manifold/issues/111

### Manifold "inliner" tool
* https://github.com/manifold-systems/manifold/issues/95

### Default parameter values
* https://github.com/manifold-systems/manifold/issues/93

### Android support
* https://github.com/manifold-systems/manifold/issues/77

### Eclipse support
* https://github.com/manifold-systems/manifold/issues/18

