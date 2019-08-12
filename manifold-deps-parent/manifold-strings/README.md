# Manifold : String Templates (aka String Interpolation)

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

Finally, you can use the `$` literally and bypass string templates using standard Java character escape syntax:
```java
String verbatim = "It is \$hour o'clock"; // prints "It is $hour o'clock"
```
Or, if you prefer, you can use template syntax:
```java
String verbatim = "It is ${'$'}hour o'clock"; // prints "It is $hour o'clock"
``` 

Template **_files_** are much more powerful and are documented in project [ManTL](http://manifold.systems/manifold-templates.html).

> Clone the [Manifold sample Web App project](https://github.com/manifold-systems/manifold-sample-web-app) to quickly
begin experimenting with ManTL templates using the Manifold IntelliJ plugin.

# IDE Support 

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ
via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access symbols within a string literal. Jump to symbol references, perform rename refactors,
etc.

# Building

## Building this project

The `manifold-strings` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-strings` dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 12.

Here are some sample build configurations references.

>Note you can replace the `manifold-strings` dependency with **`manifold-all`** as a quick way to gain access to all of
Manifold's features.

## Gradle

### Java 8
Here is a sample `build.gradle` file using `manifold-strings` with **Java 8**:
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 1.8
sourceCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    compile group: 'systems.manifold', name: 'manifold-strings', version: '2019.1.10'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // tools.jar dependency (for Java 8 only)
    compile files("${System.properties['java.home']}/../lib/tools.jar")
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyStringTemplatesProject'
```

### Java 11+
Here is a sample `build.gradle` file using `manifold-strings` with **Java 11**:
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    mavenCentral()
}

dependencies {
    compile group: 'systems.manifold', name: 'manifold-strings', version: '2019.1.10'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold-strings to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-strings', version: '2019.1.10'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyStringTemplatesProject'
```

## Maven

### Java 8

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
        <manifold.version>2019.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-strings</artifactId>
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
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- Java 8 only, for tools.jar  -->
        <profile>
            <id>internal.tools-jar</id>
            <activation>
                <file>
                    <exists>\${java.home}/../lib/tools.jar</exists>
                </file>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>com.sun</groupId>
                    <artifactId>tools</artifactId>
                    <version>1.8.0</version>
                    <scope>system</scope>
                    <systemPath>\${java.home}/../lib/tools.jar</systemPath>
                </dependency>
              </dependencies>
        </profile>
    </profiles>
</project>
```

### Java 11+
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
        <manifold.version>2019.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-strings</artifactId>
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
                        <!-- Configure manifold plugin -->
                        <arg>-Xplugin:Manifold</arg>
                    </compilerArgs>
                    <!-- Add the processor path for the plugin (required for Java 9+) -->
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

# License

## Open Source
Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

## Commercial
Commercial licenses for this work are available. These replace the above ASL 2.0 and offer 
limited warranties, support, maintenance, and commercial server integrations.

For more information, please visit: http://manifold.systems//licenses

Contact: admin@manifold.systems

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](https://www.linkedin.com/in/scott-mckinney-52295625/) - *Manifold creator, principal engineer, and founder of [Manifold Systems, LLC](http://manifold.systems)*


