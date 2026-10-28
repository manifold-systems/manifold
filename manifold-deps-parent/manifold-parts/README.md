
# _Parts_

![latest](https://img.shields.io/badge/latest-v2026.1.16-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![DOI](https://img.shields.io/badge/DOI-10.5281%2Fzenodo.21514973-blue)](https://doi.org/10.5281/zenodo.21514973)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

*Parts* is high-performance late-bound runtime composition. It resolves a long-standing dilemma in object-oriented programming
by reconciling **independent runtime components** and **late-bound self-calls** while preserving arbitrary composition and
eliminating the hazards of inheritance.

* `@link` connects objects together at runtime, automatically delegating interface implementation
* `@part` classes reach overrides in linking objects, cleanly resolving the **_Self_ problem**
* Links are ordinary objects that may be **supplied at construction**: composition is fully runtime-configurable
* Self-dispatch at **vtable speed** enables composition as a *practical* alternative to inheritance/traits/mixins (see [Performance](#performance))

Use Parts in place of inheritance or alongside it.

A simple example:

```java
interface Actor {
  void takeAction();
  void attack();
}

@part class Hero implements Actor {
  public void takeAction() {
    attack();   // self-call is dynamically dispatched through the composite
  }
 
  public void attack() {println("Swing club!");}
}

class Wizard implements Actor {
  @link Actor actor;  // dynamically links an independent Actor object
  Wizard(Actor actor) {this.actor = actor;}
  public void attack() {println("Cast spell!");}
}

Actor hero = new Hero(); // any independent Actor can be supplied here (factory, DI, etc.)
Wizard wizard = new Wizard(hero);
wizard.takeAction(); // "Cast spell!"
```
`Hero.takeAction()` calls `attack()` on itself (a self-call). Because Hero is a linked part of the Wizard composite,
the self-call dispatches to `Wizard.attack()`, so the output is:
```
Cast spell!
```

Wizard depends only on the Actor contract, not on Hero's implementation, so Hero can be supplied as an independent component
at runtime rather than fused into Wizard's hierarchy as inheritance would require. Parts forwards `takeAction()`
to the linked Hero, and Hero's self-call to `attack()` still reaches Wizard’s override. This late-bound self-dispatch, known
as *open recursion*, is a capability normally associated with inheritance. Parts provides it across independent objects
linked at runtime, so they behave as a composite.

---

Other mainstream ways to reuse behavior give you one of these two properties, <br> 
*Parts gives you both:*

|                                      | Independent <br> components | Open <br> recursion |
|:-------------------------------------|:---------------------------:|:-------------------:|
| Trait/mixin composition (flattening) |              —              |          ✓          |
| Object composition (forwarding)      |              ✓              |          —          |
| **Parts**                            |            **✓**            |        **✓**        |

<sub>*Independent components*: composition consists of separate runtime objects, assembled and configured at construction.<br>
*Open recursion*: a component's self-calls reach overrides supplied by the composite.</sub>

### Wait...

> ***Isn't this traits?***

No. Traits gain open recursion only by keeping to the single-object model of *inheritance*. They are flattened into the
hosting class, so a trait has no independent runtime identity, and composition can't be configured at runtime. Open recursion,
but not independent components: traits can't supply Hero as a constructor argument as in the example above.

> ***Doesn't Kotlin do this?***

No. Kotlin's `by`, Scala's `export`, and Go's struct embedding are examples of ordinary *forwarding*. They provide independent
components, but not open recursion: forwarding produces the `"Swing club!"` result above.

---

<!-- TOC -->
* [_Parts_](#_parts_)
    * [Wait...](#wait)
* [The Self problem](#the-self-problem)
* [`@part`](#part)
* [`@link`](#link)
  * [](#)
* [Default methods](#default-methods)
* [Abstract parts](#abstract-parts)
* [Inheritance](#inheritance)
* [Self-preservation](#self-preservation)
* [Interface encapsulation](#interface-encapsulation)
    * [Usage on interface types (`implements` clause)](#usage-on-interface-types-implements-clause)
    * [Usage on methods](#usage-on-methods)
    * [Additionally:](#additionally)
* [Diamonds](#diamonds)
* [Performance](#performance)
* [IDE Support](#ide-support)
  * [Install](#install)
* [Setup](#setup)
  * [Building this project](#building-this-project)
  * [Using this project](#using-this-project)
  * [Binaries](#binaries)
  * [Gradle](#gradle)
  * [Maven](#maven)
* [Javadoc](#javadoc)
* [Paper](#paper)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)
<!-- TOC -->

---

# The Self problem

Consider the Hero example *without* `@part`:

```java
/*@part*/ class Hero implements Actor {
  public void takeAction() {
    attack(); // behaves statically without @part
  }
  
  public void attack() {println("Swing club!");}
}

class Wizard implements Actor {
  @link Actor actor;
  Wizard(Actor actor) {this.actor = actor;}
  public void attack() {println("Cast spell!");}
}

Wizard wizard = new Wizard(createActor());
wizard.takeAction();
```
The output is:
```text
Swing club!
```

Although `takeAction()` is invoked on `wizard`, it executes inside Hero. Without `@part`, the call to `attack()` dispatches
on the Hero instance, not on the composite. Wizard's override is never reached.

This is the fundamental limitation of ordinary object composition. Self-calls remain trapped within the delegated object.
This limitation is known as the *Self* problem.

---

# `@part`

Use `@part` to define a class designed to be composed with `@link`.

Generally, a link establishes a "part-of" relationship between the linking object and the linked `part`. Both objects form
a *composite* object in terms of the interfaces defined in the link. **A part's self-calls reach the composite.**

```java
interface Actor {
  void takeAction();
  void attack();
}

@part class Hero implements Actor {
  public void takeAction() { 
    attack(); // self-call reaches composite 
  }
  
  public void attack() {
    println("Swing club!");
  }
}

class Wizard implements Actor {
  @link Actor actor;
  
  Wizard(Actor actor) {this.actor = actor;}
  
  public void attack() {
    println("Cast spell!");
  }
}

Wizard wizard = new Wizard(createActor());
wizard.takeAction(); // "Cast spell!"
```
Output:
```
Cast spell!
```
Hero's `@part` annotation extends Java's dynamic dispatch across the Wizard composite, preserving open recursion.

---

# `@link`

Use `@link` to implement one or more interfaces through a field.

A link must be typed as an interface implemented by the declaring class. This interface defines the link.
```java
class Wizard implements Actor {
  @link Actor actor; // links Actor implementation to actor

  Wizard(Actor actor) {this.actor = actor;}

  @Override 
  public void attack() {
    println("Cast spell!");
  }
  
  // unimplemented Actor methods automatically forward to actor
}
```
Links are `private` and `final` by default.

Unimplemented interface calls forward through the link to the value of the field. If the field's value is a `@part` class,
open recursion is preserved.
 
---

# Default methods

`@part` classes preserve open recursion even when behavior is defined in interface default methods.

Suppose the `takeAction()` implementation is moved from Hero into the Actor interface:
```java
interface Actor {
  default void takeAction() {
    attack(); // with @part, self-calls reach composite
  }

  void attack();
}
```
The behavior is unchanged. Calling `wizard.takeAction()` still produces:
```text
Cast spell!
```

Although `takeAction()` executes from the interface, its call to `attack()` dispatches to Wizard exactly as it would if
`takeAction()` were implemented in a part. Within the default method, `this` refers to the composite's Actor identity:
the composite that claims Actor.

---

# Abstract parts

An `abstract` `@part` class is constructed using its `asLink()` static method, a compile-time generated static method that
matches the signatures of the part's constructors.

```java
interface Actor {
  void takeAction();
  void attack();
}

@part abstract class AbstractHero implements Actor {
  public void takeAction() {
    attack();  
  }
  // attack() is not implemented
}

class Wizard implements Actor {
  @link Actor base = AbstractHero.asLink(); // use abstract parts via asLink()

  public void attack() {println("Cast spell!");} // must implement Hero's abstract methods
}
```

As with extending abstract classes with inheritance, Wizard must implement abstract methods in AbstractHero or declare itself
`abstract`.

---

# Inheritance

`@part` classes support implementation inheritance. To maintain open recursion within a linked part, the superclass
chain must consist of `@part` classes.
```java
interface A {
  String a(String a);
  String b(String b);
}

@part class AImpl implements A {
  public String a(String a) {return a + b(a);}
  public String b(String b) {return b;}
}

@part class SubAImpl extends AImpl {
  . . .
}

class MyA implements A {
  @link A a = new SubAImpl();

  public String b(String b) {return "y_z";}
}

MyA myA = new MyA();
println(myA.a( "x_" )); 
```
Output:
```text
    x_y_z
```

---

# Self-preservation

Composition integrity rests on component identity: if a part exposes itself as its concrete type, a reference to that type
can bypass the composite and compromise integrity. To prevent this, the compiler enforces that a part may use `this` only
in the context of it implemented interfaces.

The compiler error is: `'this' in a part class must be used as an interface here`.

```java
@part
class MyPart implements MyInterface {
  @override
  public void interfaceMethod() {
    MyPart y = this; // error
    MyInterface x = this; // ok
    myMethod(this); // error
    System.out.println((MyInterface)this); // ok
  }

  private MyPart myMethod(MyPart a) {
    return this; // error
  }

  private MyInterface otherMethod(MyPart a) {
    return this; // ok
  }
}
```

The same dispatch mechanism that makes self-calls reach the composite also resolves `this` references when the interface
in context is claimed by a composite, eliminating the *Self* problem.

---

# Interface encapsulation

`@internal` marks an interface in an implements clause or an interface method as internal to a composition graph. `@internal`
is the `protected` modifier analog for the world of composition. It provides the same encapsulation benefits as `protected`
but without the legacy "leakiness" of `package-private` access.

### Usage on interface types (`implements` clause)

When applied to an interface in a class's `implements` clause, it prevents that interface from being delegated to by a composite.
The interface becomes an internal capability within the class, often for implementation details.
```java
class FooPart implements Foo, @internal Bar { ... }

class MyRoot implements Foo, Bar {
  // ERROR: Bar is internal to FooPart. It cannot be delegated to MyRoot.
  @link Bar bar = new FooPart();

  // OK: Foo is not internal to FooPart.
  @link Foo foo = new FooPart();
}
```
Note, here `@internal` applies to the *delegation surface* of FooPart (the interfaces that may be linked), not to the methods
of Bar: Bar methods are still *accessible* through FooPart.

### Usage on methods

When applied to an interface method, `@internal` restricts access to the compositional scope.

```java
interface Protocol {
  String result();
  @internal String step();
}

abstract @part class ProtocolPart implements Protocol {
  public String result() { return step() + "-done"; }
}

class ProtocolRoot implements Protocol {
  @link Protocol proto = ProtocolPart.asLink();

  public String step() { return "step"; }
}

class MyTestClass {
  public void testLinkFieldAccessToInternal() {
    ProtocolRoot root = new ProtocolRoot();
    root.result();
    root.step(); // compile error: step() is internal
  }
}
```
The method is part of the internal contract shared between a composite and its links, but is hidden from external consumers.
It is visible only to:
- *The Interface*: Default methods within the same interface.
- *Implementors*: Any class that implements the interface can override or call the method. Calls are limited to "self" calls:
  calls that dereference this or the `@link` field that provides the interface implementation.

### Additionally:

- Inherited: Implementors automatically inherit `@internal` status for overridden methods; it does not need to be reapplied.
- Compiler enforced: Parts produces compile errors for `@internal` access violations at compile-time.
- Runtime enforced: When supplied at runtime, a component's `@internal` access violations are reported as runtime exceptions.

---

# Diamonds

When super interfaces overlap, a "diamond" relationship results. This is known as _the Diamond problem_.
```text
         Person
           ▲▲
          ╱  ╲
   Student    Teacher
         ▲    ▲
          ╲  ╱
           TA
```
Should TA use Student's Person or Teacher's? Use `@link(share=Person.class)` to resolve the ambiguity.

```java
interface Person  {
  String name();
  String title();
  String titledName();
}
interface Teacher extends Person {...}
interface Student extends Person {...}
interface TA extends Student, Teacher {...}

@part class PersonPart implements Person {
  private final String name;
  public PersonPart(String name) {this.name = name;}
  public String name() {return name;}
  public String title() {return "Person";}
  public String titledName() {
    return title() + " " + name();
  }
}

static @part class TeacherPart implements Teacher {
  @link Person person;
  public TeacherPart(Person person) {this.person = person;}
  public String title() {return "Teacher";}
}

static @part class StudentPart implements Student {
  @link Person person;
  public StudentPart(Person person) {this.person = person;}
  public String title() {return "Student";}
}

static @part class TaPart implements TA {
  @link(share = Person.class) Student student;
  @link Teacher teacher;

  public TaPart(Student student) {
    this.student = student;
    this.teacher = new TeacherPart(student);
  }

  public String title() {return "TA";}
}

Person person = new PersonPart("Milton");
Student student = new StudentPart(person);
TA ta = new TaPart(student);
println(ta.titledName());
```
Output:
```text
    TA Milton
```
The TA's Student and Teacher roles share the same underlying Person identity: `@link(share=Person.class)` explicitly declares
that the overlapping Person interface is supplied by the shared Student link. Without `share=Person.class` a compiler error
indicates the overlap with Person.

>Note, `@part` classes are _not_ required with `@link(share=...)`; `share` applies equally to ordinary object composition.

---

# Performance

The flexibility of arbitrary composition is a product of *partial delegation*: when a composite links only a subset of a component's
delegation surface, leaving portions of the surface unclaimed and/or claimed by two or more composites. This presents a
challenge: How does the component resolve `this` as a receiver of a late-bound call?

The conventional assumption has been that it requires additional dispatch machinery that either sacrifices performance,
compromises arbitrary compositions, or some of both.

Parts avoids that cost through [interface-scoped dispatch](https://doi.org/10.5281/zenodo.21514973). The interfaces a component implements are its delegation
surface (minus `@internal` marked interfaces). A composite can link to that surface selectively, by interface. Each link
directly wires the composite to its component, allowing a component's self-calls on the link's interface to resolve directly
to the composite's implementation. The resulting dispatch is O(1): performance equivalent to a conventional virtual call
(see [5.1 Dispatch Performance](https://doi.org/10.5281/zenodo.21514973)).

---

# IDE Support

*Parts* is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

---

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%"/></p>

---

# Setup

## Building this project

The `manifold-parts` project is defined with Maven.  To build it install Maven and a Java 8 JDK and run the following
command.
```
mvn compile
```

## Using this project

The `manifold-parts` dependency works with all build tooling, including Maven and Gradle. It fully supports Java
versions 8 - 27.

This project consists of two modules:
* `manifold-parts`
* `manifold-parts-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-parts-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-parts` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired JDK
LTS release (8 - 27) or latest JDK release, the script takes care of the rest.
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

ext {
    manifoldVersion = '2026.1.16'
}

dependencies {
    implementation "systems.manifold:manifold-parts-rt:${manifoldVersion}"
    // Add manifold to -processorpath for javac
    annotationProcessor "systems.manifold:manifold-parts:${manifoldVersion}"
}

if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
        sourceSets.main.allJava.files.any { it.name == "module-info.java" }) {

    // IF YOU DO DEFINE A MODULE-INFO.JAVA FILE:
    tasks.withType(JavaCompile).configureEach { task ->
        options.compilerArgs += ['-Xplugin:Manifold', '--module-path', task.classpath.asPath]
        options.fork = true
        //!! Add these ONLY for Java 26+
        options.forkOptions.jvmArgs += [
                '--add-exports=java.base/jdk.internal.access=manifold.util',
                '--add-opens=jdk.compiler/com.sun.tools.javac.comp=manifold.util'
        ]
    }
} else {
    // IF YOU DO NOT DEFINE A MODULE-INFO.JAVA FILE:
    tasks.withType(JavaCompile).configureEach {
        options.compilerArgs += ['-Xplugin:Manifold']
        options.fork = true
        //!! Add these ONLY for Java 26+
        options.forkOptions.jvmArgs += [
                '--add-exports=java.base/jdk.internal.access=ALL-UNNAMED',
                '--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED'
        ]
    }
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyProject'
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2026.1.16</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-parts-rt</artifactId>
            <version>${manifold.version}</version>
            <maven.compiler.source>25</maven.compiler.source>
            <maven.compiler.target>25</maven.compiler.target>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.16.0</version>
                <configuration>
                    <!-- Add these ONLY for older JDKs -->
<!--                    <source>17</source>-->
<!--                    <target>17</target>-->
                    <encoding>UTF-8</encoding>
                    <fork>true</fork>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>

                        <!-- Add these ONLY for Java 26+ -->
<!--                        <arg>-J&#45;&#45;add-exports=java.base/jdk.internal.access=ALL-UNNAMED</arg>-->
<!--                        <arg>-J&#45;&#45;add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>-->
                        <!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-parts</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

# Javadoc

`manifold-parts`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts/2026.1.16/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts/2026.1.16)

`manifold-parts-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts-rt/2026.1.16/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts-rt/2026.1.16)

---

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

---

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

---

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
