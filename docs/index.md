---
layout: default
---

<div style="margin-top:40px">
  <center>
    <img src="https://i.imgur.com/No1RPUf.png" width="60%"/>
  </center>
</div>

[![Github](https://img.shields.io/badge/github-manifold-blue.svg)](https://github.com/manifold-systems/manifold)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/systems.manifold/manifold-all/badge.svg)](https://maven-badges.herokuapp.com/maven-central/systems.manifold/manifold-all)
[![IJ Plugin](https://img.shields.io/jetbrains/plugin/d/10057-manifold.svg)](https://plugins.jetbrains.com/plugin/10057-manifold)
[![Gitter](https://badges.gitter.im/manifold-systems/community.svg)](https://gitter.im/manifold-systems/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

## What is Manifold?
[Manifold](http://manifold.systems) plugs into Java to supplement it with game-changing features, from Type-safe Metaprogramming,
Structural Typing, and Extension Methods to an integrated Template Engine and a Preprocessor. All fully supported in IntelliJ IDEA.
Simply add Manifold to your project and begin taking advantage of it.

## What can you do with Manifold?

### [Meta-programming](http://manifold.systems/docs.html)
Use the framework to gain direct, type-safe access to <i>any</i> type of metadata, such as **GraphQL**, **JSON Schema**
and **YAML**. Remove the code gen step in your build process. [Check it out!](http://manifold.systems/images/graphql.mp4)
```java
// Use your User.json schema file directly as a type, no code gen!
User user = User.builder("myid", "mypassword", "Scott")
  .withGender(male)
  .withDob(LocalDate.of(1987, 6, 15))
  .build();
User.request("http://api.example.com/users").postOne(user);
```

### [Extensions](http://manifold.systems/docs.html#the-extension-manifold)
Add extension methods to existing Java classes, even String, List, and File. Eliminate boilerplate code. [Check it out!](http://manifold.systems/images/ExtensionMethod.mp4)
```java
String greeting = "hello";
greeting.myMethod(); // Add your own methods to String!
```

### [Preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)
Use familiar directives such as `#define` and `#if` to conditionally compile your Java projects. The preprocessor offers
a simple and convenient way to support multiple build targets with a single codebase. [Check it out!](http://manifold.systems/images/preprocessor.mp4)
```csharp
#if JAVA_8_OR_LATER
  @Override
  public void setTime(LocalDateTime time) {...)
#else
  @Override
  public void setTime(Calendar time) {...}
#endif
```

### [Structural Typing](http://manifold.systems/docs.html#structural-interfaces)
Unify disparate APIs. Bridge software components you do not control. Access maps through type-safe interfaces. [Check it out!](http://manifold.systems/images/structural%20typing.mp4)
```java
Map<String, Object> map = new HashMap<>();
MyThingInterface thing = (MyThingInterface) map; // O_o
thing.setFoo(new Foo());
Foo foo = thing.getFoo();
out.println(thing.getClass()); // prints "java.util.HashMap"
```

### [Type-safe Reflection](http://manifold.systems/docs.html#type-safe-reflection)
Access private features with <b>@Jailbreak</b> to avoid the drudgery and vulnerability of Java reflection. [Check it out!](http://manifold.systems/images/jailbreak.mp4)
```java
@Jailbreak Foo foo = new Foo();
// Direct, *type-safe* access to *all* foo's members
foo.privateMethod(x, y, z);
foo.privateField = value;
```

### [Checked Exception Suppression](http://manifold.systems/docs.html#checked-exception-suppression)
You now have an option to make checked exceptions behave like unchecked exceptions!  No more unintended exception
swallowing, no more boilerplate `try`/`catch`/`wrap`/`rethrow` nonsense.
```java
List<String> strings = ...;
List<URL> urls = list
  .map(URL::new) // No need to handle the MalformedURLException!
  .collect(Collectors.toList());
```

### [String Templates](http://manifold.systems/docs.html#templating) (aka String Interpolation)
Embed variables and expressions in String literals, no more clunky string concat! [Check it out!](http://manifold.systems/images/string_interpolation.mp4)
```java
int hour = 15;
// Simple variable access with '$'
String result = "The hour is $hour"; // Yes!!!
// Use expressions with '${}'
result = "It is ${hour > 12 ? hour-12 : hour} o'clock";
```

### [Template Files with *ManTL*](http://manifold.systems/manifold-templates.html)
Author template files with the full expressive power of Java, use your templates directly in your code as types.
Supports type-safe inclusion of other templates, shared layouts, and more. [Check it out!](http://manifold.systems/images/mantl.mp4)
```java
List<User> users = ...;
String content = abc.example.UserSample.render(users);
```
A template file `abc/example/UserSample.html.mtl`
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
<br>