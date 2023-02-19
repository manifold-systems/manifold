# Java Preprocessor

## Table of Contents
* [Overview](#overview)
* [Directives](#directives)
* [Symbols](#symbols)
* [Dumping source](#dumping-source)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## Overview

The Java Preprocessor is designed exclusively for *conditional compilation* of Java source code. It is directly
integrated into the Java compiler via the Javac _Plugin_ API. Unlike conventional preprocessors it does *not* incur
separate build steps or additional file I/O, instead it directly contributes to the compilation pipeline.

<p><img src="http://manifold.systems/images/compilerflow.png" alt="javac" width="60%" height="60%"/></p>

The preprocessor offers a simple and convenient way to support multiple build targets with a single codebase.  It
provides advanced features such as tiered symbol definition via `build.properties` files, `-Akey[=value]` compiler
arguments, and environment settings symbols such as `JAVA_9_OR_LATER` and `JPMS_NAMED`. Additionally, the preprocessor
supports Android build variant symbols including `DEBUG`, `BUILD_TYPE`, and `FLAVOR`. The preprocessor is also
fully integrated into IntelliJ IDEA using the [Manifold](https://plugins.jetbrains.com/plugin/10057-manifold) plugin:  

[![preprocessor](http://manifold.systems/images/preprocessor_slide_1_play.png)](http://manifold.systems/images/preprocessor.mp4)

## Directives

The Manifold preprocessor uses familiar directives to conditionally filter source code before it is parsed.  The
preprocessor supports the following directives:

* [`#define`](#define)
* [`#undef`](#undef)
* [`#if`](#if)
* [`#elif`](#elif)
* [`#else`](#else)
* [`#endif`](#endif)
* [`#error`](#error) 
* [`#warning`](#warning)

>Note the nomenclature is borrowed from the C-family of preprocessors for the sake of familiarity.
 
### `#define`
Use `#define` to define a symbol.  When a symbol evaluates it is either `true` or `false`, it is `true` if and only if
it is defined.  You use symbols in expressions as conditions for compilation with `#if` and `#elif` directives. 
 
The preprocessor's symbols are not accessible to Java code, likewise variables in Java code are not accessible to the
preprocessor. This means symbols specified with `#define` never conflict with fields or variables of the same name.

The effects of `#define` are limited to the file scope, as such symbols defined in one file are not accessible to others.

`#define` directives can appear anywhere in a file following the `package` statement.

```java
package com.example;

#define EXPERIMENTAL

import java.math.BigDecimal;
#if EXPERIMENTAL
import com.example.features.NewFeature;
#endif

public class MyClass #if EXPERIMENTAL implements NewFeature #endif {
  ...
#if EXPERIMENTAL
  @Override
  public void newFeatureMethod() {
    ...
  }
#endif
}
``` 
  
>Note additional symbols are available to the preprocessor to access JVM and compiler settings as well as custom
build properties. These are covered below in the [Symbols](#symbols) section.

### `#undef`

Use `#undef` to *undefine* a symbol so that when evaluated, such as in an `#if` expression, its value is `false`.

A symbol can be defined either with the `#define` directive or other means including `-Akey[=value]` compiler arguments,
`build.properties` files, and environment settings such as the Java source mode (see [Symbols](#symbols)).
Regardless of a symbol's origin or scope, the `#undef` can be used to undefine it, however its effects are limited to
the scope of the containing file.

```csharp
package com.example;
  
#define FOO
#undef FOO
 
public class MyClass {  
  public static void main(Stirng[] args) {  
#if FOO  
    System.out.println("FOO is defined");
#else
    System.out.println("FOO is not defined");  
#endif  
  }  
} 
```

### `#if`

Code between `#if` and `#endif` directives is included for compilation based on the *expression* used with `#if` -- if
the expression is `true` the code compiles. The expression always evaluates as either `true` or `false`.  If the
expression is a symbol, such as one defined with `#define`, the symbol evaluates to `true` if the symbol is accessible
and has not been undefined with `#undef`.

This example uses `#if` to conditionally compile code based on whether `JAVA_8` is defined: 

```csharp
#if JAVA_8
  out.println("Compiled with Java source version 8");
#endif
``` 

The full structure of an `#if` directive looks like this:
```csharp
#if <expression>
<code>
#elif <expression>
<code>
#else
<code>
#endif
```
Details concerning [`#elif`](#elif), [`#else`](#else), and [`#endif`](#endif) directives are covered in separate sections below.
 
You can use more than symbols with `#if`. Condition expressions can have operators `&&` (and), `||` (or), and `!` (not)
to evaluate whether multiple symbols have been defined. You can also group symbols and operators with parentheses.

Expressions can also test for equality with `==` and `!=`. Two expressions are equal if:
1. They are both undefined *or*
2. They are both defined *and* their *string values* are the same

The string value of a symbol defined with `#define` is the empty string `""`. Symbols defined with `build.properties`
files, `-Akey[=value]` command line arguments, and SymbolProvider implementations such as Android build variant symbols
may have String values.

>Note it is impossible for a symbol to have a `null` value. When referenced in an equality expression, if a symbol
is not assigned a value, its value is the empty string `""`.

Additionally, you can compare numeric symbol values using relational expressions with `>`, `>=`, `<`, `<=`. If either of
the operands is not coercible to a number value, a compile-time error results indicating the value is not allowed in the
expression.
 
### `#elif`

Use `#elif` to divide an `#if` directive into multiple conditions. The first `true` condition in the series of `#if`/`#elif`
directives determines which of the directives executes:

```csharp
public class MyClass {}
  @Override 
#if JAVA_8
  public void myJava8Method() {}
#elif JAVA_9
  public void myJava9Method() {}
#elif JAVA_10
  public void myJava10Method() {}
#else
  public void myJava11Method() {}
#endif  
``` 

Here if compiling with Java 10 source compatibility mode, only `myJava10Method()` will be compiled.
  
Note `#elif` is a more convenient and easier to read alternative to writing nested `#if` directives in `#else`:
```csharp
#if FOO
  out.println("FOO");
#else
  #if BAR
  out.println("BAR");  
  #endif
#endif
```
It's easier on the eye to use `#elif`:
```csharp
#if FOO
  out.println("FOO");
#elif BAR
  out.println("BAR");  
#endif
```
  
### `#else`

If none of the conditions are `true` for `#if` and `#elif` directives, the code between `#else` and `#endif` is
compiled:

```csharp
#if DEV
  out.println("DEV mode");
#else
  out.println("Customer mode");
#endif  
``` 

### `#endif`

The `#endif` directive marks the end of the series of directives beginning with `#if`.  See the [`#if`](#if) directive
for more details and examples.
   
>Note unlike conventional preprocessors, you can place more than one directive on the same line.  Here the `#if` and `#endif`
>directives share the same line to conditionally implement an interface:
>```csharp
>public class MyClass #if(JAVA_8) implements MyInterface #endif {
>  ...
>}
>```
   
### `#error`

Use the `#error` directive to generate a compiler error from a specific location in your code:

```csharp
#if MODE_A
  out.println("MODE A");
#elif MODE_B
  out.println("MODE B");
#elif MODE_C
  out.println("MODE C");
#else
  #error "Expecting a MODE to be defined"
#endif
```

You can also generate a compiler warning with the [`#warning`](#warning) directive.
 
### `#warning`

Use the `#warning` directive to generate a compiler warning from a specific location in your code:

```csharp
#if MODE_A
  out.println("MODE A");
#elif MODE_B
  out.println("MODE B");
#else
  #warning "No MODE defined, defaulting to MODE_C"
#endif
```

You can also generate a compiler error with the [`#error`](#error) directive.


## Symbols
Similar to a variable in Java, a preprocessor symbol has a name and an optional value. There are five ways a symbol can
be defined:
1. Locally in the source file via `#define`
2. Using a `build.properties` file in the directory ancestry beginning with the root source directory
3. Using the `-Akey[=value]` option on the javac command line
4. From compiler and JVM environment settings such as Java source version, JPMS mode, operating system, etc.    
5. From custom SymbolProvider SPI implementations such as the Android Studio build variant symbols

Symbol scoping rules model a hierarchy of maps, where symbols are accessed in leaf-first order where the leaf
symbols are controlled by the `#define` and `#undef` directives in the compiling source file.  Parent symbols
correspond with 2 - 5 above.

Note the effects of `#define` and `#undef` are limited to the file scope. This means `#define` symbols are not
available to other files.  Similarly, parent symbols masked with `#undef` are unaffected in other files.

>Note Manifold's preprocessor is designed exclusively for conditional compilation, you can't use `#define` for
constant values or macro substitution as you can with a C/C++ preprocessor.


### `build.properties` files

You can provide global symbols using `build.properties` files placed in the ancestry of directories beginning with a
source root directory.  Although a symbol defined as a property can have a string value, sometimes it is preferable to
design property names to have the value encoded in the name.

Instead of this:

```properties
customer.type = ABC
customer.level = Ultimate
```

Do this:

```properties
CUSTOMER_TYPE_ABC =
CUSTUMER_LEVEL_ULTIMATE =
```

### Symbols as compiler arguments

Similar to `build.properties` you can define symbols on the javac command line via the `-Akey[=value]` option.  For
example:
```
javac -Acustomer.level=Ultimate ...
```
or
```
javac -ACUSTOMER_LEVEL_ULTIMATE ...
```

### Environment settings symbols

You get some symbols for free.  These symbols come from compiler, JVM, and IDE settings.  For instance, the Java source
compatibility mode provided on the command line via `-source` or inherited from IDE settings translates to symbols
having the following format:
```java
JAVA_N
JAVA_N_OR_LATER
```
Where `N` is the source version obtained from the environment.
  
Symbols for the JPMS mode are defined as:
```java
JPMS_NONE     // If compiling with Java 8, or Java 8 source compatibility
JPMS_UNNAMED  // If compiling with Java 9 or later and no module-info.java file is defined
JPMS_NAMED    // If compiling with Java 9 or later and a module-info.java file is defined
```  

Symbols for the operating system on which javac is running:
```java
OS_FREE_BSD
OS_LINUX
OS_MAC
OS_SOLARIS
OS_UNIX // Same as !OS_WINDOWS
OS_WINDOWS
```

The O/S architecture:
```java
ARCH_32
ARCH_64
```

### SymbolProvider SPI implementations

Implement the `SymbolProvider` SPI to augment the environment definitions with your own custom environment symbols.

For instance, the `manifold-preprocessor-android-syms` library implements the service to provide direct access to
Android build variant symbols. You can add the library as a dependency to gain automatic access to familiar Android
`BuildConfig` symbols:
```java
DEBUG
BUILD_TYPE // current build variant  
FLAVOR // flavor of current build variant
etc. 
``` 
See [manifold-preprocessor-android-syms](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor-android-syms) for details.

## Dumping source
                 
The preprocessor integrates directly with the Java parser, as such there are no intermediate files to manage. However,
some tools may need to parse or otherwise analyze processed Java source separate from the Java compiler. In this case
you can use the `manifold.source.target` compiler option to specify a directory where all source is copied as it is
compiled, including processed and generated source from Manifold.

Usage:
```
javac -Amanifold.source.target=<my-directory> ...
```
>Note, you are responsible for managing the directory in your build configuration. For instance, for the "clean" build
>target, it is your responsibility to delete the contents of the directory.
 
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

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. The plugin
fully supports the Manifold Preprocessor. It provides an interactive mode in which you can see the effects of the
directives and symbols you define and use in your code. 

# Setup

## Building this project

The `manifold-preprocessor` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-preprocessor` dependency works with all build tooling, including Maven and Gradle. It supports JDK
versions 8 - 19 and supports `-source` compatibility with any Java version.  Note this dependency is exclusive to
compile-time use, there is no runtime impact.

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

Here is a sample `build.gradle` script using `manifold-preprocessor`. Change `targetCompatibility` and
`sourceCompatibility` to your desired Java version, the script takes care of the rest. 
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
    testCompile 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor 'systems.manifold:manifold-preprocessor:2023.1.3'
    testAnnotationProcessor 'systems.manifold:manifold-preprocessor:2023.1.3'
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
rootProject.name = 'MyPreprocessorProject'
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-preprocessor-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Preprocessor App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>

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
                        <!-- Configure manifold plugin -->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-preprocessor</artifactId>
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

`manifold-preprocessor`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-preprocessor/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-preprocessor/2023.1.3)


# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)