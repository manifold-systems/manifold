---
layout: default
---

# Manifold Roadmap
### A brain dump of thoughts that may contribute toward future Manifold releases
 
## Data Science 

A long term goal for Manifold is to make Java the center of a first-class data science development platform.  One reason,
perhaps the primary reason, the data science community prefers other languages over Java relates to its type system.  Java is
a statically typed language, which generally means it's type-safe and compiles.  While these are good traits, 
they come with a significant downside if your goal is to fluently bind with structured data.  For instance,
if you want to access CSV data files with Java, you either use a string-based static library and forgo type-safety or 
you throw yet another code generator onto the pile and live with [the consequences](https://jaxenter.com/manifold-code-generators-150738.html).
But neither of these options is worth pursuing when you have dynamically typed languages at hand.  

What makes dynamic typing so attractive to data science programmers boils down to one word: Metaprogramming. With dynamic languages 
your source code can play fast and loose with structured data because metaprogramming hooks it all up later at runtime. 
But there's a catch.  The concise code comes at the heavy cost of weak type information.  Without static types 
it's difficult to discover and use features provided by metaprogramming while writing code, it is purely a runtime manifestation. 
As a consequence metaprogramming can be difficult to learn and often leads to trial and error coding. Nonetheless it is 
a trade-off many data science programmers are willing to make.

Must we choose between type-safety and metaprogramming?  What if metaprogramming could happen at _compile-time_?  What
if a static type system were "open" in such a way where types corresponding with structured data could dynamically 
materialize and directly resolve through compiler plugins?  This is Manifold's core function -- it seamlessly opens 
Java's compiler to structured data plugins that participate as first-class type suppliers called _Type Manifolds_.  

### Support Data Science Formats (initially focusing on biological sciences)
Develop type manifolds for:
* JSON
* XML
* CSV
* XLS
* FASTA
* R Language
* many many more

### Support Scientific / Statistical Data Repositories
These type manifolds are URL-based.  One proof of concept is underway for DbPedia and SPARQL. 

### Support SQL and DDL
This is a more involved Type Manifold and will involve writing a SQL parser, albeit not as complicated as a full
parser because only the declarative portions are necessary to gather type information.

### Integrate with other languages
Write type manifolds for:
* Python
* R

## Core Features

### Expand Self Type Usage

Currently `@Self` is limited to targeting method return types.  In a future release `@Self` will expand support 
to include method *parameter types*.

A key difference with Manifold's self type is although `@Self` targets the method declaration, it resolves at the method 
*call site* -- the best of both worlds. As a consequence it avoids the barrel of monkeys that is Java method bridging and, 
importantly, it enables more flexibility with the type system in terms of method signature variance and generics. This 
opens the door to type-safe usages in methods like `Object#equals(Object)` -- with `@Self` you could instead define it as: 
`Object#equals(@Self Object)` where you can override the method using the same `@Self` Object parameter type, but still have 
the compiler enforce the subclass type for arguments to the method as well as enforce subclass treatment of the 
parameter in the method body. This feature will be available in a future release.

### Type-safe Reflection via @JailBreak

Sometimes you have to call private methods and use other inaccessible features, but reflection makes this a tedious 
process and results in hard to maintain code.  Worse, with reflection you completely bypass type-safety.  Manifold 
will change that with `@JailBreak`:

```java
public class Foo {
  private void privateMethod(String arg) {...}
}

@JailBreak Foo foo = new Foo();
foo.privateMethod("hi"); // type-safe call to "inaccessible" method
```

Use `@JailBreak` to access classes, methods, and fields type-safely regardless of declared accessibility.  Because if 
you're gonna do it anyway, why suffer the consequences of reflection?

Similarly, to conveniently use inaccessible features from an expression you can use the `$jailbreak()` extension method:

```java
myObject.getFoo().$jailbreak().privateMethod("hi");
```

Where the extension method is defined on `Object` as:

```java
public static @JailBreak @Self Object $jailbreak(@This Object thiz) {
  return thiz;
}
```   

