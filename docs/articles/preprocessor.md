# A Preprocessor for Java

Discover how to build multiple targets from a single Java codebase using [the new preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)
from [the Manifold project](https://github.com/manifold-systems/manifold). In this article Scott McKinney explains how
the preprocessor plugs directly into Java's compiler to provide seamless conditional compilation using familiar
directives.


# Preview

Here's a quick preview to give you a taste of Manifold's preprocessor.
<br>
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();" autoplay loop>
    <source type="video/mp4" src="/images/preprocessor.mp4">
  </video>
</p>
<br>
We'll dive into what's going on here later in the article.
  
  
# Rationale

There is an undeniable stigma associated with preprocessors, mostly rooted in the C/C++ community. Indeed anyone who has
transitioned from the C++ world to the Java world has their favorite horror story involving the preprocessor.  The
antagonist in the story is invariably *The Macro*. This character is a demonic trickster luring programmers with the
notion that smaller equals simpler and, therefore, better.  But *The Macro's* ultimate goal is obfuscation and he's
remarkably good at achieving that goal. Every other C++ library in use is a testament to this, but I digress. So without
further ado I'll conclude with pointing out the decision to keep *The Macro* out of Java's screenplay proved salutary and
productive. The End.

Oh wait. What about conditional compilation? You know, the other half of the preprocessor. Did Java's designers throw
the baby out with the bathwater by altogether avoiding the preprocessor? Most Java "historians" will cite Java's
*platform independence* as the designers' rationale, after all most usage of `#ifdef` in C/C++ libraries relates to the
many platforms they target. True, however there are a multitude of other dimensions by which a build target may be
determined. For example:
* Java source version (6 v. 8 v. 11)
* Host version (what version of X is running me?)
* Production v. Development v. Test
* Licensed v. Free
* Novice v. Pro
* Feature grouping
* Experimental features
* Prototyping 
* etc.

While you can model your architecture around these dimensions, for example using dependency injection, sometimes that's
a bridge too far, especially when new target dimensions arise with a pre-existing architecture. And sometimes it's not
an either/or proposition -- given the option, some of your architecture can be reasonably refactored, while other
parts may be better off using a preprocessor. In any case having a preprocessor to fall back on is a nice convenience
and deserves a spot in the Java tool chest. This is the rationale behind the new preprocessor from the Manifold project.
  
>Note Java does offer a very limited version of conditional compilation via compile-time constant conditions where
unreachable branches of code are excluded from bytecode. But this type of conditional compilation is restricted to
method bodies, can only reference static final variables, and requires all code to compile regardless of the conditions,
far from a complete solution.  


# Overview

First things first, the preprocessor is a *javac plugin* which means it plugs directly into your Java compiler and runs
as part of javac -- you don't have to add intermediate build steps, manage source generation targets, or any of that.
It also means it's effing *fast* and allows you to easily build multiple targets from a single codebase.

Next, the preprocessor is exclusively designed for conditional compilation, _**No Macros!**_  You can still `#define` a
symbol, but you can't assign a value to it -- a `#define` symbol is always boolean and its value is always `true`,
unless of course the symbol is not defined or is undefined with `#undef`.

As you may have surmised the preprocessor doesn't try to reinvent anything; the directives are taken straight from the
C-family of preprocessors.  These include:

* [`#define`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#define)
* [`#undef`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#undef)
* [`#if`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#if)
* [`#elif`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#elif)
* [`#else`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#eles)
* [`#endif`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#endif)
* [`#error`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#error) 
* [`#warning`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor#warning)

I've hyperlinked Manifold's documentation for these guys.

An extremely useful feature involves the symbols you can reference from `#if`.  Not only can you reference symbols
you've defined with `#define`, you can also define and use symbols from a host of other sources that are visible to all
your project's files. These include `build.properties` files, which you can place in parent directories starting
with the source root, and javac's `-Akey[=value]` compiler arguments.  Additionally, the preprocessor provides builtin
symbols reflecting environment settings such as `JAVA_9_OR_LATER` and `JPMS_NAMED`.

Note unlike with `#define` symbols, you can define symbols with *string values* using `build.properties` and
`-Akey[=value]` compiler arguments.  In this case you can use *equality expressions* `==` and `!=` to test the value:

```java
#if FOO_VERSION == "1.2.0"
  public void foo(Bar bar) {...}
#endif  
``` 
bulid.properties:
```properties
FOO_VERSION=1.2.0
BAR_VERSION=2019.1.2
EXPERIMENTAL=
```

The environment settings symbols can be extra useful if you target multiple Java versions:
```java
public class MyClass implements
#if JAVA_11_OR_LATER
  SomeJava11Interface
#elif JAVA_8_OR_LATER
  SomeJava8Interface
#else
  #error "Unexpected Java source version"
#endif
{
  ...
}  
```

# A Simple Example

Let's dive into the screencast from the preview earlier in the article.
<br>
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();" autoplay loop>
    <source type="video/mp4" src="/images/preprocessor.mp4">
  </video>
</p>
<br>

Here the screencast demonstrates the preprocessor via IntelliJ IDEA using the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold).
The example uses `#define` to define the `MY_API_X` symbol where valid values for `X` are `1` and `2`. Note in real life
we would define this symbol in a `build.properties` file so other files can access it, but here we use `#define` to
simplify the demo.

The `#if` statements use the symbol to conditionally include or exclude code from compilation. As the value of the
symbol changes you can see the code enabling/disabling to reflect the value. Note you can turn this feature off from the
IntelliJ Settings view in the Manifold section, in this mode only the directives are shaded. In either case the
command line compiler always respects the symbol values.

Notice you can place preprocessor directives anywhere in the class: around `import` statements, classes, methods,
fields, pretty much anywhere. This feature is one of many that distinguishes the preprocessor from Java's compile-time
constant based conditional compilation.

Another cool capability you won't find in conventional preprocessors is the use of multiple directives in a single line.
You can see this in action in the `implements` clause.
 
You can also comment out directives, a feature that is not well supported in many preprocessors.

If you have a C++ background, you might be wondering where `#ifdef` and `#if defined` went. They're simply not needed
because with this preprocessor a symbol evaluates to either `true` or `false`, depending on whether or not the symbol is
defined. Only if you use the `==` or `!=` operator can you access a symbol's *string value*, which as stated earlier can
only be defined with `build.properties` or `-Akey[=value]` compiler arguments. Thus, `#if` covers all the bases. Read
more about this is the [docs](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor).

Also of note is the `#elif` directive. This is not a new concept, but if you don't have a C++ background, it may seem
odd. The simple explanation is there's no concise way to say `else if` as you would in Java:

```java
#if FOO
  out.println("FOO");
#else
  #if BAR
    out.println("BAR");
  #else
    #if BAZ
      out.println("BAZ");  
    #endif
  #endif    
#endif
```
It's easier on the eye to use `#elif`:
```java
#if FOO
  out.println("FOO");
#elif BAR
  out.println("BAR");  
#elif BAZ
  out.println("BAZ");  
#endif
```

Finally notice the use of `#error` to respond to an invalid state regarding `MY_API_X`.  This directive produces a
compile-time error at the location of its use. It's perfect for detecting and reporting a misconfigured build at
*compile-time*.
 
 
# Conclusion

Manifold reimagines the time-tested C/C++ preprocessor as a more effective means to meet today's conditional compilation
demands. It directly integrates with your Java compiler so you can quickly and easily build multiple targets from a
single codebase. You can define and use symbols from a variety of sources including properties files and environment
settings to conditionally compile every aspect of your source code. Using plugin support for IntelliJ IDEA you can
visualize exactly how your code reacts to the preprocessor directives and symbols you use. [Check it out](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor).

Check out the [Manifold project](https://github.com/manifold-systems/manifold) for more Java goodness.   

