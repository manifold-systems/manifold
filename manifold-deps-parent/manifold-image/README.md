# Manifold : Image

User interfaces frequently use image resource files for one purpose or another.  Java supports most of the popular
formats including png, jpg, gif, and bmp via a collection of utility classes such as `javax.swing.ImageIcon` and
`javax.scene.image.Image`.
  
As with any library, access to the underlying data resource is indirect. Here we manually create an `ImageIcon` with a raw
String naming the image file.  This is error prone because there is no type-safety connecting the String with the file
on disk -- your build process will not catch typos or file rename related errors:

```java
ImageIcon image = new ImageIcon("abc/widget/images/companyLogo.png");
```

Custom library layers often contribute toward image caching and other services:
```java
import abc.widget.util.ImageUtilities;

ImageIcon image = ImageUtilities.getCachedImage("abc/widget/images/companyLogo.png");
render(image);
```

The image manifold eliminates much of this with direct, type-safe access to image resources.
```java
import abc.widget.images.*;

ImageIcon image = companyLogo_png.get();
render(image);
```

All image resources are accessible as classes where each class has the same name as its image file including a suffix
encoding the image extension, this helps distinguish between images of different types sharing a single name.
Additionally image classes are direct subclasses of the familiar `ImageIcon` class to conform with existing frameworks.
As with all type manifolds there are no code gen files or other build steps involved.

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
completion to conveniently access images. Perform rename refactors to quickly and safely make project-wide changes.

# Setup

## Building this project

The `manifold-image` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-image` dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 19.

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).


## Gradle

>Note, if you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

>Note, if you are using **Kotlin**, please see the [Kotlin](http://manifold.systems/kotlin.html) docs.

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
    annotationProcessor 'systems.manifold:manifold-image:2023.1.3'
    testAnnotationProcessor 'systems.manifold:manifold-image:2023.1.3'
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
If you are using `module-info` files, you must declare a *static* dependency on `manifold`.  Additionally, since
`manifold-images` uses Java's `ImageIcon` class, you also need a runtime dependency on `java.desktop`.
```java
module MyProject {
    requires java.desktop;
    requires static manifold;
}
```

## Maven

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-image-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Image App</name>

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
            <scope>provided</scope> <!-- dependency is only applied during compile-time for manifold-image -->
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
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-image</artifactId>
                            <version>${manifold.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
If you are using `module-info` files, you must declare a *static* dependency on `manifold`.  Additionally, since
`manifold-images` uses Java's `ImageIcon` class, you also need a runtime dependency on `java.desktop`.
```java
module my.image.app {
    requires java.desktop;
    requires static manifold;
}
```

# Javadoc

`manifold-image`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-image/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-image/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
