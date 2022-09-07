# The game is rigged! Why dynamic languages win.

_Whether you stand on the static or dynamic side of the fence, the growing popularity of dynamic languages like Python
is undeniable. This flies in the face of the recent influx of statically typed languages. State-of-the-art newcomers
like Kotlin, Swift, and TypeScript clearly represent a collective leap toward richly featured static type systems. Even
dynamic languages are joining in by offering type annotations to mitigate the pitfalls inherent in their type systems.
By now one would assume dynamic languages would be waning in popularity, not gaining. What is going on here?_

# Dark magic

If programming language surveys are any indication, dynamic languages are here to stay. For instance, the TIOBE index has Python
as the second most popular programming language for 2021, just under C. When 2022 wraps up Python will likely eclipse
C and Java as the most popular language on planet Earth, according to both TIOBE and PYPL indexes.

![Python](http://manifold.systems/images/python.png)

Is Python accidentally the perfect ML language to such a degree that it has become the de facto standard? Opinions vary widely on this.
Why then does Python keep winning? Some claim it's not so much the language as it is the badass libraries that attract
so many programmers. This is probably much closer to the truth considering Python's vast selection of amazing libraries.
However, most of these libraries can thank _metaprogramming_ for their massive traction.

As the name suggests metaprogramming is about writing code that writes code. Generally, leveraging metaprogramming
services a library can dynamically add new types and modify existing types on demand as code executes. Whole
classes can be added, new functions can appear anywhere, existing functions can be modified, and so on.

For instance, with metaprogramming a library can seamlessly bridge large complicated data sets to a programmer's project
as intuitive models consisting of interconnected types with tailored query support, user-defined methods, and so on. The
possibilities are limitless.

Consequently, metaprogramming is the dark magic that is at least partly responsible for the incredible uptake of dynamic
languages over the last decade or so. It is the ace up the sleeve of Python, Ruby, R, Lua, and others.

But there's a catch.

Ideally, features conjured up from metaprogramming would be fully available for discovery and
inspection while editing code that uses them. But these features exist exclusively in runtime space, there is no static
type information to help editors and IDEs assist with discovering and using them. Thus, the cataloging of structure,
ownership, references, and names of features produced from metaprogramming is largely foisted on the developer's memory
and library expertise. Much grok required. As a consequence, Python's reputation for being simple and easy to
comprehend quickly diminishes when powerful libraries are in play. The price of dark coding magic is the darkness.


# Well-lit magic

Code generation alone is not metaprogramming, but it's the only trick static languages possess in their repertoire of magic acts.
Yes, metaprogramming is loosely code generation at runtime, so in theory almost anything generated at runtime can be
generated statically. But look around, no spotlight-grabbing magic Java libraries. We can blame the closed-door nature
of compiler APIs for this imbalance.

Generally, static code generators are not compiler-driven. They execute as separate build
steps, completely disconnected from the compiled sources employing their services. For example, there is no SPI or
any way to hook into the compiler to resolve and produce types as they are _referenced_. Instead, a code generator runs as
a separate build artifact and always produces its full potential universe of types, regardless of the actual set of
types used in source code. Indeed, static code generation is an all-or-nothing, uninformed build event that, among other
horrors, destroys the otherwise productive aspects of incremental compilation. This disconnect is the bane of static
code generation and is why code generators are notorious for slow builds, code bloat, out of sync issues, stale archive
caches, and generally being a pain in the ass.

So, why don't compilers provide _static_ metaprogramming services to lend a hand? That is the $64k question. How cool would it be to have
compiler metaprogramming SPIs for popular languages such as Java and C#? If such services did exist, perhaps there would
be much less of a disparity between Python and Java regarding, say, ML and data science? In my view metaprogramming is
a game changing, key differentiating factor.

Most importantly, the benefits of static metaprogramming apply to both runtime and _design-time_. This is a crucial advantage
that cannot be overstated. All the new types and other features provided by metaprogramming are available at compile-time.
As a result, IDEs can use the compiler's metaprogramming SPIs to universally provide amazing developer productivity
features including parser feedback, type and feature discovery, code completion, navigation, deterministic refactoring,
and more.

Imagine:
- referencing elements defined in a GraphQL file directly and type-safely in your code and then...
- navigating to a referenced element in the GraphQL file from a call site and then...
- refactoring the element name from the GraphQL file and having code references instantly reflect the change and then...
- finding all code usages directly from the GraphQL element

All with deterministic precision made possible with static analysis.

[![graphql](http://manifold.systems/images/graphql_slide_1.png)](http://manifold.systems/images/graphql.mp4)
    
^^ true story.

# Insanity

Using the javac plug-in mechanism I went ahead and built a [static metaprogramming framework](https://github.com/manifold-systems/manifold)
for the Java compiler, it currently supports JDK versions 8, 11, 17, & 18. It is largely experimental and replete with
unforgivable hacks into javac internals. Along the way I learned a great deal about javac's architecture and have come
to appreciate its codebase. I am grateful for its well composed design, otherwise the Frankenstein-level surgery
required to dynamically add, modify, and replace parts would not have been possible. I'm sure the Oracle Java lords are
overjoyed to learn of this. But I digress.

Anyhow, I built this a few years back and have since cobbled together libraries for direct, type-safe access to structured
resources. These include GraphQL, JSON, XML, JavaScript, Java Templates, and several others. Simply add these libraries
as project dependencies to trick the compiler into believing JSON files, for example, are Java types. No separate build steps, no
agents or class loader shenanigans, naturally incremental, schema-first oriented, and fully IDE integrated (IntelliJ).

The components produced from this framework are called "type manifolds" because they more or less bolt onto javac's type system
as integration conduit. Building on this concept, another kind of type manifold is the "extension manifold." But instead
of providing new types, the extension manifold targets existing types so that features such as new methods, interfaces, and annotations may
be logically added to types that otherwise can't be modified.

Beyond first-order metaprogramming, several experimental language features have spawned from this effort. They stem
from the extension manifold concept and delve deeper into javac's architecture. Instead of adding features to types,
they add features to the Java language itself. These include operator overloading, properties, tuples, structural
typing, type-safe reflection, and more. Many are provided à la carte as separate dependencies.

Yes, this is all utterly insane, I know, but I did it anyway for the lulz.

Check it out on [github](https://github.com/manifold-systems/manifold).

Thanks for reading.
