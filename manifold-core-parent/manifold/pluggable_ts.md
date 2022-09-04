# The game is rigged! Why dynamic languages always win.
                                                                  
_Whether you stand on the static or dynamic side of the fence, the resurgence of dynamic languages is undeniable. This
is in the face of all the recent attention toward static typing. Newer languages like Swift, Kotlin, Scala, and TypeScript
demonstrate just how powerful static typing can be. Add to that the many dynamic languages recently including type
hinting and type annotations to mitigate dynamic typing pitfalls, one would assume dynamic languages would be on their
way out. What is going on?_
 
# Dynamic superpower

If programming language surveys are any indication, dynamic languages are on the rise (again). For instance, the TIOBE index has Python
at the second most popular programming language for 2021, just under C. When 2022 wraps up Python will likely eclipse
C and Java as the most popular language on planet Earth, according to both TIOBE and PYPL indexes.  

![Python](http://manifold.systems/images/python.png)

Is Python accidentally the perfect ML language to such a degree that it has become the de facto standard? Of course not.
You know why Python and most other dynamic languages keep winning? Some claim it's not so much the language as it is the
badass libraries that attract programmers. And they're right. However, most of those libraries can thank **metaprogramming**
for their massive traction.  

As the name suggests metaprogramming is about writing code that writes code. Generally, leveraging metaprogramming
services, a library can dynamically add new types and modify existing types as needed during runtime. Whole
classes can be added, new functions can appear anywhere, existing functions can be modified, and so on.

Consequently, metaprogramming is the superpower that is at least partly responsible for the incredible uptake of dynamic
languages over the last few years. This is also the reason there are no such libraries for Java, Swift, Kotlin, C#, etc. Static
languages are compiled, therefore runtime metaprogramming is not much of an option. But considering runtime metaprogramming has its
own set of problems, perhaps this is a blessing in disguise.

# Static superpower?

Static languages do have a lowly form of metaprogramming via static code generators, which are crude and painful compared with using
libraries that employ actual metaprogramming. Yes, metaprogramming is loosely code generation at runtime, so in theory almost anything
generated at runtime can be generated statically. But look around, no magical Java libraries. The compiler disconnect is the problem. 
                              
Generally, aside from the limited utility of annotation processors, code generators are not compiler-driven. They execute as separate build
steps, completely disconnected from the compiled sources employing their services. For example, there is no SPI or
integration with the compiler's type system to resolve types as they are encountered. Instead, a code generator runs as
a separate build artifact and always produces its full potential universe of types, regardless of the actual set of
types used in source code. Indeed, static code generation is an all-or-nothing build event; bye-bye incremental compilation. This
disconnect is the bane of static code generation and is why code generators are notorious for slow builds, code bloat,
out of sync issues, stale archive caches, and generally being a pain in the ass.

So why don't compilers provide static metaprogramming services? That is the $64k question. How cool would it be to have
compiler metaprogramming SPIs for popular languages such as Java and C#? If such services did exist, perhaps there would
be much less of a disparity between Python and Java regarding, say, ML? In my view metaprogramming is such a game changer I
rank it up there with static typing as a key differentiating feature.

Most importantly, the utility of static metaprogramming applies to both runtime and compile-time. This is a huge advantage.
Because all the new types and other features are available at compile-time, IDEs can universally provide productive developer
assistance including type and feature discovery, code completion, navigation, deterministic refactoring, etc. Static
analysis is gold.


# Insanity
          
Using the javac plug-in mechanism I went ahead and built a [metaprogramming SPI](https://github.com/manifold-systems/manifold)
for the Java compiler. It is largely experimental and replete with unforgivable hacks into javac internals. Along the way
I learned a great deal about javac's architecture and came to appreciate its codebase. It is extremely well-designed,
I would otherwise not have been able to perform Frankenstein-level surgery on it. Anyhow, I built this a few years back
and have since cobbled together libraries for direct, type-safe access to structured resources including GraphQL, JSON,
XML, JavaScript, Templates, and several others. Projects can simply add these libraries as dependencies to trick the
compiler into believing JSON files, for example, are Java types. No separate build steps, no agents or class loader shenanigans,
naturally incremental, schema-first oriented, and fully IDE integrated.

The components produced from this SPI are called "type manifolds" because they more or less bolt onto javac's type system
as integration conduit. Another kind of type manifold is the "extension manifold." Instead of targeting resources, the
extension manifold targets Java itself so that features such as new methods, interfaces, and annotations may
be logically added to existing types.

Beyond metaprogramming, several experimental language features have spawned from this effort. They stem
from the extension manifold concept and delve deeper into javac's architecture. Instead of adding features to types,
they add features to the Java language itself. These include operator overloading, properties, tuples, structural
typing, type-safe reflection, and more. Many are à la carte as separate dependencies.

Yes, this is all utterly insane, but I did it anyway for the lulz.

Feel free to join in the lulz. Visit the [github project](https://github.com/manifold-systems/manifold) and the
[slack channel](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg).



