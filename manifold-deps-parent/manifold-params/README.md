> **⚠️ Experimental Feature**

# Optional parameters & named arguments

![latest](https://img.shields.io/badge/latest-v2025.1.6-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

The `manifold-params` compiler plugin enhances Java with **optional parameters** and **named arguments** for methods, constructors,
and records. It offers a cleaner, more flexible alternative to method overloading or builder patterns.
                                                                         
```java
public String valueOf(char[] data, 
                      int offset = 0, 
                      int count = data.length - offset) {...}

valueOf(array) // use defaults for offset and count
valueOf(array, 2) // use default for count
valueOf(array, count:20) // use default for offset by naming count
```

This plugin supports JDK versions 8 - 21 (and the latest) and integrates seamlessly with **IntelliJ IDEA** and **Android Studio**.

### Key Features
- **Optional Parameters**: Define default values for method parameters
- **Named Arguments**: Pass arguments by name, not just by position
- **Binary Compatible**: Works seamlessly with legacy code

# Contents

<!-- TOC -->
* [Optional parameters](#optional-parameters)
* [Named arguments](#named-arguments)
* [Overloading and overriding](#overloading-and-overriding)
* [Binary compatible](#binary-compatible)
    * [Backward compatible](#backward-compatible-)
    * [Binary accessible](#binary-accessible)
    * [Java standard compatible](#java-standard-compatible)
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

You can use optional parameters in methods, constructors, and records.
```java
// method
void chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// constructor
Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// record
record Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}
```

Default values can reference preceding parameters and include complex expressions.
```java
public record Destination(Country country, 
                          String city = country.capital()) {}
```

You can still use positional arguments, even when optional parameters come first.
```java
public Item(int id = -1, String name) {...}

new Item(123, "Table");  
new Item("Chair"); // default value id = -1 is used
```

# Named arguments

You can use named arguments when calling methods, constructors, or records that have optional parameters. This feature allows
you to focus on the specific arguments you need, making call sites more readable and easier to understand.
```java
new Pizza(size:Medium, kind:Detroit, pepperoni:true);
. . .
record Pizza(Size size,
             Kind kind = Thin,
             Sauce sauce = Red,
             Cheese cheese = Mozzarella, 
             boolean pepperoni = false,
             boolean prosciutto = false) {}
```
In this case, named arguments make it clear which values are being assigned to which parameters, improving the overall
readability of the code.

You can also reorder named arguments and mix positional and named arguments, but named arguments must come after positional arguments.
```java
new Pizza(Large, cheese:Fresco, sauce:Chili);
```

# Overloading and overriding

In method overriding, the default values for optional parameters are inherited from the superclass, and cannot be changed
in the subclass.
```java
public interface Contacts {
  Contact add(String name, String address = null, String phone = null);
}

public class MyContacts implements Contacts {
  @Override // no default parameter values allowed here, they are strictly inherited
  public Contact add(String name, String address, String phone) {...}
}
```

Methods with optional parameters can replace multiple overloaded methods. For example, this method:
```java
void func(String a, int b = 0, int c = b )
```
Replaces these overloads:
```java
func(String) 
func(String, int) 
func(String, int, int)
```
This forms the **signature set** of `func`.

Thus, in a subclass `func` overrides a superclass method with matching signatures.
```java
class Base {
  void func(String a, int b) {...}
}

class Sub extends Base {
  @Override
  void func(String a, int b = 0, int c = b) {...}
}

Base base = new Sub();
base.func("hi", 5); // calls Sub#func
```
Here `Sub#func` indirectly overrides `Base#func`.

Overloading is allowed with optional parameters, but if the signature sets overlap, a compile-time error will occur.
```java
class Sample {
  void func()
  void func(int alpha = 0) //error: clashes with func()
  void func(int beta, int gamma = 0) //error: clashes with func(int)
}
```

# Binary compatible

Optional parameters and named arguments preserve three essential aspects of binary compatibility:
- **Backward compatible**
- **Binary accessible**
- **Java standard compatible**

### Backward compatible  

You can add new optional parameters to existing methods without breaking compatibility with code compiled before those
parameters were introduced.  

**For example:**  
Suppose you have the following method:  
```java
public void size(int width) { ... }
```  
You can update it to include an optional `height` parameter:
```java
public void size(int width, int height = 0) { ... }
```  
Code compiled with the earlier version of the method will continue to work without requiring recompilation.

**Changing Default Values:**  
If the default value of an optional parameter changes, the updated value will apply even when older code calls the method.
```java
public void size(int width, int height = width) { ... }
```  
In this case, older code that doesn’t specify a `height` argument will use the new default (`width`) at runtime without
needing to recompile.

**⚠️ Caution:**  
Changing default values, while binary-compatible, can still impact the behavior of existing code. Make these changes
cautiously to prevent unintended side effects.

### Binary accessible
Optional parameters and named arguments are fully accessible from compiled .class files, just like source code.
```java
// .class file

public class MyClass {
  public void size(int width, int height = width) {...}
}
```
```java
// .java file

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
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
     implementation 'systems.manifold:manifold-params-rt:2025.1.6'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-params:2025.1.6'
     testAnnotationProcessor 'systems.manifold:manifold-params:2025.1.6'
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
        <manifold.version>2025.1.6</manifold.version>
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
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params/2025.1.6/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params/2025.1.6)

`manifold-params-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params-rt/2025.1.6/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params-rt/2025.1.6)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

See the [tags on this repository](https://github.com/manifold-systems/manifold/tags) for available versions.

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
