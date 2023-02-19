# Manifold : Tuples

> **⚠ Experimental Feature**

The tuples feature provides concise expression syntax to group named data items in a lightweight structure.
```java
auto t = (name: "Bob", age: "35");
System.out.println("Name: " + t.name + " Age: " + t.age);

auto t = (person.name, person.age);
System.out.println("Name: " + t.name + " Age: " + t.age);
``` 
A tuple expression consists of name/value pairs where the names are optionally labeled, otherwise they are inferred from
expression identifiers or assigned default names. The names are type-safely reflected in the corresponding tuple type,
which is inferred from the expression using [**auto**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto)
or **var**.
>Note, `var` may be used in place of `auto` if using Java 11+, otherwise if using Java 8, you must use `auto` for
> variable type inference.

>See [Type inference with **auto**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto).

You can define tuples with any number of items.
```java
var t = 
  (1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
   11, 12, 13, 14, 15, 16, 17, 18);
```
Tuples are read/write.
```java
var t = (name: "Bob", age: 35);
...
t.name = "Alec";
```
Copy tuples.
```java
var t = (name, age);
var t2 = t.copy(); // shallow copy
out.println(t2.name);
```
Tuples are iterable.
```java
var t = ("cat", "dog", "chihuahua");
for(TupleItem item: t) {
  out.println("Name: " + item.getName() + " Value: " + item.getValue());  
}
```

## Tuple labels
Data items are optionally labeled.
```java
var t = (name: "Bob", age: 35);
String name = t.name;
int age = t.age;
```
If a label is omitted, it is inferred from the value expression, or is given a default name. Default names
are `item1`, `item2`, etc.
```java
var t = (1, 2, 3);
int one = t.item1;
int two = t.item2;
int three = t.item3;

var t = (person.getName(), person.getAge(), "blue");
String name = t.name;   // inferred
int age = t.age;        // inferred
String color = t.item3; // default
```

## Multiple return values
A common use-case for tuples is to return multiple values from a method.
```java
var result = findMinMax(data);
System.out.println("Minimum: " + result.min + " Maximum: " + result.max);

auto findMinMax(int[] data) {
  if(data == null || data.length == 0) return null;
  int min = Integer.MAX_VALUE;
  int max = Integer.MIN_VALUE;
  for(int i: data) {
    if(i < min) min = i;
    if(i > max) max = i;
  }
  return min, max;
}
```
Here the combined use of tuples and [**auto**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto)
provides a clear and concise syntax for type-safely returning multiple values. As with
fields and local variables, using `auto` on a method infers its return type from its return statements. Additionally,
for improved readability, in a return statement you can omit the parenthesis otherwise required for tuple expressions.
>Note, you must use `auto` to infer a method return type; Java's `var` only works on local variables.

## Tuple types 

### Always inferred
Tuple expressions are designed as a lightweight utility to group loosely related data items. Because their types are
purely structural, they tend to be less desirable as they lack the basic qualities of nominal typing. For instance,
a nominal type such as a class is centrally defined, which enables it to be easily referenced by name, allows it to be
formally documented, and makes it available for deterministic tooling. Tuple types lack these fundamental capabilities.

Another issue with tuple types, again because they are purely structural, is they tend to get quite verbose. And because
they are not centrally defined, they must be redefined wherever they are used. As a consequence, readability suffers.

Manifold works toward solving these problems by altogether hiding tuple types from view. You never directly specify tuple
types or even see them. They are always inferred using [**auto**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto)
or **var**. If you find yourself "needing" a tuple type, as a method parameter for instance, consider instead defining a
[structural interface](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
to reflect the tuple's structure. As such the parameter is more readable, better conveys its purpose, and is generally
more in tune with Java's nominal type system.

### `Tuple` interface
All tuple types implement the `manifold.tuple.rt.api.Tuple` interface. This can be useful, for example, if you need to
test for a tuple type or need to dynamically access a tuple's values.
```java
var t = (person.name, person.age);
foo(t);
  ...
void foo(Tuple t) {
  for(TupleItem item: t) {
  ...
  }  
}
```

### Type equivalence
For type-safety, tuple types are based on both item types and item names. This means the order of name/value pairs in
a tuple expression is insignificant with respect to its type.
```java
var t1 = (name: "Milo", age: 88);
var t2 = (age: 77, name: "Twila");

t1.getClass() == t2.getClass() // true!
```
Here, `t1` and `t2` have the same tuple type because they project the same name/type pairs.

## More examples
A common use-case for tuples involves selecting and organizing data from query results.
```java
/** Selects a list of (name, age) tuples from a list of Person */
public auto nameAge(List<Person> list) { 
  return list.stream()
    .map(p -> (p.name, p.age)) // tuples are powerful here
    .collect(Collectors.toList());
}

var result = nameAge(persons);
for(var t: nameAge(persons)) {
  out.pringln("name: " + t.name + " age: " + t.age);
}
```

## Limitations
### No tuple types
Tuple types are inferred from tuple expressions, there is no way to define a tuple type explicitly. This is a designed
limitation, see [Always inferred](#always-inferred) above.

### Tuples can't reference private classes
A tuple expression may not contain a value having a private inner class type. This is a first draft limitation that will
likely be supported in a future revision.

# IDE Support

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity.

# Setup

## Building this project

The `manifold-tuple` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-tuple` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 19.

This project consists of two modules:
* `manifold-tuple`
* `manifold-tuple-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-tuple-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-tuple` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

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
    implementation 'systems.manifold:manifold-tuple-rt:2023.1.3'
    testCompile 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-tuple', version: '2023.1.3'
    testAnnotationProcessor group: 'systems.manifold', name: 'manifold-tuple', version: '2023.1.3'
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
    <artifactId>my-tuple-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Tuple App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-tuple-rt</artifactId>
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
                            <artifactId>manifold-tuple</artifactId>
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

`manifold-tuple`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-tuple/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-tuple/2023.1.3)

`manifold-tuple-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-tuple-rt/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-tuple-rt/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
