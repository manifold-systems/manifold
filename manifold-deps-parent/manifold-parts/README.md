> **⚠ Experimental**

# _Parts_

![latest](https://img.shields.io/badge/latest-v2026.1.8-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![DOI](https://img.shields.io/badge/DOI-10.5281%2Fzenodo.21514973-blue)](https://doi.org/10.5281/zenodo.21514973)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)


OOP offers two fundamental ways to reuse implementation.

* **Implementation inheritance** naturally supports polymorphism. A call from one inherited method to another behaves exactly
as expected because every method executes as part of the same object. The tradeoff: the implementation structure is fixed
at compile time, and inherited behavior is merged into a single runtime object.
* **Composition** solves the flexibility problem. Independent objects can be assembled dynamically, implementations can
be injected, and behavior can vary at runtime. But composition loses one important property of inheritance: internal polymorphism.
Once execution enters a composed object, self-calls remain inside that object.

Combining these two features in arbitrary compositions has remained an open problem in OO models for decades.

*Parts* delivers both: ***the flexibility of runtime composition and the polymorphism of inheritance***. It lets you assemble
classes from independent, runtime-configured objects. Use it in place of inheritance, or alongside it.

* `@part` provides **composite-aware dispatch**: self-calls reach overrides in the composite
* Each part is a plain object **supplied at construction**, so composition is configured at runtime
* `@link` implements an interface through a field, providing automatic forwarding
* Parts delivers compositional dynamic dispatch at **vtable speed** (see [Interface-Scoped Dispatch](https://doi.org/10.5281/zenodo.21514973))


```java
interface Hero {
  void takeAction();
  void attack();
}

@part class BaseHero implements Hero {
  public void takeAction() {
    attack();          // <--- self-call: ordinary composition can't do this
  }

  public void attack() {out.println("Swing club!");}
}

class Wizard implements Hero {
  @link Hero base;     // <--- @link delegates Hero impl to `base` part
  Wizard(Hero base) {this.base = base;}
  public void attack() {out.println("Cast spell!");}
}

Wizard wiz = new Wizard(new BaseHero()); // <--- supplied at runtime, inheritance can't do this
wiz.takeAction();
```
`BaseHero.takeAction()` calls `attack()` on itself (a self-call). Because `BaseHero` is a linked part of the `Wizard` composite,
the self-call dispatches to `Wizard.attack()`, so the output is:
```
Cast spell!
```
With ordinary composition, `BaseHero`'s own `attack()` implementation `Swing club!` would result instead. This internal
polymorphism across a runtime-injected part is the fundamental capability that Parts adds. 

By supplying BaseHero as a runtime component instead of fusing it into Wizard's hierarchy, Wizard depends only on the Hero
contract, not on BaseHero's implementation structure. Parts delegates the interface implementation while preserving the
polymorphic behavior normally associated with inheritance.

---

<!-- TOC -->
* [_Parts_](#_parts_)
* [`@part`](#part)
* [`@link`](#link)
* [The Self problem](#the-self-problem)
* [Abstract parts](#abstract-parts)
* [Inheritance](#inheritance)
* [Default methods](#default-methods)
* [Interface encapsulation](#interface-encapsulation)
    * [Usage on interface types (`implements` clause)](#usage-on-interface-types-implements-clause)
    * [Usage on methods](#usage-on-methods)
    * [Additionally:](#additionally)
* [Diamonds](#diamonds)
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

# `@part`

Use `@part` to define a class designed to be composed with `@link`.

Generally, a link establishes a "part-of" relationship between the linking object and the linked `part`. Both objects form
a *composite* object in terms of the interfaces defined in the link.

```java
interface Hero {
  void takeAction();
  void attack();
}

@part class BaseHero implements Hero {
  public void takeAction() {
    attack();
  }

  public void attack() {
    out.println("Swing club!");
  }
}

class Wizard implements Hero {
  @link Hero base = new BaseHero();
  
  Wizard(Hero base) {this.base = base;}
  
  public void attack() {
    out.println("Cast spell!");
  }
}

Wizard wizard = new Wizard(new BaseHero());
wizard.takeAction();
```
Output:
```
Cast spell!
```
BaseHero's `@part` annotation extends Java's dynamic dispatch across the Wizard composite, preserving polymorphic self-calls.

---

# `@link`

Use `@link` to implement one or more interfaces through a field.

The interfaces used in a link are the intersection of the type of the linked field and the interfaces of the enclosing class.

If the field's type is an interface, the intersection of that interface and the interfaces of the enclosing class define
the link.
```java
public class MyClass implements A, B {
  @link A foo; // links A to foo
  . . .
}
```
If the field's type is a class, the intersection of the interfaces of the class and the interfaces of the enclosing class
define the link.
```java
public class Sample implements A, B {. . .}

@link Sample foo; // links A and B to foo
```
If interfaces are specified in `@link`, the intersection of those interfaces and the interfaces of the enclosing class define
the link.
```java
  @link(A.class) Sample foo; // links just A to foo
```
Note, `@link` fields are `private` and `final` by default.

Unimplemented interface calls forward through the link to the value of the field. If the field's value is a `@part` class,
internal polymorphism is preserved.
 
---

# The Self problem

Consider the `Hero` example without `@part`:

```java
/*@part*/ class BaseHero implements Hero {
  public void takeAction() {
    attack();
  }

  public void attack() {
    out.println("Swing club!");
  }
}

class Wizard implements Hero {
  @link Hero base;

  Wizard(Hero base) { this.base = base; }

  public void attack() {
    out.println("Cast spell!");
  }
}

Wizard wizard = new Wizard(new BaseHero());
wizard.takeAction();
```
The output is:
```text
Swing club!
```

Although `takeAction()` is invoked on `wizard`, it executes inside BaseHero. The call to `attack()` therefore dispatches
on the BaseHero instance, not on the composite. Wizard's override is never reached.

This is the fundamental limitation of ordinary composition. It preserves external polymorphism, but internal self-calls remain
trapped within the delegated object. This limitation is known as the **Self problem**.

---

# Abstract parts

To use an abstract `@part` class, it must be constructed using an `asLink()` static method. These are compile-time generated
static methods that match the signatures of the part's constructors.

```java
interface Hero {
  void takeAction();
  void attack();
}

@part abstract class BaseHero implements Hero {
  public void takeAction() {
    attack();  
  }
  // attack() is abstract
}

class Wizard implements Hero {
  @link BaseHero base = BaseHero.asLink(); // <--- use abstract parts via asLink()

  public void attack() {out.println("Cast spell!");} // <--- must implement attack()
}
```
---

# Inheritance

`@part` classes support implementation inheritance. To maintain internal polymorphism within a linked part, its superclass
must also be a `@part` class.
```java
interface A {
  String a(String a);
  String b(String b);
}

@part class AImpl implements A {
  public String a(String a) {return a + b(a);}
  public String b(String b) {return b;}
}

@part class SubAImpl extends AImpl {}

class MyA implements A {
  @link A a = new SubAImpl();

  public String b(String b) {return "y_z";}
}

MyA myA = new MyA();
out.println(myA.a( "x_" )); 
```
Output:
```text
    x_y_z
```

---

# Default methods

`@part` classes preserve internal polymorphism even when behavior is defined in interface default methods.

Suppose `takeAction()` is moved from `BaseHero` into the `Hero` interface:
```java
interface Hero {
  default void takeAction() {
    attack();
  }

  void attack();
}
```
The behavior is unchanged. Calling `wizard.takeAction()` still produces:
```text
Cast spell!
```

Although `takeAction()` executes from the interface, its call to `attack()` dispatches to `Wizard` exactly as it would if
`takeAction()` were implemented in a part. Within the default method, `this` refers to the composite's `Hero` identity,
not an individual component.

---

# Interface encapsulation

The `@internal` interface marks an interface in an implements clause or an interface method as internal to a composition graph.
`@internal` is the `protected` modifier analog for the world of composition. It provides the same encapsulation benefits as `protected`
but without the legacy "leakiness" of `package-private` access.

### Usage on interface types (`implements` clause)

When applied to an interface in a delegate class's `implements` clause, it prevents that interface from being "inherited"
by delegators. The interface becomes a private capability of the delegate, often for internal implementation details.
```java
class FooPart implements Foo, @internal Bar { ... }

class MyRoot implements Foo, Bar {
  // ERROR: Bar is internal to FooPart. It cannot be delegated to MyRoot.
  @link Bar bar = new FooPart();

  // OK: Foo is not internal to FooPart.
  @link Foo foo = new FooPart();
}
```

### Usage on methods

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
When applied to an interface method, it restricts access to the compositional scope. The method is part of the internal
contract shared between a composite and its links, but is hidden from external consumers. It is visible only to:
- The Interface: Default methods within the same interface.
- Implementors: Any class that implements the interface can override or call the method. Calls are limited to "self" calls:
  calls that dereference this or the `@link` field that provides the interface implementation.

### Additionally:

- Inherited: Implementors automatically inherit `@internal` status for overridden methods; it does not need to be reapplied.
- Compiler Enforced: Parts produces compile errors for `@internal` access violations.

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
  String getName();
  String getTitle();
  String getTitledName();
}
interface Teacher extends Person {}
interface Student extends Person {}
interface TA extends Student, Teacher {}

@part class PersonPart implements Person {
  private final String name;
  public PersonPart(String name) {this.name = name;}
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {
    return getTitle() + " " + getName();
  }
}

static @part class TeacherPart implements Teacher {
  @link Person person;
  public TeacherPart(Person person) {this.person = person;}
  public String getTitle() {return "Teacher";}
}

static @part class StudentPart implements Student {
  @link Person person;
  public StudentPart(Person person) {this.person = person;}
  public String getTitle() {return "Student";}
}

static @part class TaPart implements TA {
  @link(share = Person.class) Student student;
  @link Teacher teacher;

  public TaPart(Student student) {
    this.student = student;
    this.teacher = new TeacherPart(student);
  }

  public String getTitle() {return "TA";}
}

Person person = new PersonPart("Milton");
Student student = new StudentPart(person);
TA ta = new TaPart(student);
out.println(ta.getTitledName());
```
Output:
```text
    TA Milton
```
The TA's Student and Teacher roles share the same underlying Person identity: `@link(share=Person.class)` explicitly declares
that the overlapping Person interface is supplied by the shared Student link. Without `share=Person.class` a compiler error
indicates the overlap with Person.

>Note, `@part` classes are _not_ required with `@link(share=...)`; `share` applies to forwarding as well.

---

# IDE Support

Delegation with links & parts is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

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
versions 8 - 26.

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
LTS release (8 - 26) or latest JDK release, the script takes care of the rest.
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

targetCompatibility = 17
sourceCompatibility = 17

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
     implementation 'systems.manifold:manifold-parts-rt:2026.1.8'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-parts:2026.1.8'
     testAnnotationProcessor 'systems.manifold:manifold-parts:2026.1.8'
}

if (JavaVersion.current() != JavaVersion.VERSION_1_8 &&
    sourceSets.main.allJava.files.any {it.name == "module-info.java"}) {
    tasks.withType(JavaCompile) {
        // if you DO define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold', '--module-path', it.classpath.asPath]
    }
} else {
    tasks.withType(JavaCompile) {
        // If you DO NOT define a module-info.java file:
        options.compilerArgs += ['-Xplugin:Manifold']
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
        <manifold.version>2026.1.8</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-parts-rt</artifactId>
            <version>${manifold.version}</version>
        </dependency>
    </dependencies>

    <!--Add the -Xplugin:Manifold argument for the javac compiler-->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
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
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts/2026.1.8/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts/2026.1.8)

`manifold-parts-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts-rt/2026.1.8/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts-rt/2026.1.8)

---

# Paper

The dispatch model, its correctness argument, and benchmarks are written up here:
[*Interface-Scoped Dispatch: Independent Components with O(1) Self-Calls*](https://doi.org/10.5281/zenodo.21514973)

To cite Parts, use that DOI.

---

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

---

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

---

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
