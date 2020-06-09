---
layout: default
---

# Using Manifold with Kotlin (and other JVM languages)

Although Manifold is a _Java_ compiler plugin, you can reap many of its benefits from Kotlin and other JVM languages.
For instance, Manifold support for [**GraphQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[**JSON**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
[**XML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml), and
[**Templates**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates) work
equally well with other JVM languages such as Kotlin.

These features are called [_type manifolds_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture)
and they work by automatically transforming resources such as *.graphql*, *.json*, and *.xml* files directly to Java
types without a code generation step or compiling between changes in your IDE. The only difference with Kotlin is the
resource files must be placed in a separate Java module. The indirection allows Manifold to transform the resources into
Java types behind the scenes. Perhaps the most important detail is that via the Manifold IDE plugin, Kotlin can use
Manifold just as effectively as Java. The development experience is identical. For instance, you can make GraphQL
changes and instantly access them in Kotlin code, no compilation in between. Use IntelliJ or Android Studio to
deterministically refactor/rename, find Kotlin usages from GraphQL, code completion, etc. 
 
## Get the Manifold plugin

Get the Manifold plugin directly from within IntelliJ IDEA or Android Studio via `Settings | Plugins | Marketplace | search "Manifold"`.
You must restart Android Studio to enable the plugin. 

## The Kotlin sample application

The [Kotlin sample application](https://github.com/manifold-systems/manifold-sample-kotlin-app) best illustrates how to
setup your environment and build for use with Manifold. Use it as a reference to use Manifold with your own Kotlin app.



