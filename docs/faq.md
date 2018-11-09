---
layout: default
---

# Manifold F.A.Q.s - Frequently Asked Questions (and Answers)

[Common Questions](#common-questions) • [Getting Help](#getting-help) • [Troubleshooting](#troubleshooting) • [History / Trivia](#history--trivia)


## Common Questions

#### Q: Where can I find the latest Manifold publications?

Find articles and other publications on the [Articles](http://manifold.systems/articles/articles.html) page.

#### Q: Manifold blog?

The Manifold [blog](http://manifold.systems/blog.html) is accessible on the front page here.

#### Q: How do I get the latest Intellij plugin updates?

Install the Manifold plugin from the JetBrains repository available directly from via:
`Settings | Plugins | Manifold`.  Thereafter IntelliJ lets you know when an update is
available and gives you the opportunity to sync.

#### Q: Does Manifold support Java 11?

Yes.  Manifold fully supports Java 8 - 11.  Manifold supports all manner of Java's module system.

#### Q: Is Manifold free?

Yes, Manifold is an [open source](https://github.com/manifold-systems/manifold) project available on github freely 
available for use via Apache License 2.0.


  
## Getting Help

#### Q: Where can I find help?

We use github issues to communicate with Manifold users regarding questions, comments, and issues.  If you have 
something to say, don't be shy, please go [here](https://github.com/manifold-systems/manifold/issues), click the 
`New Issue` button and let us know what's up. Remember to tag your issue with the appropriate item (question, issue, 
etc.).  We'll respond within 24 hours (we live in the Pacific Time Zone).

If your question or issue is more pressing, please send an email to [info@manifold.systems](mailto:info@manifold.systems).
We'll respond ASAP.



## Troubleshooting

#### Q: I updated to the latest Manifold IntelliJ plugin and now IntelliJ is complaining with error messages.  What is wrong?

You probably need to update the manifold libraries your project uses to the latest manifold release.  The plugin
should tell you which version of manifold you should be using with a warning message.  You can find the latest releases
[here](https://github.com/manifold-systems/manifold/tags).

If you are using Maven or Gradle, you must update your build files -- do not change the Module dependencies from 
IntelliJ's UI.



## History / Trivia

#### Q: Why is the name "Manifold"?

In mathematical physics a manifold allows one to experience an otherwise foreign
geometry as a familiar Euclidean space.  Applying that general principal to Java's
type system, a manifold allows one to access structured data as if it were a normal 
Java class.