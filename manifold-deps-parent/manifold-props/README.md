# Properties for Java
  
> **⚠ Experimental Feature**
 
The `manifold-props` project is a compiler plugin to simplify declaring and using properties in Java. Use it to reduce
the amount of code you would otherwise write and to improve your overall dev experience with properties. 
```java
public interface Book {
  @var String title; // no more boilerplate getter/setter methods!
}
```
Refer to it directly by name:
```java
book.title = "Daisy";     // calls setter
String name = book.title; // calls getter 
book.title += " chain";   // calls getter & setter
```
Additionally, the feature automatically _**infers**_ properties, both from your existing source files and from
compiled classes your project uses.

Reduce property *use* from this:
```java
Actor person = result.getMovie().getLeadingRole().getActor();
Likes likes = person.getLikes();
likes.setCount(likes.getCount() + 1);
```
to this:
```java
result.movie.leadingRole.actor
  .likes.count++;
``` 

See [Property inference](#property-inference).

Properties are fully integrated in both **IntelliJ IDEA** and **Android Studio**. Use the IDE's features to create new
properties, verify property references, access properties with code completion, and more. 
<p><img src="http://manifold.systems/images/properties.png" alt="properties" width="50%" height="50%"/></p>

### Table of Contents
* [Declaring properties](#declaring-properties)
* [Modifiers](#modifiers)
* [Computed properties](#computed-properties)
* [Backing fields](#backing-fields)
* [Overriding properties](#overriding-properties)
* [Static properties](#static-properties)
* [L-value benefits](#l-value-benefits)
* [Property inference](#property-inference)
* [Backward compatible](#backward-compatible)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

# Declaring properties

A property is declared as read-write with `@var`, as read-only with `@val`, or write-only with `@set`. 
```java
  public class Person {
    @var String name;
    @var int age;
  }
```

To use a property, simply refer to it by name:
```java
  Person person = new Person();

  person.name = "George"; // setter is called
  person.age = 32;
  
  String name = person.name;  // getter is called
  int age = person.age; 
```
The compiler automatically creates a `private` backing field and getter/setter methods where necessary:
```java
public class Person {
  private String name;
  private int age;
  
  public String getName() {
    return this.name;
  }  
  public void setName(String value) {
    this.name = value;
  }
  
  public int getAge() {
    return this.age;
  }
  public void setAge(int value) {
    this.age = value;
  }
}
```
Property access compiles as getter/setter calls:
```java
  Person person = new Person();

  person.setName("George");
  person.setAge(32);
  
  String name = person.getName();
  int age = person.getAge(); 
```

`@var` declares a *read/write* property; the compiler automatically provides getter and setter methods if there are no
user-defined ones, and a private backing field if needed.

`@val` is similar to `@var`, but is *read-only* -- just a getter is provided or user-defined, no setter.

`@set` is the opposite of `@val` -- *write-only*, just a setter is provided or user-defined, no getter;

`@var`, `@val`, and `@set` default to `public` and provide optional parameters for overriding the declared access
privilege and other modifiers. They can also add arbitrary annotations to generated getter/setter methods and
parameters.

```java
// public name property adds @MyAnnotation to generated getter/setter methods and makes the setter `protected`
@var(annos=@MyAnnotation) @set(Protected) String name;
```

The full syntax for declaring a `@var` property:

```bnf
[modifiers] @var[(<options>)] [<@get(<options>)>] [<@set(<options>)>] <type> <name> [= <initializer>];
[<getter>]
[<setter>]
```

# Modifiers
                 
Properties are always `public` by default.

A property's modifiers always apply to its _getter/setter methods_.

As this example illustrates, `protected`, `abstract`, and `final` all apply exclusively to the getter/setter methods:
```java 
public abstract class Account {
  final @var String name;
  protected abstract @var int rate;
}
```
Compiles as:
```java 
public abstract class Account {
  private String name;

  public final String getName() {return name;}
  public final void setName(String value) {this.name = value;}

  protected abstract int getRate();
  protected abstract void setRate(int value);
}
```
`final` applies to the getter/setter methods too. Since the state for a `@val` is inherently final, a `final @val` declare a
final getter:
```java
final @val Map<String, Object> bindings = new HashMap<>();
```
Compiles as:
```java
private final Map<String, Object> bindings = new HashMap<>();
. . . 
public final Map<String, Object> getBindings() {return bindigs;}
```
`static` applies to both getters/setters and state.
```java
public class Tree {
  static @var @set(Private) int nodeCount;
}
. . .
Tree.nodeCount++;
```
Compiles as:
```java
public class Tree {
  private static int nodeCount;

  public static int getNodeCount() {return nodeCount;}
  private static void setNodeCount(int value) {nodeCount = value;}
}
. . .
Tree.setNodeCount(Tree.getNodeCount() + 1);
```

# Computed properties

A property that is not used in its getter/setter methods is considered a _computed_ property; it does not need the property
field to maintain state, thus no "backing" field is provided.
```java
public class Foo {
  @var Stuff stuff; // "computed property", user-defined getter/setter

  public Stuff getStuff() {
    return lookupStuff(); // does not access `stuff`
  }
  public void setStuff(Stuff value) {
    assignStuff(value); // does not access `stuff`
  }
}
```
```java
    Foo foo = new Foo();
    Stuff s = foo.stuff; // calls getter
    foo.stuff = new Stuff(); // calls setter
```
Compiles as:

```java
public class Foo {
  // no backing field generated

  public Stuff getStuff() {
    return lookupStuff();
  }  
  public void setStuff(Stuff value) {
    assignStuff(value);
  }
}
```
```java
    Foo foo = new Foo();
    Stuff s = foo.getStuff();
    foo.setStuff(new Stuff());
```
>Note if you omit `@var Stuff stuff`, users of class `Foo` can still use `stuff` as a property.
> See [Property inference](#property-inference).
 
# Backing fields

Getter/setter methods have exclusive access to the private backing field for a property. If they don't use the backing field,
the compiler does not generate one. References to a property outside the getters/setters are compiled as calls to them.
```java
  @var String name;
  public void setName(String value) {
    name = validateName(value); // directly assigns to backing field 'name' 
  }
  
  public String toString() {
    return name; // calls getName()
  }
```

# Overriding properties

Overriding a property is similar to overriding a method. 

Creating, implementing, and using an interface:
```java
public interface Entity {
  @var String name;
  @var int age;
}

public class Person implements Entity {
  @override @var String name;
  @override @var int age;
  
  public Person(String name, int age) {
    this.name = name;
    this.age = age;
  }
}

Entity entity = new Person("Fred", 42);
entity.name = "Frederick";
print(entity.name);
print(entity.age);
```

A subclass can override a super class property.

```java
public class Shape { 
  @val int sides = 0;
}

class Rectangle extends Shape {
  @override @val int sides = 4;
}
```
`@var` can override `@val`, but not vice versa. This is because `@var` provides the getter necessary for `@val`, but
`@val` does not provide the setter required by `@var`. Similarly, `@var` can override `@set`. 
                                                                                
Note you must use `@override` when overriding a property. This way the compiler can help you identify broken code if the
super property changes or is removed in a later release.

If you only need to override the _implementation_ of a getter or a setter, just do that:
```java
public class Person {
  @var String name;
}

public class MrPerson extends Person {
  @Override public String getName() {
    return "Mr. " + super.name;
  }
}

```

# Static properties

Properties can be static too.

```java
public class Foo {
  static @var String data;
}
```
```java
    Foo.data = "the eagle has landed";
    String data = Foo.data;
```
Compile as:
```java
public class Foo {
  private static String data;

  public static String getData() {
    return data;
  }
  public static void setData(String value) {
    Foo.data = value;
  }
}
```
```java
Foo.setData("the eagle has landed");
String data = Foo.getData();
```

Note, since interfaces are restricted to read-only static data, static interface properties are limited to `@val` and calculated `@var` static properties.


# L-value benefits

Properties fully support all of Java's assignment operations:

- Compound assignment expressions: `bill.total *= rate;`
- Increment/decrement expressions: `game.score++;`
- Assignment expressions: `Result res = tracking.lastSearch = search();`

# Property inference

Properties are inferred from classes that follow the Java naming convention for getters and setters. As a result, you
can use property syntax instead of calling getter/setter methods with any Java class. 
```java
import java.util.Calendar;

Calendar calendar = Calendar.instance;  // call getInstance()
if (calendar.firstDayOfWeek == Calendar.SUNDAY) {  // call getFirstDayOfWeek()
    calendar.firstDayOfWeek = Calendar.MONDAY; // call setFirstDayOfWeek()
}
if (!calendar.isLenient) { // call isLenient()
    calendar.isLenient = true; // call setLenient()
}

```  

Properties are inferred from _both_ existing source classes in your project and compiled classes your project uses.

### Works with records

Declared fields in record classes are effectively read-only properties and are inferred as such.
```java
public record Person(String name, LocalDate dateOfBirth) {}
```
Property usage:
```java
Person person = new Person("Fred", LocalDate.of(1969, 11, 14));
String name = person.name; // access as properties
LocalDate dob = person.dateOfBirth;
```

### Works with code generators

Property inference automatically works with code generated from annotation processors and Javac plugins. This is
significant since generated code tends to reflect object models which result in a lot of getter/setter boilerplate.

For instance, using property inference reduces this GraphQL excerpt:
```java
Actor person = result.getMovie().getLeadingRole().getActor();
Likes likes = person.getLikes();
likes.setCount(likes.getCount() + 1);
```
to this:
```java
result.movie.leadingRole.actor
  .likes.count++;
``` 

# Backward compatible

Projects not using Manifold can still use classes compiled with Manifold properties. In this case a project uses
getter/setter methods as it normally would.


# IDE Support

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

# Setup

## Building this project

The `manifold-props` project is defined with Maven.  To build it install Maven and a Java 8 JDK and run the following
command.
```
mvn compile
```

## Using this project

The `manifold-props` dependency works with all build tooling, including Maven and Gradle. It fully supports Java
versions 8 - 19.

This project consists of two modules:
* `manifold-props`
* `manifold-props-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-props-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-props` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

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
     implementation 'systems.manifold:manifold-props-rt:2023.1.3'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-props:2023.1.3'
     testAnnotationProcessor 'systems.manifold:manifold-props:2023.1.3'
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
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-props-rt</artifactId>
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
                            <artifactId>manifold-props</artifactId>
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

`manifold-props`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-props/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-props/2023.1.3)

`manifold-props-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-props-rt/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-props-rt/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
