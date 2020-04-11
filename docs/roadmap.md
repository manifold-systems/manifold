---
layout: default
---

# Roadmap
 
![roadmap](http://manifold.systems/images/roadmap.jpg)
 
## On the workbench

I've taken a bit of a detour recently to experiment with supporting the [Gosu](https://github.com/gosu-lang/gosu-lang)
programming language as a type manifold. My employer, Guidewire Software, uses the language heavily, as do their
customers. Some manage Gosu assets in excess of a million lines. As the language's author I'm excited at the
prospect of using Manifold to improve and extend Gosu.

**My goals**:
* **One compiler.** Use `javac` as Gosu's compiler. Manifold makes this relatively easy.
* **Java interop.** Sharing javac's type system via Manifold should lead to comprehensive Java/Gosu interop.
* **Performance.** Use javac's type system to avoid the overhead of having to reparse/reprocess types otherwise involved
with two compilers.
* **Type system expansion.** Utilize all the types made available from Manifold; GraphQL, JSON, and Properties in particular.  
<br>

Note this is a very temporary detour.  I'll soon return to the normal Manifold grind, adding bugs.. er features and
fixing bugs ;)

## On the pile (in no particular order)
 
#### Operator overloading enhancements
* [#126](https://github.com/manifold-systems/manifold/issues/126)

#### New schema type manifolds 
* [#111](https://github.com/manifold-systems/manifold/issues/111)

#### Manifold "inliner" (aka De-Manifold) tool
* [#95](https://github.com/manifold-systems/manifold/issues/95)

#### Default parameter values
* [#93](https://github.com/manifold-systems/manifold/issues/93)

#### Android support
* [#77](https://github.com/manifold-systems/manifold/issues/77)

#### Manifold plugin for VS Code (which is Eclipse)
* [#142](https://github.com/manifold-systems/manifold/issues/142)

#### Manifold plugin for Eclipse
* [#18](https://github.com/manifold-systems/manifold/issues/18)

