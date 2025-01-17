> **⚠ Experimental**

# Optional parameters & named arguments

![latest](https://img.shields.io/badge/latest-v2024.1.43-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

The `manifold-params` project is a compiler plugin that implements binary compatible _optional parameters and named arguments_
for methods, constructors, and records. Use it with any Java project to add clarity and flexibility to call sites and as
a refreshing alternative to method overloads and builders.
                                                                         
```java
public String valueOf(char[] data, 
                      int offset = 0, 
                      int count = data.length - offset) {...}

valueOf(array) // use defaults for offset and count
valueOf(array, 2) // use default for count
valueOf(array, count:20) // use default for offset by naming count
```

Optional parameters and named arguments are fully integrated in both **IntelliJ IDEA** and **Android Studio**.
                                                    
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

An optional parameter has a default value, much like a field or local variable can have an initial value.
```java
void println(String text = "") {...}
```
The default value `""` makes the `text` parameter optional so that `println` may be called with or without an argument.
```java
println(); // same as calling println("");
println("flubber");
println(text:"flubber"); // named argument syntax
```

Optional parameters are supported equally in methods, constructors, and records.
```java
// method
void chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// constructor
Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}

// record
record Chair(Kind kind, Wood wood = Walnut, boolean antique = false) {...}
```

A default parameter value may be arbitrarily complex and may reference preceding parameters.
```java
public record Destination(Country country, String city = country.capital()) {}
```

If an optional parameter precedes a required parameter, positional arguments may still be used with all the parameters.
```java
public Item(int id = -1, String name) {...}

new Item(123, "Table");  
new Item("Chair"); // default value id = -1 is used
```

# Named arguments

Arguments may be named when calling any method, constructor, or record having optional parameters.
```java
record Pizza(Size size,
             Kind kind = Thin,
             Sauce sauce = Red,
             Cheese cheese = Mozzarella, 
             boolean pepperoni = false,
             boolean mushrooms = false,
             boolean prosciutto = false) {}
```
Naming arguments adds clarity to call sites. And with optional parameters you only have to supply the arguments you need.
```java
new Pizza(size:Medium, kind:Detroit, pepperoni:true);
```
Order named arguments to your liking, and mix positional arguments with named arguments. But a labeled argument may not
precede a positional argument.
```java
new Pizza(Large,
          proscuitto:true,
          mushrooms:true, 
          cheese:Goat);
```

# Overloading and overriding

A method override inherits all the super method's default parameter values. The default values are fixed in the super class
and may not be changed in the overriding method.
```java
public interface Contacts {
  Contact add(String name, String address = null, String phone = null);
}

public class MyContacts implements Contacts {
  @Override // no default parameter values allowed here, they are strictly inherited
  public Contact add(String name, String address, String phone) {...}
}
```

A method having optional parameters logically consists of all the methods one would otherwise write using method overloads
to approximate optional parameters.
```java
void func(String a, int b = 0, int c = b )
```
Here, `func` covers the following method signatures.
```java
func(String) 
func(String, int) 
func(String, int, int)
```
This is the _signature set_ of `func`. Accordingly, `func` indirectly overrides any super type method with a signature
belonging to this set.
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

Overloading is permitted with optional parameter methods, however a compiler error results if signature sets overlap.
```java
class Sample {
  void func()
  void func(int alpha = 0) //error: clashes with func()
  void func(int beta, int gamma = 0) //error: clashes with func(int)
}
```

# Binary compatible

There are three aspects of binary compatibility with optional parameters and named arguments.
- Backward compatible
- Binary accessible
- Java standard compatible

### Backward compatible                                                      
Adding new optional parameters to existing methods is binary compatible with code compiled before adding the new parameters.

Version 1.
```java
public void size(int width) {...}
```
Version 2 adds optional parameter `height`.
```java
public void size(int width, int height = width) {...}
```
Code compiled with v1 still runs with v2, without having to recompile with v2.
                                                       
### Binary accessible
Although not built into the JVM, optional parameters and named arguments are just as accessible and usable from compiled
code as from source.
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
`size` in .class file `MyClass` is directly accessible as if from source.

### Java standard compatible
Code compiled without `manifold-params` can still benefit from code compiled with optional parameters. In
this case default parameter values can be used with calls to method overloads that are generated for this purpose.
```java
size(myWidth);
size(myWidth, myHeight);
``` 

# IDE Support

Optional parameters and named arguments are fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

# Setup

## Building this project

The `manifold-params` project is defined with Maven.  To build it install Maven and a Java 8 JDK and run the following
command.
```
mvn compile
```

## Using this project

The `manifold-params` dependency works with all build tooling, including Maven and Gradle. It fully supports Java
versions 8 - 21.

This project consists of two modules:
* `manifold-params`
* `manifold-params-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-params-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-params` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired JDK
LTS release (8 - 21) or latest JDK release, the script takes care of the rest.
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
     implementation 'systems.manifold:manifold-params-rt:2024.1.51'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-params:2024.1.51'
     testAnnotationProcessor 'systems.manifold:manifold-params:2024.1.51'
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
        <manifold.version>2024.1.51</manifold.version>
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
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params/2024.1.51/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params/2024.1.51)

`manifold-params-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params-rt/2024.1.51/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params-rt/2024.1.51)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
