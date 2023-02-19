# Say Goodbye to Checked Exceptions

Modern languages don't do checked exceptions. But you don't have to jump ship to share the experience. In this article
Scott McKinney shows you how to stick with Java and completely neutralize checked exceptions with a simple new addition
to the [Manifold framework](http://manifold.systems/).
 
## Overview

Most contemporary JVM languages including Scala, [Kotlin](https://kotlinlang.org/docs/reference/exceptions.html), and [Ceylon](https://ceylon-lang.org/documentation/1.3/reference/statement/throw/),
do not distinguish between checked and unchecked exceptions, they're all treated as unchecked.  Similarly, the .NET CLR
does not have checked exceptions.  This is not a coincidence as there is an abundance of evidence to backup their
collective decision not to follow Java's example. Since this subject can be rather controversial, I'll defer to one of
my favorite language authors, Anders Hejlsberg, on the subject with [The Trouble with Checked Exceptions](https://www.artima.com/intv/handcuffs.html)
and move on.

## Exhibit A

Checked exceptions tend to result in boilerplate code where in order to handle an exception a call site is nested in a
`try/catch` statement and the checked exception is wrapped in an unchecked exception and rethrown:

```java
URL url;
try {
    url = new URL("http://manifold.systems/");
} catch (MalformedURLException e) {
    throw new RuntimeException(e);  // the boilerplate of boilerplates
}
try (BufferedReader reader = new BufferedReader( new InputStreamReader(url.openStream()))) {
  reader.lines().forEach(out::println);
} catch (IOException e) {
    // Whoops! Unintentionally swallowed!
}
```

As you can see the code is made much less readable with checked exceptions. Unfortunately this is not the worst part.
You can see one of the exceptions is not rethrown, but is instead *ignored*.  Unintentional exception swallowing is
a common cause for critical, hard to resolve bugs.

What is most frustrating, however, is all of this boilerplate code is written, basically, to say "I don't handle this,
please go away."  Indeed what you really mean to write is this:

```java
URL url = new URL("http://manifold.systems/");
try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
    reader.lines().forEach(out::println);
}
```

Concise, readable, and exception friendly.


## Good News!
 
With Manifold's new `manifold-exceptions` dependency you can now *choose* whether or not your Java project enforces
checked exceptions.  If you like checked exceptions, that's fine, simply do nothing.  Otherwise, you can effectively
neutralize checked exceptions and improve your productivity simply by adding a dependency and a javac argument to your
existing project:

```groovy
dependencies {
    // Add manifold-exceptions to neutralize checked exceptions
    compile group: 'systems.manifold', name: 'manifold-exceptions', version: '2023.1.3'

    // Add manifold-exceptions to -processorpath for javac (for Java 9+, not needed for Java 8)
    annotationProcessor group: 'systems.manifold', name: 'manifold-exceptions', version: '2023.1.3'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```

Now checked exceptions can safely rise unimpeded and you can avoid writing unsightly boilerplate madness. Additionally,
you can use the [Manifold plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold) to write clean
code in a nice, fully Manifold-aware environment.  

Enjoy!

>Read more about the [`manifold-exception` dependency](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)
online. While you're there check out more of the [Manifold framework](http://manifold.systems/) including [extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext),
comprehensive, type-safe access to [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[JSON](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json), and a ton more.

