---
layout: default
---

# Manifold Cheat Sheet

>#### [IntelliJ IDEA](http://manifold.systems/docs.html#working-with-intellij)
>Manifold is best experienced in IntelliJ IDEA. The Manifold plugin provides comprehensive support for IntelliJ features 
including code completion, navigation, usage searching, refactoring, incremental compilation, hotswap debugging, 
full-featured ManTL template editing, and more. 
>
>Install the plugin directly from IntelliJ:
>
><kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Browse repositories</kbd> ➜ search: `Manifold`

## Sample Projects

Clone the [Manifold sample project](https://github.com/manifold-systems/manifold-sample-project) to for a nice
demonstration of features.

Clone the [Manifold sample REST API project](https://github.com/manifold-systems/manifold-sample-rest-api) to quickly
begin experimenting with a JSON Schema REST API using Manifold.

Clone the [Manifold sample Web App project](https://github.com/manifold-systems/manifold-sample-web-app) to get hooked
on ManTL templates with the Manifold IntelliJ plugin.

## [Meta-programming](http://manifold.systems/docs.html#manifold-in-a-nutshell)
Gain direct, **type-safe** access to *any* type of data. Remove the code gen step in your build process.

### Resources
Put data files in your `resource path`.

A typical <b>Maven</b> setup:
```text
src
-- main
---- java
------ <your .java files>
---- resources
------ <your .png, .json, .mtl, and other resource files>
```

### [JSON & JSON Schema](http://manifold.systems/docs.html#json-and-json-schema)

You can use both sample JSON files and JSON Schema files.

`resources/abc/Person.json`
```json
{
  "Name": "Joe Jayson",
  "Age": 39,
...
}
```
Use `Person.json` as a JSON-by-example schema: 
```java 
import abc.Person;
...
Person person = Person.fromJsonUrl(url);
person.setFirstName("Scott");
```
Or use JSON Schema files:
`resources/abc/Contact.json`
Here is a simple `User` type defined in `resources/abc/User.json` using JSON Schema:
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
You can use this to create a new instance of the `User` type and then modify it using _setter_ methods to change
optional properties:
```java
import abc.User;
import abc.User.Gender;
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
You can load a `User` instance from a String:
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
```

You can invoke a REST API to fetch a `User` using HTTP GET:
```java
// Uses HTTP GET to invoke the API
User user = User.load().fromJsonUrl("http://api.example.com/users/$userId");
```

#### Request REST API services
Use the `request()` static method to conveniently navigate an HTTP REST API with GET, POST, PUT, PATCH, & DELETE:
```java
String id = "scott";
User user = User.request("http://api.example.com/users").getOne("/$id");
```
The `request()` method provides support for all basic REST API client usage:
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
> Clone the [Manifold sample REST API project](https://github.com/manifold-systems/manifold-sample-rest-api) to quickly
begin experimenting with a JSON Schema REST API using Manifold.

#### Writing JSON
An instance of a JSON API object can be written as formatted text with `write()`:
* `toJson()` - produces a JSON formatted String
* `toYaml()` - produces a YAML formatted String
* `toXml()` - produces an XML formatted String

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

### [YAML](http://manifold.systems/docs.html#json-and-json-schema)

Manifold fully supports YAML 1.2.  You can use YAML to build JSON Schema files as well.  All that applies to JSON applie to YAML.


### [Properties](http://manifold.systems/docs.html#properties-files)
Avoid strings, access properties type-safely:

`resources/abc/MyProperties.properties`
```properties
chocolate = Chocolate
chocolate.milk = Milk chocolate
chocolate.dark = Dark chocolate
``` 
```java
String myMessage = MyProperties.chocolate.milk;
```

### [Images](http://manifold.systems/docs.html#image-files)
Gain direct type-safe access to all your project's images, efficiently cached:

`resources/abc/images/companyLogo.png`
```java
import abc.images.*;
...
ImageIcon image = companyLogo_png.get();
render(image);
```

### [JavaScript](http://manifold.systems/docs.html#javascript)

`resources/abc/MyJsProgram.js`:
```javascript
var x = 1;

function nextNumber() {
  return x++;
}

function doSomething(x) {
  return x + " from Javascript";
}
```

A JavaScript file and its members are directly accessible as a Java class:

```java
import abc.MyJsProgram;
...
String hello = MyJsProgram.doSomething("Hello");
System.out.println(hello); // prints 'Hello from JavaScript'

double next = JsProgram.nextNumber();
System.out.println(next); // prints '1'
```

## [String Templates](http://manifold.systems/docs.html#templating) (string interpolation)
### Enabling
By default String templates are _disabled_.  Enable the feature with the `strings` Manifold plugin argument.

Maven:
```xml
<compilerArgs>
  <arg>-Xplugin:Manifold strings</arg>
</compilerArgs>
```
### Using
A **String template** lets you use the `$` character to embed a Java expression directly into a String.  You can 
use `$` to embed a simple variable:
```java
int hour = 8;
String time = "It is $hour o'clock";  // prints "It is 8 o'clock"
```
Or you can embed an expression of any complexity in curly braces:
```java
LocalTime localTime = LocalTime.now();
String ltime = "It is ${localTime.getHour()}:${localTime.getMinute()}"; // prints "It is 8:39"
```
Escape the `$` with `\$`.

Use `@DisableStringLiteralTemplates` to turn string templates off at the class and method levels.  

## [Extensions](http://manifold.systems/docs.html#the-extension-manifold)

### Basic
Add your own methods to any class e.g., `java.lang.String`:

Make a class in a package named `extensions`. Create a sub-package using the full name of the class you want to extend,
in this case `java.lang.String`:
    
```java
package abc.extensions.java.lang.String;

import manifold.ext.api.*;

@Extension
public class MyStringExtension {

  public static void print(@This String thiz) {
    System.out.println(thiz);
  }

  @Extension // required for static extension methods
  public static String lineSeparator() {
    return System.lineSeparator();
  }
}
```
Now the methods are available directly from `String`:
```java
String hello = "hello";
hello.print();

// static method
String.lineSeparator();
```

### Generics
Here `map` is a generic extension method on `Collection` having type variable `R` and conveying `Collection`'s type
variable `E`. Extension methods must reflect the type variable names declared in the extended class.
```java
public static <E, R> Stream<R> map(@This Collection<E> thiz, Function<? super E, R> mapper) {
  return thiz.stream().map(mapper);
}
```

## [@Structural - Structural Interfaces](http://manifold.systems/docs.html#structural-interfaces)
Unify disparate APIs. Bridge software components you do not control. Access maps through type-safe interfaces.
```java
@Structural
public interface Coordinate {
  double getX();
  double getY();
}
```
Structural interface applied to `java.awt.Rectangle`:
```java
setLocation((Coordinate)new Rectangle(10, 10, 100, 100));
...
void setLocation(Coordinate location) {
  this.location = location;
}
```
Structural interface applied to `java.util.HashMap` via `ICallHandler`:
```java
Map<String,Integer> map = new HashMap<>();
map.put("x", 10);
map.put("y", 10);

Coordinate coord = (Coordinate)map;
double x = coord.getX();
``` 
  
## [@Jailbreak - Type-safe Reflection](http://manifold.systems/docs.html#type-safe-reflection)
Access private features with <b>@Jailbreak</b> to avoid the drudgery and vulnerability of Java reflection.
### Basic
```java
@Jailbreak Foo foo = new Foo();
foo.privateMethod();
foo.privateMethod("hey");
foo._privateField = 88;
```
```java
public class Foo {
  private final int _privateField;
  
  public Foo(int value) {
    _privateField = value;
  }
  
  private String privateMethod() {
    return "hi";
  }
  
  private String privateMethod(String param) {
    return param;
  }
}
```
### Static Members
```java
@Jailbreak MyClass myClass = null; // value is insignificant
myClass.staticMethod();
myClass.Static_Field = "hi";
```
```java
public class MyClass {
  private static String Static_Field = "hello";
  
  private static void staticMethod() {
  }
}
```

### Types and Constructors
```java
com.abc. @Jailbreak SecretClass secretClass = 
  new com.abc. @Jailbreak SecretClass("hi");
secretClass._data = "hey";
```

## [@Self - The Self Type](http://manifold.systems/docs.html#the-self-type)

Use `@Self` on method return types to enforce `type of this` where suitable.

### Basic

A common use-case for the self type involves the *Builder* pattern:

```java
public class VehicleBuilder {
  private int _wheels;
  
  public @Self VehicleBuilder withWheels(int wheels) {
    _wheels = wheels;
    return this;
  }
}
```  

With the return type annotated with `@Self` the example works as desired:

```java
Airplane airplane = new AirplaneBuilder()
  .withWheels(2) // return type is AirplaneBuilder GOOD!
  .withWings(1) 
``` 

### Self + Generics

```java
public class Node {
  private List<Node> children;
  
  public List<@Self Node> getChildren() {
      return children;
  }
}

public class MyNode extends Node {
  ...
}
```
```java
MyNode myNode = findMyNode();
List<MyNode> = myNode.getChildren(); // wunderbar! 
```

### Self + Extensions

You can use `@Self` with extension methods too.  Here we make an extension method as a means to conveniently chain 
insertions to `Map` while preserving its concrete type:

```java
public static <K,V> @Self Map<K,V> add(@This Map<K,V> thiz, K key, V value) {
  thiz.put(key, value);
  return thiz;
}

HashMap<String, String> map = new HashMap<>()
  .add("bob", "fishspread")
  .add("alec", "taco")
  .add("miles", "mustard");
```

## [ManTL](http://manifold.systems/manifold-templates.html) (Superfast **type-safe** templates)

ManTL has a separate [cheat sheet](http://manifold.systems/manifold-templates.html)

## [Libraries](http://manifold.systems/docs.html#extension-libraries)
Leverage stock Manifold extension libraries for standard Java classes. Save time and reduce boilerplate code.
```java
File file = new File(path);
// Use refreshing extensions to File
String content = file.readText();
```  
Use the `manifold-all` dependency to access all Manifold's provided extension libraries including I/O, Web, and 
Collections.

## [Learn More](http://manifold.systems/docs.html)
