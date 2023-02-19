# Manifold : CSV (comma-separated values)

>Warning: **Experimental Feature**

Manifold plugs into the Java compiler enabling you to use CSV data seamlessly -- CSV files are types. You use CSV
directly and type-safely without a code generator or extra build steps.

```java
// Type-safely use resource file "resources/com/example/Sales.csv" without a code generator in your build
import com.example.Sales;

Sales sales = Sales.fromSource();
for(Sales.SalesItem item: sales) {
  out.println(item.getCustomer());
  ...
}
```

>#### CSV, JSON, XML, and YAML are _Interchangeable_
>You can use CSV, JSON, XML, and YAML interchangeably, as such please refer to the [**JSON and JSON Schema**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
>project reference.

## Table of Contents
* [Overview](#overview)
* [Naming](#naming)
* [Header, separators, spaces, etc.](#header-separators-spaces-etc)
* [Type Inference](#type-inference)
* [Fluent API](#fluent-api)
* [Creating & Building JSON](#creating--building-csv)
* [Loading CSV](#loading-csv)
* [Writing CSV](#writing-csv)
* [Copying CSV](#copying-csv)
* [Using CSV with JSON Schema](#using-csv-with-json-schema)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## Overview
The CSV type manifold provides comprehensive support for CSV resource files, supporting extensions `csv`, `tsv`, `psv`, 
`tab`. You can define a CSV API using a sample CSV resource file. You can also define a [JSON Schema](https://json-schema.org/) version 4 or 
later and use that as the schema to provide extra type information for your CSV files. Your CSV resource files serve as the **single source of truth**
regarding CSV data and API.  You use CSV-expressed types *directly* in your code without maintaining a separate set of classes
or wedging a code generator into your build.

Here is a sample CSV file `resources/com/example/Sales.csv`:
```csv
Customer,Date,Retail,Discount,State,Invoice
Dunedin Glassworks,2019-05-22,55.49,,FL,110020
Lotta Taxes Co.,2019-05-25,395.89,0.05,CA,110021
"Palm Harbor Tuning, Inc.",2019-05-26,202.18,,FL,110022
Milesov Firewood,2019-06-01,1396.61,,CA,110023
``` 

## Naming

Most type manifolds, including the CSV, XML, JSON, & YAML manifolds, follow the Java naming convention where a type name is based on the
resource file name relative to its location in the classpath or resource path. Thus the CSV resource file `resources/com/example/Sales.csv`
has the Java type `com.example.Sales`.

All top-level CSV types extend the Java `List` type, where the items in the list are rows from the CSV file.  For
instance, the `Sales` type is an interface extending `List<SalesItem>` where `SalesItem` is an interface having get/set
methods matching the CSV header row, if provided. 

## Header, separators, spaces, etc.

Although a standard for the CSV format exists with [RFC 4180](https://tools.ietf.org/html/rfc4180), there are many
differing versions of the format in use. Additionally, some aspects of CSV are unspecific and make parsing CSV difficult
without supplemental information and/or applying heuristics on samples of the data. The CSV manifold infers much of this
information via sampling.

### Header row 

While it is impossible to detect a header row with 100% accuracy, the heuristics the CSV manifold uses work with most
types of CSV data.

### Separators

The CSV manifold supports comma, semicolon, colon, and pipe (`, ; : |`) as field separators. Just as with header
sampling, the separator used for a given file is inferred by finding patterns in the data.

### Leading/trailing spaces  

Some dialects of CSV include leading and trailing spaces in unquoted data, others do not. The CSV manifold samples the
data to find patterns with usage of spaces and infers whether or not spaces are significant.  
                   
Note quoted data always includes leading and trailing spaces in the data.

### Empty fields

An empty field value is specified using a zero-length value. Both quoted and unquoted forms are supported.

### Empty lines

Since many CSV dialects support empty lines (lines beginning with a linebreak character), the CSV manifold ignores
empty lines.
                   
### Quotes

Many CSV dialects support quoted data.  The CSV manifold considers data that begins and ends with the double quote
character as quoted data where the data between the quotes is taken as-is, minus the enclosing quotes. It is useful as
a means to directly include the separator character and linebreak characters in data. Note unquoted data may treat the
double quote character normally, while quoted data must escape it with another quote (`""`) to distinguish it from a
closing quote.   

## Type Inference

Field types are inferred by finding patterns in samples of columnar data. Fields can have the following basic types:
* `String`
* `Integer`
* `Long`
* `Double`
* `Boolean`
* `BigInteger`
* `BigDecimal`
* `LocalDateTime`
* `LocalDate`
* `LocalTime`   

For direct control over data types and formatting you can provide a *JSON Schema* resource file for the CSV format. As
such you can use additional types such as enum classes, provide custom data types and formats, and verify the CSV data
conforms to schema constraints.
```java
// A type-safe JSON Schema resource file "resources/com/example/MySalesSchema.json" modeling sales data for CSV files
import com.example.MySalesSchema;
...
MySalesSchema sales = MySalesSchema.load().fromCsvFile("/path/to/sales.csv");
```
 
## Fluent API

CSV types are defined as a set of fluent _interface_ APIs.  For example, the `Sales` CSV type is an interface and
provides type-safe methods to:
* **create** a `Sales` list or `SalesItem`
* **build** a `SalesItem`
* **modify** properties of a `SalesItem`  
* **load** a `Sales` list from a string, a CSV file, or a URL using HTTP GET
* **write** a `Sales` and `SalesItem` as formatted CSV, XML, JSON, & YAML
* **copy** a `Sales` and `SalesItem`
* **cast** to `SalesItem` from any structurally compatible type including `Map`s, all *without proxies*

## Creating & Building CSV
You create an instance of a CSV type using either the `create()` method or the `builder()` method. Note if you want to
load data from preexisting CSV files or even load directly from the sample data you can use the `load()` method or
the `fromSource()` method, discussed later in this document. 

The `create()` method defines parameters matching the `required` properties defined in the JSON Schema, if the type is
plain CSV or no `required` properties are specified, `create()` has no parameters.

Since `Sales` is a plain CSV file, as opposed to a JSON Schema structured CSV file, you can create an empty
instance of `Sales` with `create()` and then modify it using _setter_ methods to change properties:
```java
import com.example.Sales;
...
Sales sales = Sales.create();
sales.setCustomer( "" );
```

You can use `builder()` to fluently build a new `SalesItem` instance:
```java
var salesItem = Sales.SalesItem.builder()
  .withCustomer("Purdue University")
  .withDate(LocalDate.of(2019,11,9))
  .withRetail(79.99)
  .withDiscount(0.15)
  .withState("IN")
  .withInvoice(177192)
  .build();
sales.add(salesItem);
```

You can initialize several properties in a chain of `with` calls in the builder. This saves a bit of typing with
heavier APIs.  Call the `build()` method to construct the type.

> Note if using JSON Schema `with` methods also serve as a means to initialize values for `readOnly` properties.

## Loading CSV
In addition to creating an object from scratch with `create()` and `build()` you can also load an instance from 
a variety of existing sources using `fromSource()` and `load()`.

You can load the contents of the file directly using `fromSource()`.
```java
// Load from the contents of the Sales type's origin file 
Sales sales = Sales.fromSource();
for(Sales.SalesItem item: sales) {
  out.println(item.getCustomer());
  ...
}
```
You can load a `Sales` instance from a CSV, JSON, or YAML String:
```java
// From a JSON String
Sales sales = Sales.load().fromJson("..."); 
```
Load from a file:
```java
// From a CSV file
Sales sales = Sales.load().fromCsvFile("/path/to/WinterSales.csv");

// From an JSON file
Sales sales = Sales.load().fromJsonFile("/path/to/Sales.json");
```
Invoke a REST API to load a `Sales` using HTTP GET:
```java
// From HTTP GET
Sales sales = Sales.load().fromJsonUrl("http://api.example.com/sales/$Id");
```

## Writing CSV
An instance of a CSV API object can be written as formatted text with `write()`:
* `toCsv()` - produces a CSV formatted String
* `toJson()` - produces a JSON formatted String
* `toXml()` - produces a XML formatted String
* `toYaml()` - produces a YAML formatted String

The following example produces a JSON formatted string:
```java
String json = sales.write().toJson();
System.out.println(json);
```

## Copying CSV
Use the `copy()` method to make a deep copy of any CSV API object:
```java
SalesItem salesItem = . . .;
...
SalesItem copy = salesItem.copy();
```
Alternatively, you can use the `copier()` static method for a richer set of features:
```java
SalesItem copy = SalesItem.copier(salesItem).withDiscount(0.25).copy();
```
`copier()` is a lot like `builder()` but lets you start with an already built object you can modify.  Also like
`builder()` it maintains the integrity of the schema's declared mutability -- you can't change
`readOnly` fields after the `copy()` method constructs the object.

# Using CSV with JSON Schema

You can use CSV, JSON, and YAML interchangeably using the JSON manifold's universal JSON API. This means you can also
use CSV with any JSON Schema API or REST API.  As such please refer to the [**JSON and JSON Schema**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
project reference regarding API usage specific to JSON Schema.


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

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity.

# Setup

## Building this project

The `manifold-csv` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-csv` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions
8 - 19.

This project consists of two modules:
* `manifold-csv`
* `manifold-csv-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-csv-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-csv` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

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
    implementation 'systems.manifold:manifold-csv-rt:2023.1.3'
    testCompile 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-csv', version: '2023.1.3'
    testAnnotationProcessor group: 'systems.manifold', name: 'manifold-csv', version: '2023.1.3'
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
<?csv version="1.0" encoding="UTF-8"?>
<project csvns="http://maven.apache.org/POM/4.0.0" csvns:xsi="http://www.w3.org/2001/CSVSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-csv-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Csv App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-csv-rt</artifactId>
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
                    <!-- Add the processor path for the plugin -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>systems.manifold</groupId>
                            <artifactId>manifold-csv</artifactId>
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

`manifold-csv`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-csv/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-csv/2023.1.3)

`manifold-csv-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-csv-rt/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-csv-rt/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
