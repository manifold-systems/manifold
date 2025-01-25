> **⚠ Experimental Feature**

# Manifold : Tuples

![latest](https://img.shields.io/badge/latest-v2024.1.54-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)


The tuples feature provides concise expression syntax to group named data items in a lightweight structure.
```java
auto t = (name: "Bob", age: 35);
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
Nest tuples.
```java
var pizza = (
  size: Small, 
  options: (
    shape: Square, 
    pepperoni: true
  )
);
out.println(pizza.options.shape);
```
Tuples satisfy structural interfaces.
```java
Being being = (name:"Scott", age:100);

@Structural
interface Being {
  String getName();
  int getAge();
  // setters too....
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

Note, you must use `auto` to infer a method return type; Java's `var` only works on local variables.

>🛈 While record classes can approximate similar functionality, they do so at a significantly higher cost. Records require
both an explicit type definition and full `new` expression syntax. By contrast, because they are purely structural,
tuples serve as both implied type definitions and concise expression syntax.

## Tuple types 

What is nice about tuples is they don't require a separate type definition, a tuple expression naturally conveys its type. On the other hand not having
a named type means tuple types are not suitable where explicit typing is necessary. For instance, using a tuple type as a function
parameter is awkward and less readable compared with using a nominal type. Because they are purely structural, tuple types must be redefined
wherever they are used whereas a regular Java type is more concisely referenced by name. 

### Inferred

Rather than supporting an awkward feature with readability concerns, Manifold tuple types are strictly anonymous, thus they
must be inferred using [auto](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto) or `var`.

>Note, [auto](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto)
> is more versatile than `var`. While `var` is limited to local variables, `auto` can be applied to fields and method return
> types, as well as locals. `auto` also works with earlier JDKs, including JDK 8. 

Does this mean tuples can't be used with function parameters? Not at all!

More precisely, it means tuple _types_ can't be used with function parameters. Although they are anonymous, tuple types
are also structural, which means they are compatible with Manifold's [structural interfaces](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural). Tuples can structurally
satisfy interfaces annotated with `@Structural`, thus a parameter typed with such an interface can be assignable from a
tuple.

```java
order(Large, (crust:Thick, pepperoni:true));

@Structural interface $order {
  @val Shape shape = Round;
  @val Crust crust = Thin;
  @val Sauce sauce = Red;
  @val boolean cheese = true;
  @val boolean pepperoni = false;
  @val boolean mushrooms = false;
}
void order(Size size, $order options) {...}
```
This technique provides virtual language features for named arguments & optional parameters. Use it as a refreshing
alternative to telescoping methods/constructors, method overloading, and builders.

Learn more, see [named arguments & optional parameters](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#named-arguments--optional-parameters).

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
8 - 21.

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
version (8 - 21), the script takes care of the rest.
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
    implementation 'systems.manifold:manifold-tuple-rt:2024.1.54'
    testCompile 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-tuple', version: '2024.1.54'
    testAnnotationProcessor group: 'systems.manifold', name: 'manifold-tuple', version: '2024.1.54'
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
        <manifold.version>2024.1.54</manifold.version>
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
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-tuple/2024.1.54/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-tuple/2024.1.54)

`manifold-tuple-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-tuple-rt/2024.1.54/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-tuple-rt/2024.1.54)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
