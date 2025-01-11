> **⚠ Experimental**

# Optional parameters & named arguments

![latest](https://img.shields.io/badge/latest-v2024.1.43-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

The `manifold-params` project is a compiler plugin that implements optional parameters and named arguments for methods,
constructors, and records. Use it with any Java project to add clarity and flexibility to call sites and to reduce boilerplate
in class definitions.
                                                                         
```java
public String valueOf(char[] data, 
                      int offset = 0, 
                      int count = data.length - offset) {...}

valueOf(array) // use default values for offet and count
valueOf(array, 2) // use default value for count
valueOf(array, count:20) // use default value for offset
```

Optional parameters and named arguments are fully integrated in both **IntelliJ IDEA** and **Android Studio**. Use the IDE's
features to be more productive with optional parameters and named arguments.

## Optional parameters

An optional parameter has a default value assigned to it, much like a field or local variable with an initial value.
```java
void println(String text = "") {...}
```
The default value `""` makes the `text` parameter optional so that `println` may be called with or without an argument.
```java
println(); // same as calling println("");
println("flubber");
println(text: "flubber"); // named argument syntax
```

A default value may be an arbitrary expression of any complexity and can reference preceding parameters.
```java
public record Destination(Country country, String city = country.capital()) {}
```

A method override automatically inherits all the super method's default parameter values. The default values are fixed in
the super class and may not be changed in the overriding method. 
```java
public interface Contacts {
  Contact add(String name, String address = null, String phone = null);
}

public class MyContacts implements Contacts {
  @Override // no default parameter values allowed here
  public Contact add(String name, String address, String phone) {...}
}

Contacts contacts = new MyContacts();
contacts.add("Fred");
// calling directly inherits defaults
((MyContacts)contacts).add("Bob");
```

If an optional parameter precedes a required parameter, positional arguments may still be used with all the parameters.
```java
public Item(int id = -1, String name) {...}

new Item(123, "Table");  
new Item("Chair"); // default value id = -1 is used
```

## Named arguments

Arguments may be named when calling any method, constructor, or record having optional parameters. The naming format follows
manifold's [tuple syntax](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple).

```java
void configure(String name,
               boolean showName = true,
               int color = themeColor(),
               int size = 64, 
               boolean autoSave = true) {...}
```

Naming arguments adds clarity to call sites.
```java
configure(name: "MyConfig",
          showName: false,
          color: 0x7393B3,
          size: 128,
          autoSave: false);
```
But with optional parameters you only have to supply the arguments you need.
```java
configure("Config");
configure(name:"Config", showName:false);
```
And you can order named arguments to your liking, and mix positional arguments with named arguments. But a labeled argument
may not precede a positional argument.
```java
configure("MyConfig",
          color: 0x7393B3,
          showName: false,
          autoSave: false);
```
                      
## Binary compatible

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

Additionally, code compiled without `manifold-params` can still benefit from code compiled with optional parameters. In
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
     implementation 'systems.manifold:manifold-params-rt:2024.1.49'
     testImplementation 'junit:junit:4.12'
     // Add manifold to -processorpath for javac
     annotationProcessor 'systems.manifold:manifold-params:2024.1.49'
     testAnnotationProcessor 'systems.manifold:manifold-params:2024.1.49'
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
        <manifold.version>2024.1.49</manifold.version>
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
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params/2024.1.49/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params/2024.1.49)

`manifold-params-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-params-rt/2024.1.49/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-params-rt/2024.1.49)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
