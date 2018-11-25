---
layout: default
---

# Manifold Roadmap
### A brain dump of thoughts that may contribute toward future Manifold releases
 
## Data Science 

A long term goal for Manifold is to make Java the center of a first-class data science environment.  One reason,
perhaps the primary reason, the data science community steers clear from Java relates to its type system.  Java is
a statically typed language, which generally means it's type-safe and compiles.  While these are good traits, 
they come with a significant downside if your goal is to fluently bind with structured data.  For instance,
if you want to access CSV data files with Java, you either use a string-based static library and forgo type-safety or 
you throw yet another code generator onto the pile and live with [the consequences](https://jaxenter.com/manifold-code-generators-150738.html).
Neither of these options is worth pursuing, however, when you have dynamically typed languages at hand.  

What makes dynamic typing so attractive to data science programmers boils down to one word: Metaprogramming. With dynamic languages 
your source code can play fast and loose with structured data because metaprogramming hooks it all up later at runtime.  There's 
no free lunch, however.  The concise code comes at the heavy cost of weak type information.  Without static types 
it's difficult to discover the features provided by metaprogramming in the context of a code editor, it is purely a runtime manifestation. 
As a consequence metaprogramming often involves throwing source code over the wall and hoping for the best; 
nonetheless a trade-off many data science programmers are willing to make.

But must one choose between type-safety and metaprogramming?  Most in the language community consider these to be 
diametrically opposed ideas.  But what if metaprogramming could happen at _compile-time_?  What if a static type
system were "open" in such a way where types could be dynamically resolved through plugins?  This is Manifold's 
core function -- to open Java's type system so that other type domains can plugin and participate as first-class
type providers.  This is what a Type Manifold is all about.  This is what can improve Java's standing as a data
science language.

### Support Data Science Formats (initially focusing on biological sciences)
Develop Type Manifolds for:
* JSON
* CSV
* FASTA
* many many more

### Support Scientific / Statistical Data Repositories
These type manifolds will be URL-based.  One proof of concept is underway for DbPedia and SPARQL. 

### Support SQL and DDL
This is a more involved Type Manifold and will involve writing a decent SQL parser, albeit not as complicated as a full
parser because only the declarative portions are necessary to gather type information.
 
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

Sometimes you have to call private methods and use other inaccessible features, but reflection makes this not 
only difficult and sloppy, perhaps worse you lose compile-time type-safety in the process.  Manifold will 
change that with `@JailBreak`:

```java
public class Foo {
  private void privateMethod(String arg) {...}
}

@JailBreak Foo foo = new Foo();
foo.privateMethod("hi"); // type-safe call to "inaccessible" method
```

Use `@JailBreak` to access classes, methods, and fields type-safely regardless of declared accessibility.  Because if 
you're gonna do it anyway, why suffer the consequences of reflection?
   

