---
layout: default
---

# Roadmap

*A brain dump of thoughts that may contribute toward future Manifold releases*
 
# Long Term Goals

## Toward Data Science with Java and Manifold

Currently, Java lags far behind other languages as a data science language. One reason, perhaps the primary reason, the
data science community prefers other languages relates to Java's type system.  Java is a *statically* typed
language, which generally means types must be known as you *write* code -- the compiler and tooling verify your code is
type-safe.  While type-safety is an excellent trait, it comes with a significant downside if your goal is to fluently bind
with structured data.  For instance, if you want to access JSON or GraphQL data files with Java, you either use a string-based
static library and forgo type-safety or you introduce a code generator into your build process and live with
[the consequences](https://jaxenter.com/manifold-code-generators-150738.html).

What makes dynamic typing so attractive to data science programmers boils down to one word: <b>Metaprogramming</b>. This
is the magic that lets a Ruby or Python programmer write code to access data files using concise, easy to read syntax. With
these languages, because types aren't checked as code is written (there's no compiler), metaprogramming can play fast
and loose with structured data because it hooks up all the types *later* at runtime. Ah, but **there's the catch.** 
Not knowing types as you write code can make coding more difficult, indeed much more difficult with metaprogramming
because there is no type information to help you (and your IDE) discover what all that great black magic can do for you 
 -- *"you just have to know"*. As a consequence metaprogramming can be difficult to learn and use and often leads to
 trial-and-error style coding. Nonetheless it is a trade-off many data science programmers are willing to make.

But is this truly an either/or proposition with type-safety and metaprogramming?  What if metaprogramming could happen
at _compile-time_?  What if say Java's type system were *open* in such a way where types corresponding with structured
data could dynamically materialize and directly resolve through compiler plugins?  This is Manifold's core function --
it seamlessly opens Java's compiler to structured data plugins that participate as first-class type suppliers called
_Type Manifolds_.  Essentially a type manifold plugs into the Java compiler to enable it to resolve types from sources other
than .java files. A *just-in-time code generator*, if you will.  

Perhaps another reason dynamic languages such as Python are popular with data science is that it's relatively easy to
use. Again even with the type-blindness associated with metaprogramming, data science programmers overwhelmingly choose
it over type-safe static language alternatives because their experience with them is discouraging due to the type
system *getting in the way* and not solving problems fast enough.  But now with Manifold, suddenly the tables are turned
in terms of the metaprogramming advantage. Now Java's type-safety makes sense to a Python programmer struggling to
master the dark art of <fill in the blank> meteprogramming.  Since type manifolds are effectively built into Java's
compiler as well as IDE tooling (IntelliJ), they have deterministic code completion at their fingertips. They can discover
the methods and properties available to them immediately as they write code. They can use type-sensitive navigation to
jump between their code and the data science resources they are coding against. They can make changes to structured data
and immediately and type-safely see and use the effects of the changes. This type of functionality can make the
difference between amazing developer productivity and project failure. Suddenly Java is rings the bell as a well suited
language for data science.

In effect the _type manifold_ represents a breakthrough in programming language design as the convergence of dynamic
metaprogramming and compile-time type-safety. This is the bedrock on which Manifold builds its data science framework.

## Data Science Future

Over the past couple of years Manifold's has steadily produced one "seemingly impossible" feature after another to
extend the Java platform's reach. At a distance these features may appear unrelated and that's fine, each feature
can be used independently. But there is a method to the madness -- the features aim Java's reach toward a common goal. 

A recent feature has emerged that may hint at some semblance of order -- the Science manifold. It points in the general
direction Manifold is heading, which is to provide a Data Science framework for Java.  Now that most of the building
blocks are in place like type manifolds, extension methods, operator overloading, unit expressions, science libraries,
etc. the stage is set to begin building out higher level features. These include new type manifolds targeting popular
data science formats. So in addition to the existing JSON and GraphQL manifolds, more are on the way for CSV, XLS, MD5,
and others. Yet more type manifolds will target *languages* such as the R-language to provide seamless interop with
Java.  ...


## Data Science Formats

### Ready

* GraphQL
* JSON

### Planned

* XML
* CSV
* XLS
* FASTA

## Languages

### Ready

* Javascript

### Planned

* R Language
* Python

## Dimensions

## Unit Expressions

## Ranges

## Rational Number

## Statistical library integration

## Analytics library integration


## Support Scientific / Statistical Data Repositories (Experimental)
These type manifolds are URL-based.  One proof of concept is underway for DbPedia and SPARQL. 

## Support SQL and DDL (partially under way, experimental)

## Short Term Goals

### Features on the Workbench
**These are features currently being built or refined**


### Features in the Queue
**These are features that will be implemented in the near future**

<todo:>
