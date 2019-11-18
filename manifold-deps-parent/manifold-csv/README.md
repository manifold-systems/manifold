# Manifold : CSV (comma-separated values)

>Warning: **Experimental Feature**

Manifold plugs into the Java compiler enabling you to use CSV data seamlessly -- CSV files are types. You use CSV
directly and type-safely without a code generator or extra build steps.


>#### CSV, JSON, XML, and YAML are _Interchangeable_
>You can use CSV, JSON, XML, and YAML interchangeably, as such please refer to the [**JSON and JSON Schema**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
>project reference. _All_ that applies to JSON applies to XML.

## Table of Contents
* [Overview](#overview)
* [Naming](#naming)
* [Fluent API](#fluent-api)
* [Creating & Building JSON](#creating--building-csv)
* [Loading CSV](#loading-csv)
* [Request REST API services](#request-rest-api-services)
* [Writing CSV](#writing-csv)
* [Copying CSV](#copying-csv)
* [Using CSV with JSON Schema](#using-csv-with-json-schema)
* [IDE Support](#ide-support)
* [Building](#building)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

## Overview
The CSV type manifold provides comprehensive support for CSV resource files, supporting extensions `csv`, `tsv`, `psv`, 
tab`). You can define a CSV API using a sample CSV resource file. You can also define a [JSON Schema](https://json-schema.org/) version 4 or 
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

Although a standard for the CSV format exists with [RFC 4180](https://tools.ietf.org/html/rfc4180), there a many
differing versions of the format in use. Additionally, some aspects of CSV are unspecific and make parsing CSV difficult
without supplemental information and/or applying heuristics on samples of the data. The CSV manifold infers much of this
information vai sampling.

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

Since most CSV formats support double quoted data -- data that begins and ends with the double quote character. It is 
used as a means to include the separator character and linebreak characters directly in data, the CSV manifold supports
this feature. Note unquoted data may treat the double quote character normally, while quoted data must escape it using
pair of double quotes (`""`).  

## Fluent API

CSV types are defined as a set of fluent _interface_ APIs.  For example, the `Sales` CSV type is an interface and
provides type-safe methods to:
* **create** a `Sales` list
* **build** a `SalesItem`
* **modify** properties of a `SalesItem`  
* **load** a `Sales` list from a string, a CSV file, or a URL using HTTP GET
* **request** Web service operations using HTTP GET, POST, PUT, PATCH, & DELETE
* **write** a `Sales` list as formatted CSV, XML, JSON, & YAML
* **copy** a `Sales`
* **cast** to `Sales` from any structurally compatible type including `Map`s, all *without proxies*

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

Alternatively, you can use `builder()` to fluently build a new instance:
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
Sales sales = Sales.load().fromJsonUrl("http://api.example.com/sales/$salesId");
```

## Request REST API services
Use the `request()` static method to conveniently navigate an HTTP REST API with GET, POST, PUT, PATCH, & DELETE:
```java
String id = "2019.q3";
Sales sales = Sales.request("http://api.example.com/sales").getOne("/$id");
```
The `request()` methods provides support for all basic REST API client usage:
```java
Requester<Sales> req = Sales.request("http://api.example.com/sales");

// Get all Sales via HTTP GET
List<Sales> sales = req.getMany();

// Add a SalesItem with HTTP POST
var salesItem = Sales.SalesItem.builder()
  .withCustomer("Forest Charcoal, Inc.")
  .withDiscount(0.05) 
  . . .
req.postOne(salesItem);

// Get a SalesItem with HTTP GET
String id = salesItem.getId();
sales = req.getOne("/$id");

// Update a Sales with HTTP PUT
sales.getStore().setName("Valley #2");
req.putOne("/$id", sales);

// Delete a Sales with HTTP DELETE
req.delete("/$id");
```

## Writing CSV
An instance of a CSV API object can be written as formatted text with `write()`:
* `toCsv()` - produces a CSV formatted String
* `toJson()` - produces a JSON formatted String
* `toXml()` - produces a XML formatted String
* `toYaml()` - produces a YAML formatted String

The following example produces a JSON formatted string:
```java
Sales sales = Sales.builder()
  .withSeason("Fall")
  .withStore(Store.builder() 
  . . .
String json = sales.write().toJson();
System.out.println(json);
```
Output:
```json
{
  "ProductListing": {
    "season": "Fall",
    "Store": {
      "name": "Valley #2",
      "Address": {
        "city": "Cupertino", "state": "CA", "postal": "95014",
        "Line": [
          {"order": "1", "textContent": "1101 Broadway Dr"},
          {"order": "2", "textContent": "Suite #123"}
        ]
      }
    },
    "Product": [
      {
        "department": "Men's", "brand": "Joe's", "price": "65.00",
        "description": "Joe\u2019s\u00ae 430\u2122 Athletic Cut Jeans",
        "Size": {"waste": "26-42", "inseam": "28-38", "cut": "Athletic"}
      },
      {
        "department": "Men's", "brand": "Squarepants", "price": "35.00",
        "description": "Squarepants\u00ae Unround\u2122 Pants",
        "Size": {"waste": "25-49", "inseam": "25-36", "cut": "Athletic"}
      },
      {
        "department": "Jewelry", "brand": "RiteTwice", "price": "100.00",
        "description": "RiteTwice\u00ae The 5 o'clock watch",
        "Size": {"wrist": "XS-XL"}
      }
    ]
  }
}
```

## Copying CSV
Use the `copy()` method to make a deep copy of any CSV API object:
```java
Sales sales = . . .;
...
Sales copy = sales.copy();
```
Alternatively, you can use the `copier()` static method for a richer set of features:
```java
Sales copy = Sales.copier(sales).withProductListing(. . .).copy();
```
`copier()` is a lot like `builder()` but lets you start with an already built object you can modify.  Also like
`builder()` it maintains the integrity of the schema's declared mutability -- you can't change
`readOnly` fields after the `copy()` method constructs the object.

# Using CSV with JSON Schema

You can use CSV, JSON, and YAML interchangeably, via the universal JSON API. This means you can also use CSV with any
JSON Schema API.  You can also define a JSON Schema API using CSV.  As such please refer to the
[**JSON and JSON Schema**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
project reference regarding API usage specific to JSON Schema. _All_ that applies to JSON Schema applies to CSV.



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

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity.

# Building

## Building this project

The `manifold-csv` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold-csv` dependency works with all build tooling, including Maven and Gradle. It also works with Java versions 8 - 13.

Here are some sample build configurations references.

>Note you can replace the `manifold-csv` dependency with [`manifold-all`](https://github.com/manifold-systems/manifold/tree/master/manifold-all) as a quick way to gain access to all of
Manifold's features.

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
    compile group: 'systems.manifold', name: 'manifold-csv', version: '2019.1.28'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold-csv', version: '2019.1.28'
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

```csv
<?csv version="1.0" encoding="UTF-8"?>
<project csvns="http://maven.apache.org/POM/4.0.0" csvns:xsi="http://www.w3.org/2001/CSVSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-csv-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Csv App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.28</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-csv</artifactId>
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
```csv
<?csv version="1.0" encoding="UTF-8"?>
<project csvns="http://maven.apache.org/POM/4.0.0" csvns:xsi="http://www.w3.org/2001/CSVSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-csv-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Csv App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.28</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-csv</artifactId>
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
