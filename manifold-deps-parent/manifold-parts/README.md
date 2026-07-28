> **⚠ Experimental**

# Parts

![latest](https://img.shields.io/badge/latest-v2026.1.8-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![DOI](https://img.shields.io/badge/DOI-10.5281%2Fzenodo.21514973-blue)](https://doi.org/10.5281/zenodo.21514973)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)


*Parts* lets you assemble a class from independent, dynamically configured objects.

It offers both: ***the flexibility of runtime composition and the polymorphism of inheritance***, to fill a longstanding
gap in object-oriented programming. Use it in place of inheritance, or alongside it.

- `@link` implements an interface through a field, forwarding the calls automatically
- `@part` turns that link into *true* delegation: your overrides apply *everywhere*, even inside the part (solves the [Self problem](https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html))
- Each part is a plain object supplied at construction, so composition is configured at runtime (language-level DI)
- `@link(share=...)` safely shares an interface across parts (solves the [Diamond problem](https://en.wikipedia.org/wiki/Multiple_inheritance#The_diamond_problem))
- All at Java's normal dynamic-dispatch speed (see [Interface-Scoped Dispatch](https://doi.org/10.5281/zenodo.21514973))

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
With ordinary composition, `BaseHero`'s own `attack()` implementation ("Swing club!") would run instead. This internal
polymorphism across a runtime-injected part is the fundamental capability that Parts adds. 

<!-- TOC -->
* [Parts](#parts)
* [Basic usage](#basic-usage)
* [`@link`](#link)
* [`@part`](#part)
* [Forwarding](#forwarding)
    * [A one-way flight](#a-one-way-flight)
* [Delegation](#delegation)
  * [Self-preservation](#self-preservation)
  * [Interface encapsulation](#interface-encapsulation)
    * [Usage on interface types (`implements` clause)](#usage-on-interface-types-implements-clause)
    * [Usage on methods](#usage-on-methods)
    * [Additionally:](#additionally)
  * [Abstract parts](#abstract-parts)
  * [Inheritance](#inheritance)
  * [Default methods](#default-methods)
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

# Basic usage

# `@link`
Use `@link` to implement one or more interfaces through a field. 
```java
class MyClass implements MyInterface {
  @link MyInterface myInterface; // transfers calls on MyInterface to myInterface

  public MyClass(MyInterface myInterface) {
    this.myInterface = myInterface; // dynamically configure behavior
  }
  
  // No need to implement MyInterface here, but you can override myInterface as needed
}
```
The interfaces used in a link are the intersection of the type[s] specified in the linked field and the interfaces of the
enclosing class.

```java
interface A {. . .}
interface B {. . .}
public class Sample implements A, B {. . .}
```
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
  @link Sample foo; // links A and B to foo
```
If interfaces are specified in `@link`, the intersection of those interfaces and the interfaces of the enclosing class define
the link.
```java
  @link(A.class) Sample foo; // links A to foo
```
Note, `@link` fields are `private` and `final` by default.

Unimplemented interface calls transfer through the link to the assigned value of the field. The value's type determines
how the calls are transferred. If the type is annotated with [`@part`](#part), calls are transferred using [delegation](#delegation).
Otherwise, they are transferred using call [forwarding](#forwarding).
 
# `@part`
Use `@part` to enable *true* delegation with `@link`.

Generally, a link establishes a "part-of" relationship between the linking object and the linked `part`. Both objects form
a single, composite object in terms of the interfaces defined in the link. 

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

# Forwarding
Forwarding (sometimes confused with *delegation* in the OOP world) handles a class's unimplemented interface calls by transferring
(forwarding) the calls to another object, often one that fully implements the interface.

With `@link` this process is handled automatically.

A simple example demonstrating interface composition via forwarding with a map.
```java
public class StringMap<E> implements Map<String, E> {
  @link Map<String, E> map = new HashMap<>();

  public boolean equals(Object o) {return map.equals(o);}
  public int hashCode() {return map.hashCode();}
}
``` 
The advantage over implementation inheritance is that the implementation of StringMap is decoupled from HashMap: only the
Map interface is exposed through StringMap; HashMap is an encapsulated implementation detail, which avoids the fragile base
class problem when subclassing with inheritance. `@link` performs the grunt work of forwarding unimplemented Map calls.
                                                                                                    
### A one-way flight

With forwarding the object receiving the forwarded calls knows nothing about the forwarding object. Using the Hero example: 
```java
@part class BaseHero implements Hero {
  public void takeAction() {
    attack();
  }

  public void attack() {
    out.println("Swing club!");
  }
}

class Wizard implements Hero {
  @link Hero base;

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
Swing club!
```

Without the `@part` annotation BaseHero is not wired to the linking object, Wizard. Forwarded calls are *one-way flights*.
The call to `attack()` from `takeAction()` is dispatched _statically_ -- *Wizard's override is ignored*.

Generally, linked interface calls within forwarded objects lose the *internal* polymorphism of inheritance. This behavior
is often referred to as _the Self problem_.


# Delegation

If HeroBase is annotated with `@part`, Hero methods are called using _delegation_.

Delegation is more rigorous. It enables polymorphic calls from linked parts where Wizard can override Hero methods
so that the implementation of Hero defers to Wizard.
```java
@part class BaseHero implements Hero {
  . . .
}
```
With `@part` the call to `wizard.takeAction()` results in:
```text
    Cast spell!
```
Inside BaseHero `this` refers to Wizard in terms of the Hero interface. Thus, the call to `attack()` dispatches
_dynamically_. This "true" form of delegation solves _the Self problem_.

## Self-preservation

Delegation involves composite objects each consisting of a root object and its graph of linked `part` objects. Within a
composite object, linked interface calls are initially dispatched from the root object, never from linked parts; `this`
always refers to the root in terms of the interfaces defined by the links. Otherwise, if any of the linked parts are allowed
to directly refer to a non-root part, delegation is broken.

Essentially, polymorphic calls are compromised when a direct reference to a part bypasses the root. Therefore, `part` classes
are not permitted to reference `this` in a context other than a declared interface.

Invalid `this` usages in `part` classes result in compile error: `Part class 'this' must be used as an interface here`. 
```java
@part class MyPart implements MyInterface {
    @override public void interfaceMethod() {
      privateMethod(this); // compile error
      privateMethod(new MyPart()); // ok
      MyPart w = this; // compile error
      MyInterface x = this; // ok
      Object y = (Object)this; // compile error
      Object z = (MyInterface)this; // ok
    }
    
    private MyPart privateMethod(MyPart a) {
        return this; // compile error
    }

    private MyInterface otherMethod(MyPart a) {
        return this; // ok
    }
}
```
Note, `@part` classes are not confined to usage as linked objects. They can be used anywhere for any purpose. 

## Interface encapsulation

Use the `@internal` annotation

The `@internal` interface marks an interface in an implements clause or an interface method as internal to a composition graph.
`@internal` is the protected modifier for the world of composition. It provides the same encapsulation benefits as protected
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

public void testLinkFieldAccessToInternal() {
  ProtocolRoot root = new ProtocolRoot();
  root.result();
  root.step(); // compile error: step() is internal
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

## Abstract parts

To use an abstract `@part` class, it must be constructed an `asLink()` method. These are static methods match the signatures
of the part's constructors.

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
  @link BaseHero base = BaseHero.asLink(); // <--- requires override for attack()

  public void attack() {out.println("Cast spell!");}
}
```
 
## Inheritance

`@part` classes support implementation inheritance. But to maintain polymorphic calls within linked parts, superclasses
associated with links must also be `part` classes.
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

  public String b(String b) {"y_z";}
}

MyA a = new MyA();
out.println(a.a( "x_" )); 
```
Output:
```text
    x_y_z
```

## Default methods

Consider `takeAction()` as a default method in Hero instead of an implementation in BaseHero.
```java
interface Hero {
  default void takeAction() { attack(); }
  void attack();
}
```  
Calls must behave identically regardless of where the method is implemented; polymorphism must be preserved when using `part`
classes: the call to `wizard.takeAction()` dispatches dynamically as before:
```text
    Cast spell!
```    
Inside the Hero interface `this` refers to Wizard even when called from BaseHero.
 

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

# IDE Support

Delegation with links & parts is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%"/></p>

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

# Javadoc

`manifold-parts`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts/2026.1.8/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts/2026.1.8)

`manifold-parts-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-parts-rt/2026.1.8/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-parts-rt/2026.1.8)

# Paper

The dispatch model, its correctness argument, and benchmarks are written up here:
[*Interface-Scoped Dispatch: Independent Components with O(1) Self-Calls*](https://doi.org/10.5281/zenodo.21514973)

To cite Parts, use that DOI.

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
