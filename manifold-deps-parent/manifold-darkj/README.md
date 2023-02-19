# Manifold : Dark Java

## Table of Contents
* [Usage](#usage)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## Usage

A Dark Java file is a Java *resource* source file with the extension, `.darkj`. A Dark Java file is "dark" because the
compiler does not produce a `.class` file for it.  The basic properties of a type manifold are what makes Dark Java
interesting:
* You can reference a Dark Java class as a normal Java class directly from any `.java` source file in your project
* The JRE compiles and loads a Dark Java class *dynamically* at *runtime* only if and when it is first used, otherwise
it is ignored 
* Despite the absence of a `.class` file, you can access a Dark Java class using reflection
 
To illustrate, the following example is statically compiled with Java 8; it runs with Java 8 or later.  
It demonstrates how you can target Java versions dynamically -- Java 8 with `Foo8` and Java 9+ with `Foo9` using an
interface to abstract their use:

```java
public interface Foo {
  Set<Integer> makeSet();
  
  static Foo create() {
    if(JreUtil.isJava8()) {
      return (Foo)Class.forName("abc.Foo8").newInstance();
    }
    return (Foo)Class.forName("abc.Foo9").newInstance();
  }
}

// File: Foo8.darkj
public class Foo8 implements Foo {
  @Override
  public Set<Integer> makeSet() {
    Set<Integer> set = new HashSet<>();
    set.add(1);
    set.add(2);
    set.add(3);
    return Collections.unmodifiableSet(set);
  }
}

// File: Foo9.darkj
public class Foo9 implements Foo {
  @Override
  public Set<Integer> makeSet() {
    return Set.of(1, 2, 3);
  }
}
```

When run in Java 8 the `Main` class uses `Foo8`, when run in Java 9 or later it uses `Foo9`:

```java
public class Main {
  public static void main( String[] args ) {
    Foo foo = Foo.create();
    Set<Integer> set = foo.makeSet();
  }
}
```

This basic interface factory pattern can be used anywhere late-bound compilation is desirable.
 
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
completion to conveniently access Dark Java. Make changes to your Dark Java files and use the changes immediately, no
compilation!  Find usages of any element in your Dark Java files. Perform rename refactors to quickly and safely make
project-wide changes.

# Setup

## Building this project

The `manifold-darkj` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-darkj` dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 19.

>Note, since Dark Java is a dynamic compilation feature, you must include Manifold dependencies in both compile-time
>and runtime. As a consequence, the Manifold runtime compilation services add a bit of overhead in terms of
>application initialization time and footprint.  
 
## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

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
    implementation group: 'systems.manifold', name: 'manifold-darkj', version: '2023.1.3'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    if (JavaVersion.current() == JavaVersion.VERSION_1_8) {
        // tools.jar dependency (for Java 8 only), primarily to support manifold dynamic compilation with Dark Java.
        // If you are *not* using Dark Java, you **don't** need tools.jar
        compile files( "${System.properties['java.home']}/../lib/tools.jar" )
    }
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-darkj', version: '2023.1.3'
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

tasks.compileJava {
    classpath += files(sourceSets.main.output.resourcesDir) //adds build/resources/main to javac's classpath
    dependsOn processResources
}
tasks.compileTestJava {
    classpath += files(sourceSets.test.output.resourcesDir) //adds build/resources/test to test javac's classpath
    dependsOn processTestResources
}
```
Use with accompanying `settings.gradle` file:
```groovy
rootProject.name = 'MyDarkJavaProject'
```

## Maven

### Java 8

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-darkj-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Dark Java App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-darkj</artifactId>
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
                    <source>8</source>
                    <target>8</target>
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

### Java 9 or later
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-darkj-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Dark Java App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-darkj</artifactId>
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
                            <artifactId>manifold-darkj</artifactId>
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

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
