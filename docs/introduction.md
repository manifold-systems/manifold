---
layout: default
---

## Introducing _Manifold_!
[Manifold](http://manifold.systems) is a breakthrough technology you can use to seamlessly extend Java to compile and load types from sources
_in addition to_ `.java` files.  Using this framework your code has direct, type-safe access to metadata such as YAML & JSON Schema files, DDL & SQL queries,
and even other programming languages. Building on this foundation Manifold provides an ever growing set of extensions, including comprehensive support for
[YAML and JSON Schema](http://manifold.systems/docs.html#json-and-json-schema),
[extension methods](http://manifold.systems/docs.html#extension-classes),
full featured [templates](http://manifold.systems/manifold-templates.html),
[string interpolation](http://manifold.systems/docs.html#templating), and a lot more.

Manifold fulfills the promise: _**your metadata is the single source of truth**_. There is *nothing* to manage between your metadata and your
code -- no code generation steps in your build, no POJOs, no annotation processor steps, no custom class loaders, no runtime agents.

All features are fully supported in IntelliJ IDEA.  Author JSON Schema documents and code against them as you make changes without
a code generation step. Jump directly to a YAML property from a call site in your code. Quickly rename a JSON field and its
usages across your codebase.  Use Hotswap to make and test changes to files while debugging.  Author templates with the full
expressive power of Java and use them type-safely in your code.  Etc. [Check it out!](http://manifold.systems/images/ExtensionMethod.mp4)

Manifod is easy to use, it's just a JAR file you add to your existing project.  Just drop it in and begin taking advantage of it.

## What can you do with Manifold?

### [Meta-programming](http://manifold.systems/docs.html#manifold-in-a-nutshell)
Use the framework to gain direct, type-safe access to <i>any</i> type of metadata, such as JSON Schema and YAML. Remove the code gen step in your build process.
```java
// Use your User.json schema file directly as a type, no code gen!
User user = User.builder("myid", "mypassword", "Scott")
  .withGender(male)
  .withDob(LocalDate.of(1987, 6, 15))
  .build();
User.request("htt://api.example.com/users").postOne(user);
```

### [Extensions](http://manifold.systems/docs.html#the-extension-manifold)
Add extension methods to existing Java classes, even String, List, and File. Eliminate boilerplate code. [Check it out!](http://manifold.systems/images/ExtensionMethod.mp4)
```java
String greeting = "hello";
greeting.myMethod(); // Add your own methods to String!
```

### [Structural Typing](http://manifold.systems/docs.html#structural-interfaces)
Unify disparate APIs. Bridge software components you do not control. Access maps through type-safe interfaces.
```java
Map<String, Object> map = new HashMap<>();
MyThingInterface thing = (MyThingInterface) map; // O_o
thing.setFoo(new Foo());
Foo foo = thing.getFoo();
out.println(thing.getClass()); // prints "java.util.HashMap"
```

### [Type-safe Reflection](http://manifold.systems/docs.html#type-safe-reflection)
Access private features with <b>@Jailbreak</b> to avoid the drudgery and vulnerability of Java reflection.
```java
@Jailbreak Foo foo = new Foo();
// Direct, *type-safe* access to *all* foo's members
foo.privateMethod(x, y, z);
foo.privateField = value;
```

### [Checked Exception Suppression](http://manifold.systems/docs.html#checked-exception-suppression)
Simply add the `exceptions` plugin argument: `-Xplugin:Manifold strings` *`exceptions`*. Now checked exceptions
behave like unchecked exceptions!  No more compiler errors, no more boilerplate `try`/`catch` nonsense.
```java
List<String> strings = ...;
List<URL> urls = list
  .map(URL::new) // No need to handle the MalformedURLException!
  .collect(Collectors.toList());
```

### [String Templates](http://manifold.systems/docs.html#templating) (aka String Interpolation)
Embed variables and expressions in String literals, no more clunky string concat!
```java
int hour = 15;
// Simple variable access with '$'
String result = "The hour is $hour"; // Yes!!!
// Use expressions with '${}'
result = "It is ${hour > 12 ? hour-12 : hour} o'clock";
```

### [Template Files with *ManTL*](http://manifold.systems/manifold-templates.html)
Author template files with the full expressive power of Java, use your templates directly in your code as types.
```java
List<User> users = ...;
String content = abc.example.UserSample.render(users);
```
A tempate file `abc/example/UserSample.html.mtl`
```html
<%@ import java.util.List %>
<%@ import com.example.User %>
<%@ params(List<User> users) %>
<html lang="en">
<body>
<% users.stream()
   .filter(user -> user.getDateOfBirth() != null)
   .forEach(user -> { %>
    User: ${user.getName()} <br>
    DOB: ${user.getDateOfBirth()} <br>
<% }); %>
</body>
</html>
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
