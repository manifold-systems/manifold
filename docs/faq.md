---
layout: default
---

# Manifold F.A.Q.s - Frequently Asked Questions (and Answers)

[Common Questions](#common-questions) • [Getting Help](#getting-help) • [Troubleshooting](#troubleshooting)


## Common Questions

#### Q: Does Manifold support Java 11? 12? 8?
Yes.  Manifold fully supports Java 8 - 12.  Manifold also fully supports the Java Platform Module System (JPMS).  See the
[Setup Guide](http://manifold.systems/docs.html#setup) for more info.

#### Q: Manifold is somehow using Java internal APIs to do its magic. Could it break in a future version of Java?
Unlikely.  Java internal APIs can change from version to version, however Manifold always adjusts to changes ahead of Java
releases.  To understand this better consider Manifold currently works with Java versions 8, 9, 10, 11, 12, and soon 13. 
The internal APIs have changed significantly between all those versions, yet Manifold continues to work and improve
along the way.

#### Q: Is Manifold free?
Yes, Manifold is an [open source](https://github.com/manifold-systems/manifold) project available on github freely 
available for use via Apache License 2.0.

#### Q: Does Manifold work with Maven?  Gradle?
Yes.  Using Maven and Gradle with Manifold is simple.  Please refer to the [Maven](http://manifold.systems/docs.html#maven) 
and [Gradle](http://manifold.systems/docs.html#gradle) sections of the [Manifold Setup](http://manifold.systems/docs.html#setup) 
instructions. 

#### Q: Does Manifold provide IDE support?
Yes.  IntelliJ IDEA provides comprehensive support for Manifold.  Download the plugin directly from IntelliJ 

#### Q: How do I get the latest Intellij plugin updates?
Install the Manifold plugin from the JetBrains repository available directly via: 

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`
  
IntelliJ notifies you within 24 hours when an update is available and gives you the opportunity to sync.

#### Q: How do I get String templates working, like `"Count: $count"`? 
Just add the `manifold-strings` dependency to your project along with the `-Xplugin:Manifold` javac argument. See the
[Build](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings#build) docs for more info.

## Getting Help

#### Q: Where can I find help?
**Report A Bug**
We use github issues to track bugs, features, and other requests.  Please (pretty please with sugar on top) if you
discover a bug, have a feature request, or an idea go [here](https://github.com/manifold-systems/manifold/issues) and let us know. We'll
respond within 24 hours and work in the Pacific Time Zone.

**Private E-mail**
If your question or issue is more pressing or confidential, don't hesitate to send an email to [info@manifold.systems](mailto:info@manifold.systems).
We'll respond ASAP.

#### Q: I've read the docs page.  Can I learn more about Manifold elsewhere?

Links to recently published Manifold articles are available on the [Articles](http://manifold.systems/articles/articles.html) 
page.  There is always another article on the way, check back for more. 

## Troubleshooting

#### Q: I updated to the latest Manifold IntelliJ plugin and now IntelliJ is complaining with error messages.  What is wrong?
You probably need to update your project dependencies to use the latest manifold release.  If your project's
dependencies are out of sync, the plugin tells you which version of manifold you need with in a warning message
when you load your project.  You can find the latest releases [here](https://github.com/manifold-systems/manifold/tags).

**Important:** If you are using Maven or Gradle, you must update your build files -- do not change the Module dependencies from 
IntelliJ's UI. Please refer to the [Maven](http://manifold.systems/docs.html#maven) and [Gradle](http://manifold.systems/docs.html#gradle)
sections of the [Manifold Setup](http://manifold.systems/docs.html#setup) instructions. 

Please [let us know](https://github.com/manifold-systems/manifold/issues) if you can't get it working, we're happy to 
help.