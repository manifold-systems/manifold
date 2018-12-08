<center>
  <img src="https://i.imgur.com/No1RPUf.png" width="80%"/>
</center>

## What is Manifold?
[Manifold](http://manifold.systems) is a new breed of Java tooling. It provides powerful features to make 
Java development more appealing and productive. Simply add the Manifold jar to your project and begin taking advantage 
of it.

## What can you do with Manifold?

### [Meta-programming](http://manifold.systems/docs.html#manifold-in-a-nutshell)
Gain direct, type-safe access to structured data. Eliminate code generators. Minimize build time.
```java 
// JSON files are types!
Person person = Person.fromJsonUrl(url);
person.setFirstName("Scott");
```

### [Extensions](http://manifold.systems/docs.html#the-extension-manifold)
Add extension methods and interfaces to existing Java classes, even String, List, and File. Eliminate boilerplate code.
```java
String greeting = "hello";
greeting.myMethod(); // augment any type
```  

### [Structural Typing](http://manifold.systems/docs.html#structural-interfaces)
Unify disparate APIs. Bridge software components you do not control. Access maps through type-safe interfaces.
```java
MyInterface thing = (MyInterface)notMyInterface;
thing.myMethod(); // treat as your interface
```

### [Templates](http://manifold.systems/docs.html#templating)
Make type-safe, templatized data files using pure Java. Use the same template expressions in Java strings.
```java
int hour = 8;
// print "It is 8 o'clock"
System.out.println("It is $hour o'clock"); 
``` 
 
### [Type-safe Reflection](http://manifold.systems/docs.html#jailbreak)
Access private features with <b>@JailBreak</b> to avoid the drudgery and vulnerability of Java reflection.
```java
@JailBreak Foo foo = new Foo();
// Direct, type-safe access to *all* foo's members
foo.privateMethod(x, y, z); 
foo.privateField = value;
```
       
### [Libraries](http://manifold.systems/docs.html#extension-libraries)
Leverage stock Manifold extension libraries for standard Java classes. Save time and reduce boilerplate code.
```java
File file = new File(path);
// Use refreshing extensions to File
String content = file.readText();
```  
### [IntelliJ](http://manifold.systems/docs.html#working-with-intellij)
Use the Manifold IntelliJ IDEA plugin to fully leverage Manifold in your development cycle. The plugin provides 
comprehensive support for IntelliJ features including code completion, navigation, usage searching, refactoring, 
incremental compilation, hotswap debugging, full-featured template editing, and more.

### [Learn More](http://manifold.systems/docs.html)
