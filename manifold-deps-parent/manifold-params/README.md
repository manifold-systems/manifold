> **⚠️ Experimental Feature**

# Optional parameters & named arguments

![latest](https://img.shields.io/badge/latest-v2025.1.21-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

The `manifold-params` compiler plugin adds support for **optional parameters** and **named arguments** in Java methods,
constructors, and records -- offering a simpler, more expressive alternative to method overloading and builder patterns.

```java
public String valueOf(char[] data, 
                      int offset = 0, 
                      int count = data.length - offset) {...}

valueOf(array) // use defaults for offset and count
valueOf(array, 2) // use default for count
valueOf(array, count:20) // use default for offset by naming count
```

This plugin **supports JDK versions 8 - 21+** and integrates seamlessly with **IntelliJ IDEA** and **Android Studio**.

### Key features
* **Optional parameters** -- Define default values directly in methods, constructors, and records
* **Named arguments** -- Call methods using parameter names for clarity and flexibility
* **Flexible defaults** -- Use expressions, reference earlier parameters, or access instance fields
* **Customizable behavior** -- Override default values in subclasses or other contexts
* **Safe API evolution** -- Add parameters or change defaults without breaking binary or source compatibility
* **Eliminates overloads and builders** -- Collapse boilerplate into a single, expressive method or constructor
* **IDE-friendly** -- Fully supported in IntelliJ IDEA and Android Studio

# Contents

<!-- TOC -->
* [Optional parameters](#optional-parameters)
* [Named arguments](#named-arguments)
* [Overloading and overriding](#overloading-and-overriding)
    * [Overriding](#overriding)
    * [Overloading](#overloading)
    * [Default value inheritance](#default-value-inheritance)
* [Binary compatible](#binary-compatible)
    * [Binary backward compatible](#binary-backward-compatible)
    * [Binary accessible](#binary-accessible)
    * [Java standard compatible](#java-standard-compatible)
* [Practical Applications](#practical-applications)
    * [Replacing `copyWith()` in records](#replacing-copywith-in-records)
* [IDE Support](#ide-support)
  * [Install](#install)
* [Setup](#setup)
  * [Building this project](#building-this-project)
  * [Using this project](#using-this-project)
  * [Binaries](#binaries)
  * [Gradle](#gradle)
  * [Maven](#maven)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)
<!-- TOC -->

# Optional parameters

An **optional parameter** has a default value, making it possible to omit the argument when calling the method.
```java
void println(String text = "") {...}

println(); // equivalent to println("");
println("Hello, world!");
println(text:"Hello, named argument!");
```

You can use optional parameters in **methods**, **constructors**, and **records**.
```java
// method
void chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// constructor
Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// record
record Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}
```

**Default values** can reference preceding parameters, instance members, and include complex expressions.
```java
public record Destination(Country country, 
                          String city = country.capital()) {}
```
```java
public class Chat {
  private Color colorPref;
  . . .
  public void message(String text, Color textColor = colorPref) {. . .}
}
```

Optional parameters can appear before required ones -- unlike most languages -- and still support natural positional calls:
```java
public Item(int id = -1, String name) { ... }

new Item(123, "Table");  // id = 123, name = "Table"
new Item("Chair");       // id = -1 (default), name = "Chair"
```
This means you don’t need to reorder parameters awkwardly just to put optional ones last. It keeps method signatures clean
while preserving intuitive calling syntax.

# Named arguments

You can use **named arguments** when calling methods, constructors, or records having optional parameters. This feature allows
you to focus on the specific arguments you need, making call sites more readable and easier to understand.
```java
new Pizza(size:Medium, kind:Detroit, Set.of(Pepperoni));
. . .
record Pizza(Size size,
             Kind kind = Thin,
             Sauce sauce = Red,
             Cheese cheese = Mozzarella, 
             Set<Meat> meat = Set.of(),
             Set<Veg> veg = Set.of()) {}
```
In this case, named arguments make it clear which values are being assigned to which parameters, improving the overall
readability of the code.

You can also **reorder** named arguments and **mix positional and named arguments**, but named arguments must come after
positional arguments.
```java
new Pizza(Large, cheese:Fresco, sauce:Chili);
```

# Overloading and overriding

To understand how optional parameters affect overloading and overriding, it’s important to first understand a method’s
**signature set**--the set of overloads defined by the different ways it can be called with positional arguments.

A method with optional parameters implicitly defines multiple overloads. For example, the following method:

```java
void func(String a, int b = 0, int c = b)
````

can be invoked with one, two, or three positional arguments, which corresponds to this set of overloads:

```java
func(String)
func(String, int)
func(String, int, int)
```

Together, these overloads form the **signature set** of `func`. Signature sets guide method resolution and overriding,
serving as a simple, consistent way to support optional parameters within Java’s overload-based method model.

---

### Overriding

In a subclass, a method with optional parameters **overrides** any superclass method that falls within its *signature set*--even
if the superclass method has fewer parameters.

```java
class Base {
  void func(String a, int b) { ... }
}

class Sub extends Base {
  @Override
  void func(String a, int b = 0, int c = b) { ... }
}
```
Here, `Sub#func` overrides `Base#func` because the latter’s signature is part of `Sub#func`’s signature set:

```java
func(String)
func(String, int)
func(String, int, int)
```

```java
Base base = new Sub();
base.func("hi", 5); // calls Sub#func
```

This behavior allows a subclass to override a method *and* extend its parameter list by adding new optional parameters.
It’s a powerful feature that lets you **evolve or enhance behavior without breaking existing call sites**, making overrides
more expressive and future-friendly.

---

### Overloading

**Overloading** is supported with optional parameters using signature sets (see earlier definition). If two methods' signature
sets overlap in a way that causes ambiguity, a compile-time error is reported, ensuring clarity and preventing conflicts.

```java
class Sample {
  void func(int alpha);  
  void func(int beta, int gamma = 0); // error: overlaps with func(int)
}
```

To prevent ambiguity, all method declarations in a given scope must have disjoint signature sets.

### Default value inheritance

When overriding a method with optional parameters, a subclass **inherits** the default values from the superclass unless it
explicitly **overrides** them.

```java
class Base {
  void sample(String a = "hello", int b = 5) {
    out.println(a + b);
  }
}

class Sub extends Base {
  @Override
  void sample(String a = "hi", int b) {
    super.sample(a, b);
  }
}
```
In this example, `Sub#sample` overrides the default value for `a` but inherits the default for `b`. As a result:
```java
Base base = new Sub();
base.sample();
```
Prints: `hi5`

This selective override mechanism offers fine-grained control, allowing subclasses to adjust default values as needed while
preserving compatibility with the base method signature.

# Binary compatible

Optional parameters and named arguments preserve three essential aspects of binary compatibility:
- **Binary backward compatible**
- **Binary accessible**
- **Java standard compatible**
  
### Binary backward compatible
A **binary backward compatible** library lets newer versions be used seamlessly with applications or libraries compiled against
older versions, so you can **upgrade without recompiling**.

Support for optional parameters helps preserve both **source** and **binary** compatibility by allowing safe modifications
to method signatures and default values.

You can add new optional parameters to existing methods without breaking code compiled or written before those changes --
a safe and powerful way to **extend and evolve APIs** over time.

**For example:**<br>
Suppose you have the following method:
```java
public void size(int width) { ... }
```
You can update it to include an optional `height` parameter:
```java
public void size(int width, int height = width) { ... }
```
Code compiled with the earlier version will continue to work without requiring recompilation.

This also applies to methods with pre-existing optional parameters:
```java
public void size(int width = 0) { ... }
```
Adding more optional parameters preserves binary backward compatibility:
```java
public void size(int width = 0, int height = width) { ... }
```

**Changing Default Values:**<br>
If the default value of an optional parameter changes, the updated value will apply even when older code calls the method.
```java
public void size(int width, int height = width * 2) { ... }
```  
In this case, older code that doesn’t specify a `height` argument will use the new default (`width * 2`) at runtime without
needing to recompile.

**⚠️ Caution:**  
Changing default values, while binary compatible, can still impact the behavior of existing code. Make these changes
cautiously to prevent unintended side effects.

### Binary accessible
Optional parameters and named arguments are fully accessible from compiled .class files, just like source code.
```java
// .class file (compiled)

public class MyClass {
  public void size(int width, int height = width) {...}
}
```
```java
// .java file (source)

MyClass myClass = new MyClass();
myClass.size(width:100);
```

### Java standard compatible
Code compiled without `manifold-params` can still interact with methods compiled using optional parameters, benefiting from
default values when calling method overloads.
```java
size(myWidth);
size(myWidth, myHeight);
``` 

# Showcase

### Adding `copyWith()` to records

Optional parameters make it easy to implement a `copyWith()` method for immutable data types like records — without boilerplate.
By using default values that reference the current instance, you can update only the fields you care about.
                                                                                                           
Building on the prior `Pizza` example:
```java
record Pizza(Size size,
             Kind kind = Thin,
             Sauce sauce = Red,
             Cheese cheese = Mozzarella,
             Set<Meat> meat = Set.of(),
             Set<Veg> veg = Set.of()) {

  public Pizza copyWith(Size size = this.size,
                        Kind kind = this.kind,
                        Cheese cheese = this.cheese,
                        Sauce sauce = this.sauce,
                        Set<Meat> meat = this.meat,
                        Set<Veg> veg = this.veg) {
    return new Pizza(size, kind, cheese, sauce, meat, veg);
  }
  
  public Pizza copy() {
    return copyWith();
  }
}
```

You can construct a `Pizza` using defaults or with specific values:
```java
var pizza = new Pizza(Large, veg:Set.of(Kimchi));
```
Then update it as needed using `copyWith()`:
```java
var updated = pizza.copyWith(kind:Deep, meat:Set.of(PorkBelly));
```
Here, the constructor acts as a flexible, type-safe builder. `copyWith()` simply forwards to it, defaulting unchanged fields.

> ℹ️ This pattern is a candidate for automatic generation in records for a future version of `manifold-params`.
                                               

# IDE Support

The plugin works fully in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

To install, get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from the IDE:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

# Setup

## Building this project

The `manifold-params` project uses Maven. To build it, install Maven and a Java 8 JDK, then run:
```
mvn compile
```

## Using this project

This project works with all Java build tools, including Maven and Gradle, and supports Java versions 8 - 21.

It includes two modules:
* `manifold-params`
* `manifold-params-rt`

For best performance (especially with Android or other JVM languages):
* Add a dependency on `manifold-params-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-params` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

## Binaries

If you are *not* using Maven or Gradle, download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>If you are targeting **Android**, see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired JDK
LTS release (8 - 21) or latest JDK release.
```groovy
plugins {
    id 'java'
}

group 'systems.manifold'
version '1.0-SNAPSHOT'

targetCompatibility = 21
sourceCompatibility = 21

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
     implementation 'systems.manifold:manifold-params-rt:2025.1.21'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-params:2025.1.21'
     testAnnotationProcessor 'systems.manifold:manifold-params:2025.1.21'
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
        <manifold.version>2025.1.21</manifold.version>
        <!-- choose your preferred JDK LTS release, or latest JDK release -->
        <maven.compiler.source>21</maven.compiler.target>
        <maven.compiler.target>21</maven.compiler.release>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-params-rt</artifactId>
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
                    <encoding>UTF-8</encoding>
                    <compilerArgs>
                        <!-- Configure manifold plugin-->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-params</artifactId>
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

`manifold-params`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params/2025.1.21/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params/2025.1.21)

`manifold-params-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params-rt/2025.1.21/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params-rt/2025.1.21)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

See the [tags on this repository](https://github.com/manifold-systems/manifold/tags) for available versions.

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
