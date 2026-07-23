> **⚠ Experimental**

# Parts

![latest](https://img.shields.io/badge/latest-v2026.1.8-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![DOI](https://img.shields.io/badge/DOI-10.5281%2Fzenodo.21514973-blue)](https://doi.org/10.5281/zenodo.21514973)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)


*Parts* unifies composition and implementation inheritance in a single compositional model.

Objects are assembled from late-bound components instead of fixed inheritance hierarchies, yet behave as if they were
defined with implementation inheritance.

- Use `@link` to automatically implement interfaces through the fields of a class
- Mark a class with `@part` so its overrides apply *everywhere* in the composed object (solves the [Self problem](https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html))
- Configure composition dynamically with constructor injection (think language support for DI)
- Safely share interface implementations (solves the [Diamond problem](https://en.wikipedia.org/wiki/Multiple_inheritance#The_diamond_problem))
- Preserve Java's dynamic-dispatch performance (see [Interface-Scoped Dispatch](https://doi.org/10.5281/zenodo.21514973))

 <!-- TOC -->
* [Parts](#parts)
* [Basic usage](#basic-usage)
  * [`@link`](#link)
  * [`@part`](#part)
* [Forwarding](#forwarding)
    * [A one-way flight](#a-one-way-flight)
* [Delegation](#delegation)
    * [Self-preservation](#self-preservation)
    * [Inheritance](#inheritance)
    * [Default methods](#default-methods)
* [Diamonds](#diamonds)
* [Example](#example)
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

## `@link`
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
 
## `@part`
Use `@part` to enable delegation with `@link`.

Generally, a link establishes a "part-of" relationship between the linking object and the linked `part`. Both objects form
a single, composite object in terms of the interfaces defined in the link. 

```java
interface Doubler {
  int getDown();
  int doubleDown();
}

@part class DoublerPart implements Doubler {
  public int getDown() {return 0;}
  
  // call to getDown() is polymorphic when used with @link
  public int doubleDown() {return getDown() * 2;}
}

class MyClass implements Doubler {
  @link Doubler doubler = new DoublerPart();
  
  // overrides doubler's getDown()
  @Override public int getDown() {return 8;}
}

Doubler doubler = new MyClass();
out.println(doubler.doubleDown());
```
Output:
```text
    16
```
DoublerPart's `@part` annotation enables true delegation in MyClass's link.

The takeaway from this example is DoublerPart's call to `getDown()` calls MyClass's `getDown()`, indicating linked interfaces
are polymorphic wrt `part` classes, thus fulfilling the _true_ qualifier in "true delegation". The [Delegation](#delegation) section
covers more about the what and how of `@part`.

# Forwarding
Forwarding uses a separate object to handle unimplemented interface calls. A class implements an interface simply by invoking
the methods on another object that implements the interface.

With `@link` this process is handled automatically.

A simple example demonstrating interface composition via forwarding with a map.
```java
public class StringMap<E> implements Map<String, E> {
  @link Map<String, E> map = new HashMap<>();

  public boolean equals(Object o) {return map.equals(o);}
  public int hashCode() {return map.hashCode();}
}
``` 
The advantage over implementation inheritance is that the implementation of StringMap is decoupled from HashMap; only the Map
interface is exposed. `@link` performs the grunt work of forwarding unimplemented Map calls.
                                                                                                    
### A one-way flight

Here, StudentPart uses `@link` to automatically transfer calls to unimplemented Person methods to the `person` field.
But PersonPart does something interesting, its implementation of `getTitledName()` calls other Person methods. 

```java
interface Student extends Person {
  String getMajor();
}
interface Person {
  String getName();
  String getTitle();
  String getTitledName();
}

class StudentPart implements Student {
  @link Person person;
  private final String major;
  
  public StudentPart(Person person, String major) {
    this.person = person;
    this.major = major;
  }

  public String getTitle() {return "Student";}
  public String getMajor() {return major;}
}

class PersonPart implements Person {
  private final String name;

  public PersonPart(String name) {this.name = name;}
  
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {return getTitle() + " " + getName();}
}

PersonPart person = new PersonPart("Milton");
StudentPart student = new StudentPart(person, "Metallurgy");
out.println(student.getTitledName());
```
Output:
```text
    Person Milton
```
With forwarding, the object handling the calls is unaware of the link defined in the forwarding object. As a consequence,
forwarded calls are one-way flights. The call to `getTitle()` from `PersonPart#getTitledName()` is invoked _statically_,
StudentPart's override is ignored.

Generally, linked interface calls within forwarded objects are not polymorphic. This behavior is often referred to as
_the Self problem_.


# Delegation

If PersonPart is annotated with `@part`, Person methods are called using _delegation_.

Delegation is more rigorous. It enables polymorphic calls from linked parts where StudentPart can override Person methods
so that the implementation of Person defers to StudentPart.
```java
@part class PersonPart implements Person {
  . . .
}
```
With `@part` the call to `student.getTitledName()` results in:
```text
    Student Milton
```
Inside PersonPart `this` refers to StudentPart in terms of the Person interface. Thus, the call to `getTitle()` dispatches
_dynamically_. This "true" form of delegation solves _the Self problem_.

### Self-preservation

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

### Inheritance

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

### Default methods

Consider `getTitledName()` as a default method in Person instead of an implementation in PersonPart.
```java
interface Person {
  String getName();
  String getTitle();
  default String getTitledName() {return getTitle() + " " + getName();}
}
```  
Calls must behave identically regardless of where the method is implemented; polymorphism must be preserved when using `part`
classes. As such, the call to `student.getTitledName()` dispatches dynamically as before:
```text
    Student Milton
```    
Inside the Person interface `this` refers to StudentPart even when called from PersonPart.
 

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
interface Teacher extends Person {
  String getDepartment();
}

interface TA extends Student, Teacher {
}

@part class TeacherPart implements Teacher {
  @link Person person;
  private final String department;

  public TeacherPart(Person person, String department) {
    this.person = person;
    this.department = department;
  }
  public String getTitle() {return "Teacher";}
}

@part class TaPart implements TA {
  @link(share=Person.class) Student student; // use student as the person
  @link Teacher teacher;

  public TaPart(Student student) {
    this.student = student;
    this.teacher = new TeacherPart(student, "Math"); // the student is the teacher
  }
}
```
The student is the teacher, so TaPart shares the Person link with `@link(share=Person.class)` and the supplied `student`
is passed along to the Teacher constructor. Without `share=Person.class` a compiler error indicates the overlap with Person:
```java
java: Interface 'Person' found in other links: '_teacher'.
Use '@link(share=Person.class)' to share the 'Person' link with '_teacher' or implement the interface directly.
```

>Note, `part` classes are _not_ required with `@link(share=...)`; it applies to forwarding as well.

# Example

Here is the Student/Teacher example in one code sample for easier readability.

```java
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

public class DelegationExample
{
  interface Person
  {
    String getName();

    String getTitle();

    String getTitledName();
  }

  interface Teacher extends Person
  {
    String getDept();
  }

  interface Student extends Person
  {
    String getMajor();
  }

  interface TA extends Student, Teacher
  {
  }

  static @part class PersonPart implements Person
  {
    private final String name;

    public PersonPart( String name )
    {
      this.name = name;
    }

    public String getName()
    {
      return name;
    }

    public String getTitle()
    {
      return "Person";
    }

    public String getTitledName()
    {
      return getTitle() + " " + getName();
    }
  }

  static @part class TeacherPart implements Teacher
  {
    @link
    Person person;
    private final String dept;

    public TeacherPart( Person person, String dept )
    {
      this.person = person;
      this.dept = dept;
    }

    public String getTitle()
    {
      return "Teacher";
    }

    public String getDept()
    {
      return dept;
    }
  }

  static @part class StudentPart implements Student
  {
    @link
    Person person;
    private final String major;

    public StudentPart( Person person, String major )
    {
      this.person = person;
      this.major = major;
    }

    public String getTitle()
    {
      return "Student";
    }

    public String getMajor()
    {
      return major;
    }
  }

  static @part class TaPart implements TA
  {
    @link(share = Person.class)
    Student student;
    @link
    Teacher teacher;

    public TaPart( Student student )
    {
      this.student = student;
      this.teacher = new TeacherPart( student, "Math" );
    }

    public String getTitle()
    {
      return "TA";
    }
  }

  public static void main( String[] args )
  {
    Person person = new PersonPart( "Milton" );
    Student student = new StudentPart( person, "CS" );
    TA ta = new TaPart( student );
    String titledName = ta.getTitledName();
    System.out.println( titledName );
  }
}
```
Output:
```text
    TA Milton
```

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
versions 8 - 25.

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
LTS release (8 - 25) or latest JDK release, the script takes care of the rest.
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
