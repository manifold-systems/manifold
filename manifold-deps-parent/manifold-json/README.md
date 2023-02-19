# Manifold : JSON and JSON Schema

[![json](http://manifold.systems/images/json_slide_1.png)](http://manifold.systems/images/json.mp4)

## Table of Contents
* [Overview](#overview)
* [Naming](#naming)
* [Fluent API](#fluent-api)
* [Creating & Building JSON](#creating--building-json)
* [Loading JSON](#loading-json)
* [Request REST API services](#request-rest-api-services)
* [Configuring Proxies](#configuring-proxies)
* [Writing JSON](#writing-json)
* [Copying JSON](#copying-json)
* [Properties Marked `readOnly` or `writeOnly`](#properties-marked-readonly-or-writeonly)
* [Nullable Properties](#nullable-properties)
* ['additionalProperties' and 'patternProperties'](#additionalproperties-and-patternproperties)
* Types
  * [Nested Types](#nested-types)
  * [Format Types](#format-types)
  * [Composition Types with `allOf`](#composition-types-with-allof)
  * [Union Types with `oneOf`/`anyOf`](#union-types-with-oneofanyof)
  * [Interfaces are _Structural_](#interfaces-are-_structural_)
* [Extensions](#extensions)
* [JSON, XML, CSV, & YAML Utilities](#json-xml--yaml-utilities)
* [IDE Support](#ide-support)
* [Setup](#setup)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)

>#### *** JSON, XML, CSV, and YAML are Interchangeable ***
>It is important to note, although the JSON manifold targets JSON and JSON schema files, it equally targets XML and YAML. In fact the
>[XML manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml) and
>[YAML manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml) are just
>thin layers on top of the JSON manifold -- the types the JSON manifold projects from JSON files are the same exact
>types the XML and YAML manifolds project. This means you can use JSON, XML, CSV, and YAML interchangeably. As covered in
>this document you use the same objects to create, build, modify, load, request, write, and copy all three formats.
>```java
>import com.example.MyJson; // types-safely use the JSON resource file: /com/example/MyJson.json
>
>MyJson myJson  = MyJson.fromSource();       // load the data in the file into an instance of MyJson
>String xml = myJson.write().toXml();        // write the JSON as formatted XML
>myJson = (MyJson) Xml.fromXml( xml, true ); // read the object back from XML!
>```
>Indeed, any mention of JSON and `.json` files in this document equally applies to XML and `.xml` files as well as YAML
>and `.yml`/`.yaml` files.

## Overview
The JSON type manifold provides comprehensive support for JSON resource files (extension `.json`).  You can define a 
JSON API with JSON resources consisting of either sample JSON or [JSON Schema](https://json-schema.org/) version 4 or 
later. Your JSON resource files serve as the **single source of truth** regarding JSON APIs.  You use JSON-expressed
types *directly* in your code without maintaining a separate set of classes or wedging a code generator into your build.

> Clone the [Manifold sample REST API project](https://github.com/manifold-systems/manifold-sample-rest-api) to quickly
begin experimenting with a JSON Schema REST API using Manifold.

Here is a simple `User` type defined in `resources/com/example/schemas/User.json` using JSON Schema:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "http://example.com/schemas/User.json",
  "type": "object",
  "definitions": {
    "Gender": {
      "type": "string",
      "enum": ["male", "female"]
    }
  },
  "properties": {
    "name": {
      "type": "string",
      "description": "User's full name.",
      "maxLength": 80
    },
    "email": {
      "description": "User's email.",
      "type": "string",
      "format": "email"
    },
    "date_of_birth": {
      "type": "string",
      "description": "Date of uses birth in the one and only date standard: ISO 8601.",
      "format": "date"
    },
    "gender": {
      "$ref" : "#/definitions/Gender"
    }
  },
  "required": ["name", "email"]
}
```

## Naming

Most type manifolds, including the JSON, XML, CSV, & YAML manifolds, follow the Java naming convention where a type name is based on the
resource file name relative to its location in the resource path. Thus the JSON resource file `resources/com/example/schemas/User.json`
has the Java type `com.example.schemas.User`.

The name *should* also match the schema `$id`, if one is provided.  The `User` type declares `"$id": "http://example.com/schemas/User.json"`,
which corresponds with the name `com.example.schemas.User`.

## Fluent API

JSON types are defined as a set of fluent _interface_ APIs.  For example, the `User` JSON type is an interface and
provides type-safe methods to:
* **create** a `User`
* **build** a `User`
* **modify** properties of a `User`  
* **load** a `User` from a string, a file, or a URL using HTTP GET
* **request** Web service operations using HTTP GET, POST, PUT, PATCH, & DELETE
* **write** a `User` as formatted JSON, CSV, YAML, or XML
* **copy** a `User`
* **cast** to `User` from any structurally compatible type including `Map`s, all *without proxies*

## Creating & Building JSON
You create an instance of a JSON type using either the `create()` method or the `builder()` method.

The `create()` method defines parameters matching the `required` properties defined in the JSON Schema, if the type is
plain JSON or no `required` properties are specified, `create()` has no parameters.

The `User.create()` method declares two parameters matching the `required` properties:
```java
static User create(String name, String email) {...}
```
You can use this to create a new instance of the `User` type with `name` and `email` arguments and then modify it using
_setter_ methods to change optional properties:
```java
import com.example.schemas.User;
import com.example.schemas.User.Gender;
import java.time.LocalDate;
...
User user = User.create("Scott McKinney", "scott@manifold.systems");
user.setGender(Gender.male);
user.setDate_of_birth(LocalDate.of(1980, 7, 4));
```

Alternatively, you can use `builder()` to fluently build a new instance:
```java
User user = User.builder("Scott McKinney", "scott@manifold.systems")
  .withGender(Gender.male)
  .withDate_of_birth(LocalDate.of(1980, 7, 4))
  .build();
```

You can initialize several properties in a chain of `with` calls in the builder. This saves a bit of typing with
heavier APIs.  After it is fully configured call the `build()` method to construct the type.

> Note `with` methods also serve as a means to initialize values for `readOnly` properties.

## Loading JSON
In addition to creating an object from scratch with `create()` and `build()` you can also load an instance from 
a variety of existing sources using `load()`.

You can load a `User` instance from a JSON, XML, CSV, or YAML String:
```java
// From a YAML string
User user = User.load().fromYaml( 
  "name: Scott McKinney\n" + 
  "email: scott@manifold.systems\n" +
  "gender: male\n" +
  "date_of_birth: 1980-07-04"
 );
```

Load from a file:
```java
// From a JSON file
User user = User.load().fromJsonFile("/path/to/MyUser.json");

// From an XML file
User user = User.load().fromJsonFile("/path/to/MyUser.xml");
```

You can invoke a REST API to fetch a `User` using HTTP GET:
```java
// Uses HTTP GET to invoke the API
User user = User.load().fromJsonUrl("http://api.example.com/users/$userId");
```

## Request REST API services
Use the `request()` static method to conveniently navigate an HTTP REST API with GET, POST, PUT, PATCH, & DELETE:
```java
String id = "scott";
User user = User.request("http://api.example.com/users").getOne("/$id");
```
The `request()` methods provides support for all basic REST API client usage:
```java
Requester<User> req = User.request("http://api.example.com/users");

// Get all Users via HTTP GET
IJsonList<User> users = req.getMany();

// Add a User with HTTP POST
User user = User.builder("scott", "mypassword", "Scott")
  .withGender(male)
  .build();
req.postOne(user);

// Get a User with HTTP GET
String id = user.getId();
user = req.getOne("/$id");

// Update a User with HTTP PUT
user.setDob(LocalDate.of(1980, 7, 7));
req.putOne("/$id", user);

// Delete a User with HTTP DELETE
req.delete("/$id");
```

## Configuring Proxies

If your endpoint involves a proxy server, you can setup an `Endpoint` class.

```java
private static final Endpoint ENDPOINT = new Endpoint("http://api.example.com/users", "http://my.proxy.com", 4321);
...
Requester<User> req = User.request(ENDPOINT);
...
req.postOne(user);
```
An `Endpoint` lets you specify the REST endpoint URL along with a proxy server address, port, and type. Simpler
constructors allow you to omit the port and/or type, which default to `8080` and `HTTP`. You can also use `Endpoint`
without a proxy using just a URL.

## Writing JSON
An instance of a JSON API object can be written as formatted text with `write()`:
* `toJson()` - produces a JSON formatted String
* `toYaml()` - produces a YAML formatted String
* `toXml()` - produces an XML formatted String
* `toCsv()` - produces a CSV formatted String

The following example produces a JSON formatted string:
```java
User user = User.builder("Scott McKinney", "scott@manifold.systems")
  .withGender(Gender.male)
  .withDate_of_birth(LocalDate.of(1980, 7, 4))
  .build();

String json = user.write().toJson();
System.out.println(json);
```
Output:
```json
{
  "name": "Scott McKinney",
  "email": "scott@manifold.systems",
  "gender": "male",
  "date_of_birth": "1980-07-04"
}
```

## Copying JSON
Use the `copy()` method to make a deep copy of any JSON API object:
```java
User user = User.create(...);
...
User copy = user.copy();
```
Alternatively, you can use the `copier()` static method for a richer set of features:
```java
User copy = User.copier(user).withName("Bob").copy();
```
`copier()` is a lot like `builder()` but lets you start with an already built object you can modify.  Also like
`builder()` it maintains the integrity of the schema's declared mutability -- you can't change
`readOnly` fields after the `copy()` method constructs the object.

## Properties Marked `readOnly` or `writeOnly` 
If a property is set to `readOnly` in a schema you can initialize it as a parameter in the `create()` and `builder()`
methods. A `readOnly` property does not have a corresponding setter method in the API, thus you can't modify it after a
type is initialized.

Conversely, a `writeOnly` property such as a password is only writable -- you cannot read such a property using a `get`
method.

## Nullable Properties
Manifold supports JSON Schema's many curious ways to say that a property can have a `null` value. These include:
* The type array:  `"type": ["", "null"]`
* The union type:  `"oneOf": [ ..., {"type": "null"}]`
* The enum type: `"enum": [..., null]`
* [OpenAPI 3.0](https://swagger.io/docs/specification/about/) _nullable_ attribute: `"nullable": true`

## **'additionalProperties'** and **'patternProperties'**
If a schema defines `additionalProperties` and/or `patternProperties`, the API provides a pair of methods to get/put 
arbitrary properties for a JSON instance, these are in addition to the getter/setter methods for named properties.
For instance, if a type `Thing` declares `additionalProperties` you can do this:
```java
Thing thing = Thing.create();
thing.put("MyProperty", "MyValue");
String value = (String)thing.get("MyProperty");
```  

For improved type-safety you can define structural interfaces for applicable properties:
```java
@Structural
public interface HasColor extends Bindings {
  default String getColor() {
    return (String)get("color");
  }  
  default void setColor(String value) {
    put("color", value);
  }  
} 
```
```java
HasColor hasColor = (HasColor)thing;
hasColor.setColor("blue");
String color = hasColor.getColor();
```

## Nested Types
Nested types defined within a JSON type, such as the `Gender` enum type in `User`, are available in the `User` API as
inner interfaces or enum types.  An nested interface type has all the same features as a top-level type including `create()`,
`builder()`, `load()`, etc.

## `format` Types
As you can see from the `User` example Manifold supports standard JSON Schema `format` types.  These include:

| Format           | JSON Type    | Java Type                                     |
|------------------|--------------|-----------------------------------------------|
| `"date-time"`    | `"string"`   | `java.time.LocalDateTime`                     |
| `"date"`         | `"string"`   | `java.time.LocalDate`                         |
| `"time"`         | `"string"`   | `java.time.LocalTime`                         |
| `"utc-millisec"` | `"integer"`  | `java.time.Instant`                           |
| `"int64"`        | `"integer"`  | `long` or `java.lang.Long` if nullable        |
| `"int32"`        | `"integer"`  | `int` or `java.lang.Integer` if nullable      |
| `"big-integer"`  | `"string"`   | `java.math.BigInteger`                        |
| `"big-decimal"`  | `"string"`   | `java.math.BigDecimal`                        |
| `"binary"`       | `"string"`   | `manifold.json.rt.api.OctetEncoding`      |
| `"byte"`         | `"string"`   | `manifold.json.rt.api.Base64Encoding`     | 

Other standard format types not listed here are supported but remain as `java.lang.String` or whichever `type` is 
specified along with the `format`.

Additionally, Manifold includes an API you can implement to provide your own custom formats.  Implement the 
`IJsonFormatTypeCoercer` interface as a 
[service provider](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html#register-service-providers).


## Composition Types with `allOf`
JSON Schema's `allOf` construct is a way to reuse types by composing a type with references to other types. A Manifold 
JSON type involving `allOf` uses interface composition to define the type.  
```yaml
definitions:
  Address:
    type: object
    properties:
       ...
 
type: object
properties: 
  BillingAddress:
  - $ref: '#/definitions/Address'      
  ShippingAddress:
    allOf:
    - $ref: '#/definitions/Address'
    - properties:
        Kind:
          enum:
          - residential
          - commercial
      required:
      - Kind
```
The resulting type for `ShippingAddress` is a composition of types utilizing interface inheritance:
```java
@Structural
public interface ShippingAddress extends Address {
  Kind getKind();
  void setKind(Kind value);
}
``` 

Note all JSON API interfaces are [structural](#structural_interfaces), which means JSON API types are assignable if the
methods they define are compatible.  Although `allOf` types conveniently use interface inheritance for re-use, it is
not necessary for assignability.


## Union Types with `oneOf`/`anyOf`
Normally you define a property with a single type, like `string` or `Address`.  However, using `oneOf` or `anyOf` you
can declare a _set_ of possible types for a property, where a property value can be an instance of any one of the types.  
The language community commonly refers to this as a [union type](https://en.wikipedia.org/wiki/Union_type). 

Although Java does not directly support unions, they can be synthesized with method naming conventions.
```yaml
pet:
  oneOf:
  - $ref: '#/definitions/Cat'
  - $ref: '#/definitions/Dog'
```  
The enclosing interface's `pet` property declares methods to reflect the possible types:
```java
Cat getPetAsCat();
void setPetAsCat(Cat value);
Dog getPetAsDog();
void setPetAsDog(Dog value);
Object getPet();
void setPet(Object value);
``` 
There is still only one value backing the `pet` property.

## Interfaces are _Structural_
JSON API interfaces are *structural* interfaces. You can read all about what a structural interface is [here](#structural-interfaces).  In short
a structural interface doesn't have to be implemented directly in order to be used.  For instance, you can make a
type-safe call through a structural interface method on an object so long as the object has a method with the same name
and compatible parameters:
```java
User user = (User) new FooUser();
user.setName("Scott");

public class FooUser {
...
  public void setName(String name) {...}
...
}
```
Even though `FooUser` does not directly implement our `User` API, we can still use `FooUser` as our `User` if it
satisfies the parts of `User` we need to invoke, such as the `setName()` method.  This can be handy when integrating
with other systems that may have generated classes from the same schemas.

Another example illustrating the utility of structural interfaces involves a more dynamic application. Manifold
provides an extension class to enable dynamic structural typing on any class deriving from Java's `Map`. This means you
can do this:
```java
HashMap<String, Object> map = new HashMap<>();
User user = (User) map;
user.setFirstName("Bob");
String bob = (String) map.get("name");
```
So useful is this that it is the foundation of the JSON API implementation.  All JSON API objects are directly backed by
the JSON `Bindings` map that is parsed from the JSON payload. This is also part of what makes the Manifold JSON API
uniquely both type-safe and the *single source of truth*.  There is literally nothing between your JSON Schema API documents and
the code that consumes them.

Read more about [dynamic structural typing](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#dynamic-typing-with-icallhandler).

## Extensions

You and the consumers of your JSON API can use Manifold extension classes to tailor it to specific lines of business.

You can add new methods:
```java
user.needsPasswordRemind();
...
```
```java
package extensions.com.example.schemas.User;
import com.example.schemas.User;
@Extension
public class MyUserExtension {
  public static boolean needsPasswordRemind(@This User thiz) {
    return passwordCheck(thiz, otherInfo);
  }

  public static void postAHyperMediaLink(...) {...}

  // more extension methods...
}
```

You can add new interfaces:
```java
package extensions.com.example.schemas.User;
import com.example.schemas.User;
@Extension
public class MyUserExtension extends EmailContact {
  // implement EmailContact methods User does not already satisfy here...
}
```
Now `User` also logically extends `EmailContact` and can be directly used as such in code.

> Note extensions do NOT physically alter the classes they extend, they only provide type information so the compiler can
resolve method calls and perform static type analysis.

You can even write your own type manifolds to dynamically generate extension classes and have your code automatically
resolve against the extensions. This can be useful to seamlessly add hypermedia linkage to your JSON API.  See
[Generating Extension Classes](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#generating-extension-classes) for more info.


## JSON, XML, CSV, & YAML Utilities
In addition to the JSON type manifold other forms of JSON, XML, CSV, and YAML support include:
* Extension methods on `URL` and `Bindings` e.g.,
```json
// Easily convert JSON for use as a HTTP query
myUrl.append(query.getBindings().makeArguments());
```
* The `Json`, `Xml`, `Yaml` and `JsonUtil` classes
* The `OctetEncoding` and `Base64Encoding` classes facilitate sending/receiving binary information
* Structural interfaces on `Bindings` -- you can define your own structural interfaces for improved type-safety on
bindings (or maps)


# IDE Support 

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>

## Sample Project

Experiment with the [Manifold Sample REST API Project](https://github.com/manifold-systems/manifold-sample-rest-api)
via:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from Version Control</kbd> ➜ <kbd>Git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProjectMenu.png" alt="echo method" width="60%" height="60%"/></p>

Enter: <kbd>https://github.com/manifold-systems/manifold-sample-rest-api.git</kbd>

<p><img src="http://manifold.systems/images/OpenSampleProject_json.png" alt="echo method" width="60%" height="60%"/></p>

Use the [plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to really boost your productivity. Make changes to
your JSON, XML, CSV, and YAML files and use the changes immediately in your code, no compilation step.  You can use features such
as Find Usages, Refactor/Rename, and Navigation directly between elements in JSON, XML, CSV, and YAML resources files and Java
files. Additionally you can make and test changes in a live application or service using IntelliJ's Hotswap debugger.

# Setup

## Building this project

The `manifold-json` project is defined with Maven.  To build it install Maven and a Java 8 JDK and run the following
command.
```
mvn compile
```

## Using this project

The `manifold-json` dependency works with all build tooling, including Maven and Gradle. It fully supports Java versions
8 - 19.

This project consists of two modules:
* `manifold-json`
* `manifold-json-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-json-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold-json` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

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
    implementation 'systems.manifold:manifold-json-rt:2023.1.3'
    testImplementation 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor 'systems.manifold:manifold-json:2023.1.3'
    testAnnotationProcessor 'systems.manifold:manifold-json:2023.1.3'
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
    <artifactId>my-json-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Json App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-json-rt</artifactId>
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
                            <artifactId>manifold-json</artifactId>
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

`manifold-json`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-json/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-json/2023.1.3)

`manifold-json-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-json-rt/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-json-rt/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
