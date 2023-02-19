# Manifold : String Templates (aka String Interpolation)

# Basics

A **String template** lets you use the `$` character to embed a Java expression directly into a String.  You can use `$`
to embed a simple variable:
```java
int hour = 8;
String time = "It is $hour o'clock";  // prints "It is 8 o'clock"
```
Or you can embed an expression of any complexity in curly braces:
```java
LocalTime localTime = LocalTime.now();
String ltime = "It is ${localTime.getHour()}:${localTime.getMinute()}"; // prints "It is 8:39"
```

# Bypassing String Template Processing

## `@DisableStringLiteralTemplates`

If you need to turn the feature off in specific areas in your code, you can use the `@DisableStringLiteralTemplates` 
annotation to control its use.  You can annotate a class, method, field, or local variable declaration to turn it on 
or off in that scope:
```java
@DisableStringLiteralTemplates // turns off String templating inside this class
public class MyClass
{
  void foo() {
    int hour = 8;
    String time = "It is $hour o'clock";  // prints "It is $hour o'clock"
  }
  
  @DisableStringLiteralTemplates(false) // turns on String templating inside this method
  void bar() {
    int hour = 8;
    String time = "It is $hour o'clock";  // prints "It is 8 o'clock"
  }
}
```

## `ITemplateProcessorGate` SPI

Additionally, you can exclude types from string template processing programmatically by implementing the
[`ITemplateProcessorGate`](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-strings/src/main/java/manifold/strings/api/ITemplateProcessorGate.java)
SPI. This option mostly applies to use-cases where you don't have access to source code you are building e.g., generated
source. Importantly, because the service provider is used at compile-time the provider class implementation must be
precompiled. Typically this means you define the class in a separate module and then make a dependency on that module.
To use the provider you register it in the `META-INF/services` directory in your `resources` path. See the
[string template tests](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings-test-excl)
for an example of this. See the [Java SPI documentation](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
for more details regarding Java services.
 
## Escaping '$'

You can use the `$` literally and bypass string templates using standard Java character escape syntax:
```java
String verbatim = "It is \$hour o'clock"; // prints "It is $hour o'clock"
```
Or, if you prefer, you can use template syntax:
```java
String verbatim = "It is ${'$'}hour o'clock"; // prints "It is $hour o'clock"
``` 

## Disabling simple `$foo` expressions
           
Finally, you can disable simple `$foo` syntax in favor is strictly using `${foo}` syntax with the javac
`-Amanifold.strings.simple.disabled=true` command line option. As such, you can still use string templates with the
`${foo}` syntax, while simple usages of `$` are excluded from processing.
                                                        
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
. . .
    <compilerArgs>
      <arg>-Xplugin:Manifold</arg>
      <arg>-Amanifold.strings.simple.disabled=true</arg>
    </compilerArgs>
. . .
```
```java
int hour = 5;
out.println("It is $hour o'clock"); // prints "It is $hour o'clock"
out.println("It is ${hour} o'clock"); // prints "It is 5 o'clock"
```

# Template Files

Template **_files_** are much more powerful and are documented in project [ManTL](http://manifold.systems/manifold-templates.html).

> Clone the [Manifold sample Web App project](https://github.com/manifold-systems/manifold-sample-web-app) to quickly
begin experimenting with ManTL templates using the Manifold IntelliJ plugin.

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

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access symbols within a string literal. Jump to symbol references, perform rename refactors,
etc.

# Setup

## Building this project

The `manifold-strings` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-strings` dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 19.

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
    compileOnly 'systems.manifold:manifold-rt:2023.1.3'
    testImplementation 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor 'systems.manifold:manifold-strings:2023.1.3'
    testAnnotationProcessor 'systems.manifold:manifold-strings:2023.1.3'
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
If you are using `module-info` files, you must declare a *static* dependency on `manifold`.
```java
module MyProject {
    requires static manifold;
}
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-strings-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My StringTemplates App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>

    <dependencies>
        <dependency>
            <!-- Necessary only during compile-time to resolve generated source-level annotations -->
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-rt</artifactId>
            <version>${manifold.version}</version>
            <scope>provided</scope> <!-- dependency is only applied during compile-time for manifold-strings -->
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
                        <!-- Configure manifold plugin -->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-strings</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
If you are using `module-info` files, you must declare a *static* dependency on `manifold`.
```java
module my.strings.app {
    requires static manifold;
}
```

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)


