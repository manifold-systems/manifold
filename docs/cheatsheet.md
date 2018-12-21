---
layout: default
---

# Manifold Cheat Sheet

## [Meta-programming](http://manifold.systems/docs.html#manifold-in-a-nutshell)
Gain direct, type-safe access to <i>any</i> data source. Remove the code gen step in your build process.

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
```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type" : "object",
  "id" : "urn:xyz:Contact:1.0",

  "definitions": {
    "Address": {
      "type": "object",
      "properties": {
        "street_address": { "type": "string" },
        "city":           { "type": "string" },
        "state":          { "type": "string" }
      },
      "required": ["street_address", "city", "state"]
    }
  },
...
}
```
```java 
import abc.Contact;
...
Contact contact = Contact.fromJsonUrl(url);
```
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

## [Structural Interfaces](http://manifold.systems/docs.html#structural-interfaces)
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

ManTL has a separate [cheat sheet](http://manifold.systems/manifold-templates-cheatsheet.html)

## [Libraries](http://manifold.systems/docs.html#extension-libraries)
Leverage stock Manifold extension libraries for standard Java classes. Save time and reduce boilerplate code.
```java
File file = new File(path);
// Use refreshing extensions to File
String content = file.readText();
```  

## [IntelliJ](http://manifold.systems/docs.html#working-with-intellij)
Use the Manifold IntelliJ IDEA plugin to fully leverage Manifold in your development cycle. The plugin provides 
comprehensive support for IntelliJ features including code completion, navigation, usage searching, refactoring, 
incremental compilation, hotswap debugging, full-featured template editing, and more.

## [Learn More](http://manifold.systems/docs.html)
