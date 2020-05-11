# Manifold : Properties

Many Java applications incorporate [properties resource files](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html)
(*.properties files) as a means of separating configurable text from code:

`resources/abc/MyProperties.properties`:
```properties
my.chocolate = Chocolate
my.chocolate.dark = Dark Chocolate
my.chocolate.milk = Milk Chocolate
```

Unfortunately access to these files requires boilerplate library code and the use
of hard-coded strings:
```java
Properties myProperties = new Properties();
myProperties.load(getClass().getResourceAsStream("/abc/MyProperties.properties"));

println(myProperties.getProperty("my.chocolate.dark"));
```

With the Properties type manifold we can access properties directly using simple, type-safe code:
```java
println(abc.MyProperties.my.chocolate.dark);
```

Behind the scenes the properties type manifold creates a Java class for the properties file, which reflects its
hierarchy of properties.  As you develop your application, changes you make in the file are immediately available in
your code with no user intervention in between -- no code gen files and no compiling between changes.

# IDE Support 

Manifold is best experienced using [IntelliJ IDEA](https://www.jetbrains.com/idea/download).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA directly from IntelliJ
via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample Project](https://github.com/manifold-systems/manifold-sample-project) via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-project.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Use code
completion to conveniently access properties. Make changes to your Properties files and use the changes immediately, no
compilation!  Find usages of any element in your Properties files. Perform rename refactors to quickly and safely make
project-wide changes.

# Setup

## Building this project

The `manifold-properties` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-properties` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions 8 - 13.

>Note you can replace the `manifold-properties` dependency with [`manifold-all`](https://github.com/manifold-systems/manifold/tree/master/manifold-all) as a quick way to gain access to all of
Manifold's features.

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 13), the script takes care of the rest. 
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
    compile group: 'systems.manifold', name: 'manifold-properties', version: '2020.1.10'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-properties', version: '2020.1.10'
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
rootProject.name = 'MyProject'
```

## Maven

### Java 8

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-properties-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Properties App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2020.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-properties</artifactId>
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
</project>
```

### Java 9 or later
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-properties-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Properties App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2020.1.10</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-properties</artifactId>
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
                            <artifactId>manifold-properties</artifactId>
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

* [Scott McKinney](mailto:scott@manifold.systems)
