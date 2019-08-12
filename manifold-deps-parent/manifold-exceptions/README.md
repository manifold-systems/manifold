# Manifold : Exceptions

Simply add the `manifold-exceptions` dependency to your project. Now checked exceptions behave like unchecked
exceptions!  No more compiler errors, no more unintentional exception swallowing, no more boilerplate
try/catch/wrap/rethrow nonsense. This is how most modern JVM languages behave, now you have the option to make Java do
the same.

## Table of Contents
* [No More Catch-n-Wrap](#no-more-catch-n-wrap)
* [Lambdas](#lambdas)
* [IDE Support](#ide-support)
* [Building](#building)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## No More Catch-n-Wrap
The vast majority of checked exceptions go unhandled, instead they are caught, wrapped in unchecked exceptions, and
rethrown like this:
```java
URL url;
try {
  url = new URL("http://manifold.systems");
}
catch(MalformedURLException e) {
  throw new RuntimeException(e);
}
process(url);
```
This code alone explains why the designers of modern languages such as Scala, Kotlin, and others chose not to follow
Java's example.  The `exceptions` plugin option provides you with the same choice.  With it enabled you can write the
same code like this:
```java
process(new URL("http://manifold.systems"));
```
Sells itself.

## Lambdas
Perhaps the most offensive checked exception use-cases involve lambdas:
```java
List<String> strings = ...;
List<URL> urls = list
  .map(URL::new) // Unhandled exception error: MalformedURLException
  .collect(Collectors.toList());
```
The checked exception prevents concise lambda usage here.  With Manifold, however, you are free to write code as you
like:
```java
List<String> strings = ...;
List<URL> urls = list
  .map(URL::new) // Mmm, life is good
  .collect(Collectors.toList());
```

# IDE Support 

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. With the plugin
installed IntelliJ is aware of all things Manifold, including checked exception handling e.g., the editor won't complain
about checked exceptions.

# Building

## Building this project

The `manifold-exceptions` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-exceptions` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 12.

Here are some sample build configurations references.

>Note you can replace the `manifold-exceptions` dependency with **`manifold-all`** as a quick way to gain access to all
of Manifold's features.

## Gradle

### Java 8
Here is a sample `build.gradle` file using `manifold-exceptions` with **Java 8**:
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
    compile group: 'systems.manifold', name: 'manifold-exceptions', version: '2019.1.10'
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
Here is a sample `build.gradle` file using `manifold-exceptions` with **Java 11**:
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
    compile group: 'systems.manifold', name: 'manifold-exceptions', version: '2019.1.10'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold-exceptions to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-exceptions', version: '2019.1.10'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '-Xplugin:Manifold'
    options.fork = true
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyExceptionsProject'
```

## Maven

### Java 8

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-exceptions-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Exceptions App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-exceptions</artifactId>
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
    <artifactId>my-exceptions-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Exceptions App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-exceptions</artifactId>
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
                    <!-- Add the processor path for the plugin (required for Java 9+) -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-exceptions</artifactId>
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
