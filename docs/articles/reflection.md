# Java Access Control, Stop the Insanity!

Java's access modifiers `public`, `protected`, `package-private`, `private` are enforced *both* at compile-time and at
runtime. Here Scott McKinney explains why this is insane and how access to internals should be simpler and type-safe.

## Crazy Talk

Routinely developers use access modifiers as a means to separate API from implementation (aka encapsulation). The
modifiers provide a clean way to tell other developers, "Here's the stuff I intend for you to use, all the rest is
internal to my implementation."  In essence access modifiers serve to help developers identify and safely use an API.

Fortunately the Java compiler makes sure developers don't *unintentionally* cross the boundaries established by the
modifiers. There's no way we can call a private method illegally without the compiler complaining about it. So why does
the Java runtime also enforce access control when the compiler has already done the work? Is it a form of *security*?
Sadly this is a widely held misconception. Allow me to explain. 

The lock on your front door is a form of security, right?  The lock's purpose is to prevent an intruder from entering
your home.  One thing you wouldn't do is devise a way for intruders to evade your security. And you certainly would not
advertise this information for all to see, right?  For example, you wouldn't post a sign on your front lawn detailing
the spare bedroom window you leave wide open specifically for intruders, right?  Otherwise by definition the lock on
your door is not a form of security, but is instead an indication of insanity.  

The Java Reflection API is the sign posted on the JVM's front lawn. It explains precisely how to bypass Java's runtime
access control; you can call any method you like using reflection. Indeed Java developers commonly use reflection to 
*intentionally* access internal members and _**there is nothing wrong with that**_ if it's the best option at hand. But
Java reflection is inefficient and error prone, it escapes type-safety and often requires sophisticated caching to
improve performance.

But why bother with reflection here?  As I have already established there is no meaningful security in the JVM with
respect to access control; anyone can bypass it at any time. So what is accomplished by making it hard and slow and
dangerous with reflection?

## Wishful Thinking

Why not provide a simpler, type-safe syntax to access internals:
```java
unsafe Foo foo = new Foo();
foo.privateMethod();
```
The `unsafe` modifier informs the compiler of your *intention* to use internal members of `Foo` type-safely. As such
the compiler grants `foo` with open access to `Foo`.  The advantages of this approach are significant:
* Your code is **type-safe**, the compiler verifies access to internal members
* If the internals change, your code breaks at **compile-time**, not runtime
* Your code is much **easier to read** and maintain
* Your code is visible to **static analysis tooling** 
* **Eliminates caching** and other complications associated with reflection

All of this could be achieved with or without the cooperation of the JVM.  If access control were removed from runtime,
great! Aside from the new `unsafe` modifier, nothing else is needed and performance is optimal.  Otherwise the
compiler could generate reflection code for usages of `unsafe` variables, still a big win. 

## Back to Reality

Of course all of this is make believe, `unsafe` will never see the light of day. But [was it over when the Germans bombed Pearl Harbor?](https://www.youtube.com/watch?v=Wv5c2YR1lVE)
Heck no! Fortuitously a similar feature already exists from the [Manifold framework](http://manifold.systems)
aptly named [Type-safe Reflection](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-safe-reflection-via-jailbreak).
It is nearly identical to the `unsafe` proposal:
```java
@Jailbreak Foo foo = new Foo();
foo.privateMethod();
```       
With the `@Jailbreak` annotation `foo` has open access to `Foo`, all fully type-safe and compiler friendly. Read more
about Type-safe Reflection and other features at [Manifold](http://manifold.systems).  
 

 