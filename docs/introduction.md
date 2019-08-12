---
layout: default
---

## Introducing _Manifold_!
[Manifold](http://manifold.systems) is a breakthrough technology you can use to seamlessly extend Java to compile and
load types from non-Java source files.  Using this framework your code has direct, type-safe access to metadata such as
GraphQL, YAML & JSON Schema files, SQL, and even other programming languages. Building on this foundation Manifold
provides an ever growing set of extensions, including comprehensive support for
[GraphQL](http://manifold.systems/docs.html#graphql),
[YAML and JSON Schema](http://manifold.systems/docs.html#json-and-json-schema),
[extension methods](http://manifold.systems/docs.html#extension-classes),
full featured [templates](http://manifold.systems/manifold-templates.html),
[string interpolation](http://manifold.systems/docs.html#templating), and a lot more.

Manifold fulfills the promise: _**your metadata is the single source of truth**_. There is *nothing* to manage between your metadata and your
code -- no code generation steps in your build, no POJOs, no annotation processor steps, no custom class loaders, no runtime agents.

All features are fully supported in IntelliJ IDEA.  Author GraphQL schema files and code against them as you make changes without
a code generation step. Jump directly to a YAML property from a call site in your code. Quickly rename a JSON field and its
usages across your codebase.  Use Hotswap to make and test changes to files while debugging.  Author templates with the full
expressive power of Java and use them type-safely in your code.  Etc.

Manifold is easy to use, it's just a dependency you add to your existing project.  See the
[Setup Guide](http://manifold.systems/docs.html#setup) for details.
