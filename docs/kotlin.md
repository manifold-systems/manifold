---
layout: default
---

# Using Manifold with Kotlin (and other JVM languages)

Although Manifold is a _Java_ compiler plugin, you can reap many of its benefits from Kotlin and other JVM languages.
For instance, Manifold support for [**GraphQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[**JSON**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
[**XML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml), and
[**Templates**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates) applies
equally to other JVM languages such as Kotlin.

These features are called [_type manifolds_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture)
and they work by automatically transforming resources such as *.graphql*, *.json*, and *.xml* files directly to Java
types without a code generation step or compiling between changes in your IDE. With Kotlin these resources must be placed
in a separate Java module, the indirection allows Manifold to transform the resources into types accessible from Kotlin.
Perhaps the most important detail involves the Manifold IDE plugin. The development experience is identical to Java's.
For instance, you can drop a *.graphql* schema file into your project and immediately use the types it defines. Create your own
*.graphql* query files and type-safely access them from Kotlin. Instantly use changes. All without compiling. Use IntelliJ or
Android Studio to deterministically refactor/rename, find Kotlin usages from GraphQL, navigate to GraphQL declarations,
code completion, etc. The same applies to all Manifold-enabled types including JSON, XML, Templates, etc.
 
## Get the Manifold plugin

Get the Manifold plugin directly from within IntelliJ IDEA or Android Studio via `Settings | Plugins | Marketplace | search "Manifold"`.
You must restart Android Studio to enable the plugin. 

## The Kotlin sample application

The [Kotlin sample application](https://github.com/manifold-systems/manifold-sample-kotlin-app) best illustrates how to
setup your environment and build for use with Manifold. Use it as a reference to use Manifold with your own Kotlin app.

>Note, Kotlin support is limited to [type manifolds](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture).
>Other Manifold features are exclusive to the Java language, these include [the preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor),
>[structural typing](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural),
>[extension classes](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-classes-via-extension),
>[operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading),
>[unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions),
>[type-safe reflection](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-safe-reflection-via-jailbreak),
>and [the Self type](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#the-self-type-via-self).  

