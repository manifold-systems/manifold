---
layout: default
---

# Manifold FAQ - Frequently Asked Questions

[Common Questions](#common-questions) • [Getting Help](#getting-help) • [Troubleshooting](#troubleshooting)

# Common Questions

#### Q: Does Manifold support Java 21? 17? 8?
Yes -- Manifold fully supports all LTS versions since JDK 8, including 8, 11, 17, and 21. Each release keeps pace with
the JDK, targeting the latest version--LTS or not--and is on track for JDK 25 and beyond.

#### Q: Manifold relies on internal compiler APIs. Could this cause it to break in a future version of Java?
Highly improbable. Manifold consistently adapts ahead of JDK releases, having tracked 14 major Java versions since its
debut--always well ahead of Oracle’s official releases.

This kind of access isn’t unique to Manifold--many essential tools and frameworks, including IntelliJ and Spring, rely
on it as well. It’s therefore extremely unlikely that Oracle would introduce drastic changes that would actively block
such widespread usage.

#### Q: Doesn't Kotlin already do all this?
No, it doesn't. While Kotlin offers some features like properties, it lacks most of what Manifold provides. For example,
Kotlin doesn’t have anything equivalent to Manifold’s core feature: the type manifold, a static metaprogramming API which
powers `manifold-sql`, `manifold-json`, `manifold-graphql`, `manifold-xml`, and more.

Additionally, many of Manifold’s unique extensions to Java are not present in Kotlin, including:

- True delegation
- Extension classes
- Structural typing
- Tuples
- Unit expressions
- Type-safe templates
- Conditional compilation
- ...and more.

Importantly, Kotlin is a separate language, while Manifold is a compiler plugin that enhances Java. You can add Manifold
to any Java project and pick the features you want, without leaving the Java ecosystem.

#### Q: Is Manifold free?
Yes, Manifold is [open source](https://github.com/manifold-systems/manifold) and available on GitHub, free for use via Apache License 2.0.

The Manifold [plugins for IntelliJ and Android Studio](https://plugins.jetbrains.com/plugin/10057-manifold/) are also
[open source](https://github.com/manifold-systems/manifold-ij) and free to use.

#### Q: Does Manifold work with Maven or Gradle?
Yes. Please refer to the [Maven](http://manifold.systems/docs.html#maven) and [Gradle](http://manifold.systems/docs.html#gradle)
sections of the [Setup](https://github.com/manifold-systems/manifold#projects) documentation for detailed instructions
based on the subproject(s) you're using.

#### Q: Does Manifold provide IDE support?
Yes. The [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) provides comprehensive support for
**IntelliJ IDEA** and **Android Studio**.

To download or update the plugin, go directly through the IDE:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

#### Q: How do I set up a Manifold feature like `manifold-sql`, `manifold-json`, etc.?
Add the corresponding `manifold-*` dependency (or dependencies) to your project, and include the `-Xplugin:Manifold`
compiler argument. [Setup](https://github.com/manifold-systems/manifold#projects) can vary slightly depending on whether
you're using Java 8 or Java 9+, so refer to the Setup section in the subproject’s README for full details. 

### Q: Who uses Manifold?

Here are some companies currently using Manifold:

<img width="70%" src="http://manifold.systems/images/companies.png">

# Getting Help

#### Q: Where can I find help?

**Discussions**  
Join our [Discord server](https://discord.gg/9x2pCPAASn) to start a discussion, ask questions, provide feedback, and more.

**Report a Bug**  
The Manifold project uses GitHub Issues to track bugs, features, and other requests. If you discover a bug, have a feature
request, or an idea, go [here](https://github.com/manifold-systems/manifold/issues) and let it be known. Expect a response within 24 hours.

**Private Email**  
If your question or issue is more urgent or confidential, don't hesitate to email us at [info@manifold.systems](mailto:info@manifold.systems).

# Troubleshooting

#### Q: I updated to the latest Manifold IntelliJ plugin, but now IntelliJ is showing error messages. What's going on?
This usually happens when your project dependencies are out of sync with the latest version of Manifold. When you load
your project, the plugin will display a warning message indicating which version of Manifold you need to update to.

You can always find the latest releases [here](https://github.com/manifold-systems/manifold/tags).

**Important:** If you're using Maven or Gradle, be sure to update your build files. Do not modify module dependencies
directly in IntelliJ’s UI. For detailed setup instructions, refer to the [Maven](http://manifold.systems/docs.html#maven)
and [Gradle](http://manifold.systems/docs.html#gradle) sections in the [Setup](https://github.com/manifold-systems/manifold#projects)
documentation for the specific subprojects you are using.

If you're still having trouble, feel free to [reach out](https://discord.gg/9x2pCPAASn)--there's a good chance someone
else has encountered the same issue, and help will be on the way!

#### Q: I defined some useful *extension methods*, but they aren't showing up in my other project. How can I share them as a dependency?

The module containing your extension methods must declare that it should be processed for extension methods. This is done
using the `Contains-Sources` JAR *manifest entry*. For example, with Maven:

```xml
<manifestEntries>
    <!-- Class files as source must be available for extension method classes -->
    <Contains-Sources>java,class</Contains-Sources>
</manifestEntries>
```
For more details on creating [extension libraries](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-libraries),
refer to the documentation.

### Q: I defined a new *extension class* with some extension methods, but IntelliJ flags their usage as errors. What's going on?
This issue typically happens when IntelliJ's cache is out of sync with the extensions. If IntelliJ flags legal usage of
a Manifold feature as an error, you can often fix it by making a small change at the use site and then undoing it.

If that doesn't resolve the problem, try closing and reloading the project. If the issue persists, please report it as
an [issue](https://github.com/manifold-systems/manifold/issues) on Manifold's GitHub repo.
