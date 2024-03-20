
# Java's curious disdain for properties

I recently read a [post](https://www.reddit.com/r/java/comments/1b5bolp/it_is_time_we_get_a_language_support_for_getters/) pleading for formal language support for state abstraction in the form of properties as a respectable albeit beyond
overdue solution to the dogged getter/setter travesty. As expected the overriding response was "Just use records" as is
the case lately whenever the topic surfaces. But this makes little sense given the full scope of Java's state abstraction
deficiencies. Records are inadequate in this regard and don't compare at all with the comprehensive solution that properties
provides. Let's set the record straight.

--- 

A property is an _**abstraction**_ of _**state**_. It encapsulates a single element of state that is:
- optionally mutable and
- discretionarily accessible

Internally, the state a property manages may be backed in a multitude of ways: 
- a field
- a data source
- an aggregation
- etc.

A property may also be declared as:
- lazy initialized
- bounded/observable
- delegated
- etc.

It is optionally:
- virtual/abstract
- inheritable
- polymorphic

Additionally, properties are:
- l-values (accessed and assigned directly by name)
                                                             
Records simply can't satisfy most of these criteria, and no one should expect otherwise.

At a minimum properties _eliminate_ getter/setter boilerplate from view, wherever they are used, both at the declaration
site _and_ the _**use site**_.
```java
public interface Book { 
  var String title; // abstract, mutable property
}

book.title = "War and Peace"; // mutable properties are abstract l-values
String name = book.title;     // conveniently accessible as identifiers 
book.title += " (revised)";   // and lean heavily on the compiler for robust features 
```
Additionally, properties may be _**inferred**_ from older source files and compiled dependencies, providing a bridge to
code written for earlier JDKs.

Of course this just barely touches on the capabilities of properties, the feature provides far more power than this. I
would encourage anyone not already familiar with properties in modern static languages, particularly C# and Kotlin, to
have a deep look.

---

A record is a _**concrete**_ terminal _**type**_. It offers a more concise declaration than a similarly featured
class with strict constraints on state. Essentially, records provide nice declaration-site shorthand for simple data
classes by generating boilerplate constructors, equals/hashcode/toString, and accessor methods. Additionally, the
immutable, value-based quality of records can be combined with other features of the language such as deconstruction and
pattern matching.

By definition records are not abstractions of state, they are immutable _containers_ of raw state directly exposed via
accessor methods, basically immutable data classes. Thus, properties and records are not an either/or proposition,
they solve separate problems. The two features can and should exist in harmony; records could be _implemented_ with properties,
as such records would be substantially more capable and interesting.

State itself needs help in the Java language. While records suffice in some areas, the feature is far from a comprehensive
remedy to Java's severe lack of state abstraction. Indeed, after using a language fitted with properties then going back
to Java, records or not, it feels like a stone-age tool.

---

Tragically, properties will probably continue to be ridiculed in mainstream Java discussions. The "how dare you!" responses have changed
over the years, but none make an ounce of sense in my judgment. It used to be "Properties is a 'rich' feature, Java is not
a 'rich' language..." What? This was back when Java wasn't adding features as quickly as they are today. Consequently, the
"not a rich language" claim can't be made any longer, whatever it means, because I guess there is a feature threshold that
designates a "rich" language? Anyhow, they appear to be leaning on records now as the primary excuse. Shrug.  

Although the tone might suggest it, the purpose of this post is not to disparage the Java language. On the contrary, it is more
to light a fire under it. I use Java daily, it's still my preferred JVM language, but just barely. Honestly,
my ideal Java would probably take it back to Java 8 and start over. Put Valhalla and Loom on the front burners and finish them,
make lambdas actual closures, what's the deal with that? Add properties, declaration-site generic variance, true delegation / traits, operator overloading, null-safe
operators, structural interfaces, extension methods, string templates, proper templates, and make the type-system pluggable in the compiler,
so we can stop writing code generators like it's 1979. But yeah, mainly properties.

> Disclaimer: I wrote a FOSS [compiler plugin](https://github.com/manifold-systems/manifold) for Java that provides properties
> and other desirable features to the language. And I have cats.


