---
layout: default
---

# Manifold F.A.Q.s - Frequently Asked Questions (and Answers)

[Common Questions](#common-questions) • [Getting Help](#getting-help) • [Troubleshooting](#troubleshooting)


## Common Questions

#### Q: Does Manifold support Java 11? 13? 8?
Yes.  Manifold fully supports Java 8 - 13.  Manifold also fully supports the Java Platform Module System (JPMS).  See the
[Setup Guide](http://manifold.systems/docs.html#setup) for more info.

#### Q: Manifold is somehow using Java internal APIs to do its magic. Could it break in a future version of Java?
Unlikely.  Java internal APIs can change from version to version, however Manifold always adjusts to changes ahead of Java
releases.  To understand this better consider Manifold currently works equally well with Java versions 8, 9, 10, 11, 12,
and 13. Over time the internal APIs do indeed change, yet Manifold continues to adapt and improve along the way.

#### Q: Is Manifold free?
Yes, the Manifold project is [open source](https://github.com/manifold-systems/manifold) and publicly available on
github, free for use via Apache License 2.0.

#### Q: Does Manifold work with Maven?  Gradle?
Yes.  Please refer to the [Setup](http://manifold.systems/docs.html#setup) instructions. 

#### Q: Does Manifold provide IDE support?
Yes.  [IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold) provides comprehensive support for Manifold.
Download / Update the plugin directly from within IntelliJ:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold` 
  
>Note: IntelliJ notifies you within 24 hours when an update is available and gives you the opportunity to sync.

#### Q: Do I really need a license for the IntelliJ plugin?
The [IntelliJ plugin](https://plugins.jetbrains.com/plugin/10057-manifold/) is offered separately via the JetBrains
Marketplace. It is *free* for use with IntelliJ IDEA *Community Edition* and is licensed for use with IntelliJ IDEA
*Ultimate Edition* after a 30 day free trial. The plugin remains free for students and faculty using Ultimate. Other
discounts apply and are available from the JetBrains Marketplace.

> *Update:* The Manifold plugin is free for use with IDEA Community Edition version 2020.1 and later. Due to limitations
> with JetBrains Marketplace licensing, earlier CE versions require a license after the 30 day trial. If you are a student
> or faculty member, you may qualify for a discount, please contact [JetBrains sales](https://www.jetbrains.com/support/sales/#department=american&tab=email-sales)
> for more information. Student discounts and other JetBrains offers apply and will be directly supported as the
> JetBrains Marketplace develops.

#### Q: How do I get manifold-*fill-in-blank* working with my project? 
Add the manifold-*fill-in-blank* dependency to your project along with the `-Xplugin:Manifold` javac argument, the setup
is sensitive to the version of Java you are using, generally whether you are using Java 8 or 9+. See the
[Setup](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings#setup) docs for
complete instructions.

#### Q: I defined some useful *extension methods*. How can I share them as a dependency?
The module with your extension methods must declare that it should be processed for extension methods. Do that with the
`Contains-Sources` manifest entry. For instance, from Maven:
```xml
  <manifestEntries>
      <!--class files as source must be available for extension method classes-->
      <Contains-Sources>java,class</Contains-Sources>
  </manifestEntries>
```
Please see the documentation for making [extension libraries](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#extension-libraries).

## Getting Help

#### Q: Where can I find help?
**Report A Bug**
The Manifold project uses github issues to track bugs, features, and other requests.  If you discover a bug, have a
feature request, or an idea go [here](https://github.com/manifold-systems/manifold/issues) and let it be known. Expect a
response within 24 hours.

**Private E-mail**
If your question or issue is more pressing or confidential, don't hesitate to send an email to [info@manifold.systems](mailto:info@manifold.systems).

**Discussions**
Join our [Slack Group](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa~a8qe0KDhOoD6Bgg) to start
a discussion, ask questions, provide feedback, etc. Someone is usually there to help.

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

Please [make some noise](https://join.slack.com/t/manifold-group/shared_invite/zt-e0bq8xtu-93ASQa) if you can't get it
working, chances are you're not alone and help will arrive soon.
