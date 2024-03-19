
# Java's lack of state abstraction

I recently read a [post](https://www.reddit.com/r/java/comments/1b5bolp/it_is_time_we_get_a_language_support_for_getters/) pleading for formal language support for state abstraction in the form of properties as a respectable albeit beyond
overdue solution to the dogged getter/setter travesty. As expected the overriding response was "Just use records!" as is
the case lately whenever the topic surfaces. But a complete solution to Java's state abstraction deficiencies is much broader
and far more powerful than what records brings to the table. Indeed, as a state abstraction feature records barely scratch
the surface. 

A record is a _**concrete**_ terminal _**type**_. It offers a more concise declaration than a similarly featured
class with strict constraints on state. Essentially, records provide nice declaration-site shorthand for simple data
classes by generating boilerplate constructors, equals/hashcode/toString, and accessor methods. Additionally, the
immutable, value-based quality of records can be combined with other features of the language such as deconstruction, pattern
matching, and more. 

A property is an _**abstraction**_ of _**state**_. It encapsulates a single element of state that is optionally mutable and
discretionarily accessible. The state it manages may be backed in a multitude of ways: a field, a data source, an aggregation,
etc. It may also be declared to be lazy initialized, bounded/observable, delegated, and so on. A property is also optionally
inheritable and elegantly polymorphic.

By definition records are not abstractions of state, they are immutable _containers_ of raw state directly exposed via
accessor methods, basically immutable data classes. Thus, properties and records are not an either/or proposition,
they solve separate problems. The two features can and should exist in harmony; records could be _implemented_ using properties,
as such records would be substantially more capable and interesting.

---

---

At a minimum properties _eliminate_ getter/setter boilerplate from view, wherever they are used, both at the declaration
site _and_ the use site.
```java
public interface Book { 
  var String title; // abstract, mutable property
}
```
Refer to it directly by name:
```java
book.title = "War and Peace"; // mutable properties are abstract l-values
String name = book.title;     // conveniently accessible as identifiers 
book.title += " (revised)";   // and lean heavily on the compiler 
```
Additionally, properties may be _**inferred**_ from older source files and compiled dependencies, providing a bridge to
code written for earlier JDKs.

Of course this just barely touches on the capabilities of properties, the feature provides far more power than this. I
would encourage anyone not already familiar with properties in modern static languages, particularly C# and Kotlin, to
have a deep look.

                                                          
---

---
   

State itself needs help in the Java language. While records suffice in some areas, the feature is far from a comprehensive
answer to Java's severe lack of state abstraction features. Having heavily used and developed languages employing properties,
it's obvious. They become nearly indispensable once you've used them regularly then go back to a language without the feature.
Java feels like a stone-age language in this respect.

Tragically, properties will probably continue to be a taboo subject in mainstream Java social media circles, which have become echo chambers
blindly parroting the Java language designers' narrative. The excuses have changed over the years, but none make an ounce of sense
in my judgment. It used to be "Properties is a 'rich' feature, Java is not a 'rich' language..." What? This was back when
Java wasn't adding features as quickly as they are today. Consequently, the "not a rich language" claim can't be made any
longer, whatever it means, because I guess there is a feature threshold that designates a "rich" language? Anyhow, they
appear to be leaning on records now as the primary excuse. Shrug.  

Although it may read like one, this is not a hit piece on the Java language. I use Java daily, it's still my preferred
JVM language, but just barely. Honestly, my ideal Java would probably take it back to Java 8, or 17 and ditch modules, kill plans
to remove Unsafe and give me back unmitigated reflection... and give lambdas full closure, what's the deal with that? Add
properties, true delegation/traits, operator overloading, null-safe operators, structural interfaces, and make the type-system
pluggable in the compiler so we can stop writing code generators like it's 1979. But yeah, mainly properties.

> Disclaimer: I wrote a FOSS compiler plugin for Java providing properties to the language. And I have two cats.


