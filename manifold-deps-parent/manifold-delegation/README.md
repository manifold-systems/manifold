# Delegation with links & parts
  
> **⚠ Experimental Feature**

> WARNING: Under construction!!!
 
The `manifold-delegation` project is a compiler plugin providing language support for call forwarding and true delegation.
These features are an experimental effort toward interface composition as a practical alternative to implementation inheritance.

Use `@link` to automatically transfer calls to unimplemented interface methods through fields in the same class.

* Choose between call forwarding and true delegation with `@part`
* Override linked interface methods (solves [the Self problem](https://web.media.mit.edu/~lieber/Lieberary/OOP/Delegation/Delegation.html))
* Share super interface implementations (solves [the Diamond problem](https://en.wikipedia.org/wiki/Multiple_inheritance#The_diamond_problem))
* Configure class implementation dynamically

# Basic usage

## `@link`
```java
class MyClass implements MyInterface {
  @link MyInterface myInterface; // transfers calls on MyInterface to myInterface

  public MyClass(MyInterface myInterface) {
    this.myInterface = myInterface; // dynamically configure behavior
  }
  
  // No need to implement MyInterface here, but you can override myInterface as needed
}
```
## `@part`
```java
@part class DoublerPart implements Doubler {
  public int getDown() {return 0;}
  
  // call to getDown() is polymorphic when used with @link
  public int doubleDown() {return getDown() * 2;}
}

interface Doubler {
  int getDown();
  int doubleDown();
}
```

# Forwarding

Generally, the difference between forwarding and true delegation is that forwarding does not fully support virtual methods,
while true delegation does. This difference is at the heart of _the Self problem_ (aka _broken delegation_).

In terms of this project, delegation works exclusively with `@part` classes. If a `@part` class is assigned to a `@link`
field, the link uses delegation and fully supports polymorphic calls. Otherwise, the link uses forwarding.

```java
class MyStudent implements Student {
  @link Person person;
  private final String major;
  
  public MyStudent(Person person, String major) {
    this.person = person;
    this.major = major;
  }

  public String getTitle() {return "Student";}
  public String getMajor() {return major;}
}

interface Person {
  String getName();
  String getTitle();
  String getTitledName();
}
interface Student extends Person {
  String getMajor();
}
```
With `@link` on the `person` field MyStudent automatically transfers calls to unimplemented Person methods to the field.
```java
class MyPerson implements Person {
  private final String name;

  public PersonPart(String name) {this.name = name;}
  
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {return getTitle() + " " + getName();}
}

MyPerson person = new MyPerson("Milton");
MyStudent student = new MyStudent(person, "Metallurgy");
out.println(student.getTitledName());
```
Since MyPerson is _not_ annotated with `@part` forwarding is used to transfer interface method calls.

But with forwarding, since the calls are one-way tickets, the call to `student.getTitledName()` results in:
```text
    Person Milton
```
With forwarding the call to `getTitle()` from MyPerson is not polymorphic with respect to the link established
in MyStudent.

Generally, this behavior can be viewed as positive or negative, depending on the desired call transfer model.

# Delegation

If the field's value is a `@part` class, the Person methods are called using _delegation_. Unlike forwarding, delegation
enables polymorphic calls; MyStudent can override Person methods so that the implementation of Person defers to MyStudent.
Essentially, `@part` solves _the Self problem_.
```java
@part class PersonPart implements Person {
  private final String name;

  public PersonPart(String name) {this.name = name;}
  
  public String getName() {return name;}
  public String getTitle() {return "Person";}
  public String getTitledName() {return getTitle() + " " + getName();}
}

PersonPart person = new PersonPart("Milton");
MyStudent student = new MyStudent(person, "Metallurgy");
out.println(student.getTitledName());
```
The call to `student.getTitledName()` results in:
```text
    Student Milton
```
This is because PersonPart is a `part` class, which enables polymorphic calls from linked parts. This means inside PersonPart
`this` refers to MyStudent in terms of the Person interface. Thus, the call to `getTitle()` dispatches dynamically to MyStudent.

If PersonPart were _not_ annotated with `@part`, the result would have been:
```text
    Person Milton
```
Because without `@part` PersonPart does not know of its role as a link in MyStudent; `this` refers to PersonPart in terms
of the Person interface. As a consequence, `getTitle()` dispatches statically to PersonPart.
 

## Default methods

Consider `getTitledName()` as a default method in Person instead of an implementation in PersonPart.
```java
interface Person {
  String getName();
  String getTitle();
  default String getTitledName() {return getTitle() + " " + getName();}
}
```  
Calls must behave identically regardless of where the method is implemented; polymorphism must be preserved when using `part`
classes. As such the call to `student.getTitledName()` results just as before:
```text
    Student Milton
```    
Inside Person `this` refers to MyStudent even when called from PersonPart.

# Diamonds

When super interfaces overlap, a "diamond" relationship results. This is known as _the diamond problem_.
```text
         Person
           ▲▲
      ┌────┘└────┐
   Student    Teacher
      ▲          ▲
      └────┐┌────┘
           TA
```
Should TA use Student's Person or Teacher's? Use `@link(share=true)` to resolve the ambiguity.
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
  @link(share=true) Student student; // use student as the person
  @link Teacher teacher;

  public TaPart(Student student) {
    this.student = student;
    this.teacher = new Teacher(student, "Math"); // the student is the teacher
  }
}
```
The student is the teacher, so TaPart shares the link to student with `@link(share=true)` and student is passed along to
the Teacher constructor.

>Note, `part` classes are not required with `@link(share=true)`; it works with both forwarding and delegation.

# Structural interfaces

Sometimes the class you want to link to doesn't implement the interface you want to expose. If you don't control the implementation
of the class, you can define a [structural interface](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
to map specific methods you want to expose.
```java
@Structural
interface LimitedList<E> {
  boolean add(E e);
  E get( int index );
  boolean contains(Object e);
}

class MyLimitedList<E> implements LimitedList<E> {
  @link LimitedList _list;

  public MyLimitedList(LimitedList  list) {
    _list = list;
  }
}

// ArrayList structurally satisfies LimitedList
LimitedList<String> limitedList = new MyLimitedList<>((LimitedList<String>)new ArrayList<>());
limitedList.add("hi");
assertTrue(limitedList.contains("hi"));
assertEquals("hi", limitedList.get(0));
```

# Inheritance

Delegation involves a composite object consisting of a root object and its graph of linked `part` classes. Inside
this composite object linked interface calls are always applied to the root object and never to the linked parts; `this`
must always refer to the root in terms of the interfaces defined by the links.

If any of the linked parts are allowed to directly refer to another linked part, delegation is broken. Polymorphic calling
is compromised because a direct reference to another link bypasses the root, which must always dispatch all interface calls.

Therefore, to maintain delegation integrity, `part` classes may only subclass other `part` classes.

# IDE Support

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

# Setup

## Building this project

The `manifold-delegation` project is defined with Maven.  To build it install Maven and a Java 8 JDK and run the following
command.
```
mvn compile
```

## Using this project

The `manifold-delegation` dependency works with all build tooling, including Maven and Gradle. It fully supports Java
versions 8 - 19.

This project consists of two modules:
* `manifold-delegation`
* `manifold-delegation-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-delegation-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-delegation` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 19), the script takes care of the rest.
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
     implementation 'systems.manifold:manifold-delegation-rt:2022.1.38'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-delegation:2022.1.38'
     testAnnotationProcessor 'systems.manifold:manifold-delegation:2022.1.38'
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
        <manifold.version>2022.1.38</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-delegation-rt</artifactId>
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
                    <source>11</source>
                    <target>11</target>
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-delegation</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Javadoc agent

See [Javadoc agent](http://manifold.systems/javadoc-agent.html) for details about integrating specific language extensions
with javadoc.

# Javadoc

`manifold-delegation`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-delegation/2022.1.38/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-delegation/2022.1.38)

`manifold-delegation-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-delegation-rt/2022.1.38/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-delegation-rt/2022.1.38)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
