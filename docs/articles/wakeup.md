# Wakeup static languages, metaprogramming is eating your lunch 

_For at least a couple of decades dynamic languages have gained popularity due in part to the powers of metaprogramming.
Of course, statically typed languages are unable to perform the full spectrum of runtime metaprogramming As a consequence,
they have gained little traction in white-hot fields such as ML and data science. This post addresses this imbalance and presents
some ideas toward a remedy._

# Dark magic

When 2022 wraps up Python will likely eclipse C and Java as the most popular language on planet Earth, according to both
TIOBE and PYPL indexes.

![Python](http://manifold.systems/images/python.png)

Is Python accidentally the perfect ML language to such a degree that it has become the de facto standard? Opinions vary widely on this.
Why then does Python keep winning? Some claim it's not so much the language as it is the badass libraries that are the
main attraction. This is probably closer to the truth considering Python's vast selection of amazing libraries.
However, most of these libraries can thank _metaprogramming_ for their massive traction.

As the name suggests metaprogramming is about writing code that writes code. Generally, leveraging metaprogramming
services a library can dynamically add new types and modify existing types on demand as code executes. Whole
classes can be added, new functions can appear anywhere, existing functions can be modified, and so on. With
metaprogramming a library can, for instance, seamlessly bridge large, otherwise complicated data sets as intuitive
models consisting of interconnected types with tailored query support, user-defined methods, and a host of other
features. The possibilities are limitless.

Consequently, metaprogramming is the dark magic that is at least partly responsible for the incredible uptake of dynamic
languages over the last decade or so. It is a veritable ace up the sleeve for Python.

But there's a catch.

Ideally, features conjured from metaprogramming would be fully available for discovery and
inspection while writing code that uses them. On the contrary, because these features exist exclusively in runtime space, there is no static
type information to help editors and IDEs assist with discovering and using them. Thus, the cataloging of structure,
ownership, references, and names of features produced from metaprogramming is largely foisted on the developer. Much
grok required. As a consequence, Python's reputation for being simple and easy to comprehend can quickly diminish when
powerful libraries are in play. The price of dark coding magic is the darkness.


# Well-lit magic

Code generation alone is not metaprogramming, but it's the only trick static languages possess in their repertoire of magic acts.
Yes, metaprogramming is loosely code generation at runtime, so in theory almost anything generated at runtime can be
generated statically. But look around, no spotlight-grabbing magic Java libraries. The closed-door nature of compiler
APIs may be to blame for this imbalance.

Generally, static code generators are not compiler-driven. They execute as separate build
steps, completely disconnected from the compiled sources employing their services. **Critically, there is no API or
hook into compilers to resolve and produce types as they are referenced; there is no just-in-time code generation, if you will.**
Instead, a code generator runs as a separate build artifact and always produces its full potential universe of types, regardless of the actual set of
types used in source code. Indeed, static code generation is an all-or-nothing, uninformed build event that, among other
horrors, destroys the otherwise productive aspects of incremental compilation. This disconnect is the bane of static
code generation and is why code generators are notorious for slow builds, code bloat, out of sync issues, stale archive
caches, and generally being a pain in the ass.

So, why don't compilers provide _static_ metaprogramming services to lend a hand? That is the $64k question. How cool
would it be to have compiler-driven code gen APIs for popular languages such as Java and C#? If such services did
exist, perhaps there would be much less of a disparity between Python and Java regarding, say, ML and data science?
Static metaprogramming could be a game-changing proposition.

Most importantly, the benefits of static metaprogramming apply to both runtime and _design-time_. This is a crucial benefit
that cannot be overstated. Using the same compiler-defined metaprogramming APIs, tooling such as IDEs can statically
analyze the domain of types and features provided by API implementors, the badass libraries. Thus, IDEs can
seamlessly catalog and project all the cool features conjured from libraries in real time as changes are made. As a
result, developer productivity is off the charts with instant parser feedback, type and feature discovery, code
completion, navigation, deterministic refactoring, true schema-first design, and more.

Imagine:
- dropping a GraphQL schema or query file into your project and immediately accessing its types in your code and then...
- displaying a list of available functions and properties for a GraphQL type and then...  
- navigating to a referenced element in the GraphQL file directly from the code reference and then...
- refactoring the element name from the GraphQL file and having code references instantly reflect the change and then...
- finding all code usages of the element directly from the GraphQL file

[![graphql](http://manifold.systems/images/graphql_slide_1.png)](http://manifold.systems/images/graphql.mp4)
    
^^ true story.

# For the lulz

Using the javac plug-in mechanism I went ahead and built a well-lit [static metaprogramming framework](https://github.com/manifold-systems/manifold)
for the Java compiler, it currently supports JDK versions 8, 11, 17, & 18. It is largely experimental and replete with
unforgivable hacks into javac internals. Along the way I learned a great deal about javac's architecture and have come
to appreciate its codebase. I am grateful for its well composed design, otherwise the Frankenstein-level surgery
required to dynamically add, modify, and replace parts would not have been possible. I'm sure the Oracle Java lords are
overjoyed to learn of this. But I digress.

Anyhow, I built the initial release many years back and have since cobbled together libraries for direct, type-safe access to structured
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
