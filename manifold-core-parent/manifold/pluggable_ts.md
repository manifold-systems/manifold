# The game is rigged! Why dynamic languages always win.
                                                                  
_Whether you stand on the static or dynamic side of the fence, the resurgence of dynamic languages is undeniable. This
flies in the face of the recent influx of statically typed languages. State-of-the-art newcomers like Kotlin, Swift, and C#
clearly demonstrate the lunge toward richly featured static type systems, at least within the mainstream language
community. Even dynamic languages are admitting some level of defeat by including type annotations to mitigate pitfalls
inherent in their type systems. By now one would assume dynamic languages would be waning in popularity. What is going
on here?_
 
# Dynamic superpower

If programming language surveys are any indication, dynamic languages are on the rise (again). For instance, the TIOBE index has Python
as the second most popular programming language for 2021, just under C. When 2022 wraps up Python will likely eclipse
C and Java as the most popular language on planet Earth, according to both TIOBE and PYPL indexes.  

![Python](http://manifold.systems/images/python.png)

Is Python accidentally the perfect ML language to such a degree that it has become the de facto standard? Probably not.
Why then does Python keep winning? Some claim it's not so much the language as it is the badass libraries that attract
so many programmers. This is much closer to the truth, as there is a vast selection of amazing libraries to choose from.
However, most of those libraries can thank **metaprogramming** for their massive traction.  

As the name suggests metaprogramming is about writing code that writes code. Generally, leveraging metaprogramming
services, a library can dynamically add new types and modify existing types as needed during runtime. Whole
classes can be added, new functions can appear anywhere, existing functions can be modified, and so on.

Using metaprogramming services a library can magically add new entity classes mirroring a database, inject HTTP support
for data classes, weave user-defined methods into a domain model, and so on. The possibilities are limitless. 

Consequently, metaprogramming is the superpower that is at least partly responsible for the incredible uptake of dynamic
languages over the last few years. This is also the reason there are no such libraries for Java, Kotlin, C#, etc. Static
languages are compiled, therefore runtime metaprogramming is out of the picture. But considering runtime metaprogramming has its
own set of problems, perhaps this is a blessing in disguise.

# Static superpower?

Code generation alone is not metaprogramming, but this is all static languages have in their repertoire of magic acts.
Yes, metaprogramming is loosely code generation at runtime, so in theory almost anything generated at runtime can be
generated statically. But look around, no magical Java libraries. Only pain and suffering. The compiler disconnect is
the problem. 
                              
Generally, aside from the limited utility of annotation processors, code generators are not compiler-driven. They execute as separate build
steps, completely disconnected from the compiled sources employing their services. For example, there is no SPI or
any way to hook into the compiler's type system to resolve types as they are encountered. Instead, a code generator runs as
a separate build artifact and always produces its full potential universe of types, regardless of the actual set of
types used in source code. Indeed, static code generation is an all-or-nothing, uninformed build event that, among other
horrors, destroys the otherwise productive gains with incremental compilation. This disconnect is the bane of static
code generation and is why code generators are notorious for slow builds, code bloat, out of sync issues, stale archive
caches, and generally being a pain in the ass.

So why don't compilers provide static metaprogramming services? That is the $64k question. How cool would it be to have
compiler metaprogramming SPIs for popular languages such as Java and C#? If such services did exist, perhaps there would
be much less of a disparity between Python and Java regarding, say, ML? In my view metaprogramming is such a game changer I
rank it up there with static typing as a key differentiating feature.

Most importantly, the benefits of static metaprogramming apply to both runtime and _design-time_. This is a crucial advantage
that cannot be overstated. Because all the new types and other features provided by metaprogramming are available at compile-time,
IDEs can universally provide amazing developer productivity features including type and feature discovery, code completion,
navigation, deterministic refactoring, etc. Imagine refactoring an element name in a JSON file and having code references
change accordingly, without running a code generator or writing one-off IDE plugins. Static analysis is gold.


# Insanity
          
Using the javac plug-in mechanism I went ahead and built a [metaprogramming framework](https://github.com/manifold-systems/manifold)
for the Java compiler. It is largely experimental and replete with unforgivable hacks into javac internals. Along the way
I learned a great deal about javac's architecture and came to appreciate its codebase. I am grateful for its well composed
design, otherwise the Frankenstein-level surgery required to dynamically add, modify, and replace parts would not have
been possible. I'm sure the Oracle Java lords are overjoyed to learn of this. But I digress.

Anyhow, I built this a few years back and have since cobbled together libraries for direct, type-safe access to structured
resources. These include GraphQL, JSON, XML, JavaScript, Templates, and several others. Projects can simply add these libraries
as dependencies to trick the compiler into believing JSON files, for example, are Java types. No separate build steps, no
agents or class loader shenanigans, naturally incremental, schema-first oriented, and fully IDE integrated.

The components produced from this framework are called "type manifolds" because they more or less bolt onto javac's type system
as integration conduit. Building on this concept, another kind of type manifold is the "extension manifold." But instead
of providing new types, the extension manifold targets existing types so that features such as new methods, interfaces,
and annotations may be logically added to types you otherwise can't modify.

Beyond first-order metaprogramming, several experimental language features have spawned from this effort. They stem
from the extension manifold concept and delve deeper into javac's architecture. Instead of adding features to types,
they add features to the Java language itself. These include operator overloading, properties, tuples, structural
typing, type-safe reflection, and more. Many are à la carte as separate dependencies.

Yes, this is all utterly insane, but I did it anyway for the lulz.

Check it out on [github](https://github.com/manifold-systems/manifold).



