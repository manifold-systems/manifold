# The game is rigged! Why dynamic languages always win.

# Dynamic superpower

If programming language surveys are any indication, dynamic languages are on the rise (again). For instance, the TIOBE index has Python
at the second most popular programming language for 2021, just under C. When 2022 wraps up Python will likely eclipse
C and Java as the most popular language on planet Earth, according to both TIOBE and PYPL indexes.  

![Python](http://manifold.systems/images/python.png)

Wtf? Is Python accidentally the perfect ML language to such a degree that it has become the de facto standard?
Hell no. You know why Python and most other dynamic languages keep winning? One word: Metaprogramming. Some claim it's
the badass libraries that keep 'em coming. And they're right. However, most of those libraries can thank metaprogramming
for their massive traction.  

As the name suggests metaprogramming is about writing code that writes code. Generally, using a language's
metaprogramming API a library can dynamically add new types and modify existing types as needed during runtime. Whole
classes can be added, new functions can appear anywhere, existing functions can be modified, and so on.

Consequently, metaprogramming is the superpower that is at least partly responsible for the incredible uptake of dynamic languages like Python.
This is also the reason there are no such libraries for Java, Go, Kotlin, Scala, C#, etc. Static languages are
compiled, runtime metaprogramming is just not an option.

# Static superpower?

Static languages do have a lowly form of metaprogramming called code generation, which is crude and painful compared with using
libraries that employ metaprogramming. Yes, metaprogramming is just code generation at runtime, so in theory most anything
generated with metaprogramming can be generated statically with a code generator. But look around, no badass libraries
for Java. The disconnect is the problem. 
                              
Aside from annotation processors, code generators do not
execute within the compiler. They execute as separate build steps, completely disconnected from the compiled sources that
use their goods. For example, there is no API or integration with the compiler's type system to resolve types as they are
encountered. Instead, a code generator runs as a separate build artifact and always produces its full potential universe of
types, regardless of the actual set of types that are used. This disconnect is the bane of static code generation and
is why code generators are notorious for slow builds, code bloat, out of sync issues, stale archive caches, and generally
being a pain in the ass.

So why don't compilers provide static metaprogramming APIs? That is the $64k question. How cool would it be to have a
metaprogramming API for popular static languages such as Java, C#, etc.?  Maybe I'm crazy, but it is my belief that if
such APIs did exist, there would be no disparity between Python and Java regarding, say, ML. I'm a static language
asshole, so I'd go a step further and say Python would not have a leg to stand on if Java, C#, etc. added static
metaprogramming to the mix. Shrug.

# Insanity
          
Using the Plugin API I went ahead and built a [static metaprogramming library](https://github.com/manifold-systems/manifold) for the Java compiler. It is replete with
unforgivable hacks to javac's internals. But I have to say, I have come to appreciate the Java compiler codebase.
It is extremely well-designed, I would otherwise not have been able to perform Frankenstein-level surgery on it. Anyhow,
I built this a few years back and have since cobbled together libraries for direct, type-safe access to structured data
resources like GraphQL, JSON, XML, and even JavaScript, and others. They trick the compiler into believing JSON files,
for example, are Java types.

I call the components produced from this API "type manifolds" because they more or less bolt onto javac's type system
as integration conduit. Another kind of type manifold is the "extension manifold." Instead of targeting external structured
data, the extension manifold targets Java itself so that extensions such as new methods can be logically added to existing
types. A host of experimental language features stem from this concept including operator overloading, properties, tuples, structural
typing, etc. These are all à la carte as separate dependencies.

Yes, this is all completely insane.


