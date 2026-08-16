<br>

![manifold-diamond.png](docs/images/manifold-diamond2.png)

![latest](https://img.shields.io/badge/latest-v2026.1.4-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

---

# Manifold

**Manifold extends Java at compile time.**

It adds language features and type-safe access to external data, APIs, and DSLs *directly* to Java, without leaving the
Java compiler and without additional build steps.

```java
// Type-safe SQL, written directly in Java
Language english =
    "[.sql/] select * from Language where name = 'English'".fetchOne();
```
Or add behavior to existing Java types:
```java
"hello".myStringMethod();
```
Or use [_Parts_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-parts) for polymorphic composition of independent objects:
```java
@part class BaseHero implements Hero {
  void takeAction() {
    attack();  // dynamically dispatches through the composite
  }
}
```

Manifold is a **javac compiler plugin**. Add only the features you want to an existing Java project and keep using ordinary
Java, Java libraries, and the JVM.

Works with JDK LTS releases 8 - 25 + latest, plus comprehensive IDE support in **IntelliJ IDEA** and **Android Studio**.
             
---

# What can you do with Manifold?

## [Parts](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-parts)

*Parts* resolves a long-standing dilemma in statically typed object-oriented programming: **independent runtime components** and **internal polymorphism**
remain unreconciled in general-purpose models.

|                                      | Independent components | Internal polymorphism |
| :----------------------------------- | :--------------------: | :-------------------: |
| Implementation inheritance           |            —           |           ✓           |
| Trait/mixin composition (flattening) |            —           |           ✓           |
| Object composition (forwarding)      |            ✓           |           —           |
| ***Parts***                          |          **✓**         |         **✓**         |

*Parts* is the first to provide both properties in arbitrary compositions without sacrificing component independence, limiting
polymorphism, or compromising performance.

```java
interface Actor {
  void takeAction();
  void attack();
}

@part class Hero implements Actor {
  public void takeAction() {
    attack();   // self-call is dynamically dispatched through the composite
  }
 
  public void attack() {println("Swing club!");}
}

class Wizard implements Actor {
  @link Actor actor;  // dynamically links an independent Actor object
  Wizard(Actor actor) {this.actor = actor;}
  public void attack() {println("Cast spell!");}
}

Actor hero = createActor(); // independent Actor supplied at runtime (factory, DI, etc.)
Wizard wizard = new Wizard(hero);
wizard.takeAction();
```
`Hero.takeAction()` calls `attack()` on itself (a self-call). Because Hero is a linked part of the Wizard composite,
the self-call dispatches to `Wizard.attack()`, so the output is:
```
Cast spell!
```
With ordinary object composition, Hero's own `attack()` would be called and `Swing club!` would result instead.

This **internal polymorphism across a runtime-injected part** is the defining capability of Parts, and it comes at **vtable speed**:
dispatch is O(1), performance equivalent to a conventional virtual call. See the [Parts README](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-parts/README.md)
for a deeper dive, or the [formal paper](https://doi.org/10.5281/zenodo.21514973) for the underlying model and supporting analysis.

---

## [Compile-time meta-programming](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)
Use the framework to gain direct, type-safe access to *any* type of resource, such as
[**SQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql),
[**JSON**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
[**GraphQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[**XML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml),
[**YAML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml),
[**CSV**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv), and even other languages such as [**JavaScript**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)

[**SQL:**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql)
Use _native_ SQL of any complexity _directly_ and _type-safely_ from Java.
```java
Language english =
  "[.sql/]select * from Language where name = 'English'".fetchOne();
Film film = Film.builder("My Movie", english)
  .withDescription("Nice movie")
  .withReleaseYear(2023)
  .build();
MyDatabase.commit();
```

[**JSON:**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
Use .json schema files directly and type-safely, no code gen steps! Find usages of .json properties in your Java code.
```java
// From User.json
User user = User.builder("myid", "mypassword", "Scott")
  .withGender(male)
  .withDob(LocalDate.of(1987, 6, 15))
  .build();
User.request("http://api.example.com/users").postOne(user);
```

[**GraphQL:**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
Use types defined in .graphql files *directly*, no code gen steps! Make GraphQL changes and immediately use them with code completion.
```java
var query = MovieQuery.builder(Action).build();
var result = query.request("http://com.example/graphql").post();
var actionMovies = result.getMovies();
for (var movie : actionMovies) {
  out.println(
    "Title: " + movie.getTitle() + "\n" +
    "Genre: " + movie.getGenre() + "\n" +
    "Year: " + movie.getReleaseDate().getYear() + "\n");
}
```

---

## [Extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
Add your own methods to existing Java classes, even *String*, *List*, and *File*. Eliminate boilerplate code.
[&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/ExtensionMethod.mp4)
```java
String greeting = "hello";
greeting.myMethod(); // Add your own methods to String!
```
Or, **intercept** existing methods.
```java
// Intercepts Search.find() to insert a caching layer
result = search.find(); // returns cached results
``` 

---

## [Properties](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)
Eliminate boilerplate getter/setter code, improve your overall dev experience with properties.
```java
public interface Book {
  @var String title; // no more boilerplate code!
}
// refer to it directly by name
book.title = "Daisy";     // calls setter
String name = book.title; // calls getter 
book.title += " chain";   // calls getter & setter
```
Additionally, the feature automatically _**infers**_ properties, both from your existing source files and from
compiled classes your project uses. Reduce property use from this:
```java
Actor person = result.getMovie().getLeadingRole().getActor();
Likes likes = person.getLikes();
likes.setCount(likes.getCount() + 1);
```
to this:
```java
result.movie.leadingRole.actor.likes.count++;
``` 

---

## [Optional parameters & named arguments](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-params)
Use optional parameters and named arguments with any Java project to add clarity and flexibility to call sites
and as a refreshing alternative to method overloads and builders.
```java
String valueOf(char[] data, 
               int offset = 0, 
               int count = data.length - offset) {...}

valueOf(array) // use defaults for offset and count
valueOf(array, 2) // use default for count
valueOf(array, count:20) // use default for offset by naming count
```
Binary compatible with methods, constructors, and records.

---

## [Operator Overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
Implement *operator* methods on any type to directly support arithmetic, relational, index, and unit operators.
```java
// BigDecimal expressions
if (bigDec1 > bigDec2) {
  BigDecimal result = bigDec1 + bigDec2;
  ...
}
// Implement operators for any type
MyType value = myType1 + myType2;
```  

---

## [Tuple expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple)
Tuple expressions provide concise syntax to group named data items in a lightweight structure.
```java
var t = (name: "Bob", age: "35");
System.out.println("Name: " + t.name + " Age: " + t.age);

var t = (person.name, person.age);
System.out.println("Name: " + t.name + " Age: " + t.age);
```
You can also use tuples with new [`auto` type inference](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto) to enable multiple return values from a method.

---

## [Unit Expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
Unit or *binding* operations are unique to the Manifold framework. They provide a powerfully concise syntax and can be
applied to a wide range of applications.

Use units with the [science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science) framework to type-safely incorporate SI units and precise measurements into your applications.
```java
import static manifold.science.util.UnitConstants.*; // kg, m, s, ft, etc
...
Length distance = 100 mph * 3 hr;
Force f = 5.2 kg m/s/s; // same as 5.2 N
Mass infant = 9 lb + 8.71 oz;
Time time = 1min + 3.7sec;
```  

---

## [Preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)
Use familiar directives such as **#define** and **#if** to conditionally compile your Java projects. The preprocessor offers
a simple and convenient way to support multiple build targets with a single codebase. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/preprocessor.mp4)
```java
#if JAVA_8_OR_LATER
  @Override
  public void setTime(LocalDateTime time) {...}
#else
  @Override
  public void setTime(Calendar time) {...}
#endif
```   

---

## [Structural Typing](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
Unify disparate APIs. Bridge software components you do not control. Use structural typing to access objects (and maps)
using type-safe interfaces. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/structural%20typing.mp4)
```java
Map<String, Object> map = new HashMap<>();
MyThingInterface thing = (MyThingInterface) map; // O_o
thing.setFoo(new Foo());
Foo foo = thing.getFoo();
out.println(thing.getClass()); // prints "java.util.HashMap"
```

---

## [Type-safe Reflection](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-safe-reflection-via-jailbreak)
Access private features with <b>@Jailbreak</b> to avoid the drudgery and vulnerability of Java reflection. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/jailbreak.mp4) 
```java
@Jailbreak Foo foo = new Foo();
// Direct, *type-safe* access to *all* foo's members
foo.privateMethod(x, y, z);
foo.privateField = value;
```

---

## [Checked Exception Handling](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)
You now have an option to make checked exceptions behave like unchecked exceptions!  No more unintended exception
swallowing. No more *try*/*catch*/*wrap*/*rethrow* boilerplate!
```java
List<String> strings = ...;
List<URL> urls = strings.stream()
  .map(URL::new) // No need to handle the MalformedURLException!
  .collect(Collectors.toList());
```

---

## [String Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings)
Inline variables and expressions in String literals, no more clunky string concat! [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/string_interpolation.mp4)
```java
int hour = 15;
// Simple variable access with '$'
String result = "The hour is $hour"; // Yes!!!
// Use expressions with '${}'
result = "It is ${hour > 12 ? hour-12 : hour} o'clock";
``` 

---

## [A *Java* Template Engine](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)
Author template files with the full expressive power of Java, use your templates directly in your code as types.
Supports type-safe inclusion of other templates, shared layouts, and more. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/mantl.mp4)
```java
List<User> users = ...;
String content = abc.example.UserSample.render(users);
```
A template file *abc/example/UserSample.html.mtl*
```html
<%@ import java.util.List %>
<%@ import com.example.User %>
<%@ params(List<User> users) %>
<html lang="en">
<body>
<% for(User user: users) { %> 
  <% if(user.getDateOfBirth() != null) { %>
    User: ${user.getName()} <br>
    DOB: ${user.getDateOfBirth()} <br>
  <% } %>
<% } %>
</body>
</html>
```

---

# Who is using Manifold?

Sampling of companies using Manifold:

<img width="80%" src="./docs/images/companies.png">

---

# [IDE Support](https://github.com/manifold-systems/manifold)
Use the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to fully leverage Manifold with **IntelliJ IDEA** and **Android Studio**. The plugin provides
comprehensive support for Manifold including code completion, navigation, usage searching, refactoring, incremental compilation,
hotswap debugging, full-featured template editing, integrated preprocessor, and more.

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="manifold ij plugin" width="60%"/></p>

[Get the plugin from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/10057-manifold)

---

# [Projects](https://github.com/manifold-systems/manifold)
The Manifold project consists of the core Manifold framework and a collection of sub-projects implementing SPIs provided
by the core framework. Each project consists of one or more **dependencies** you can easily add to your project:

[Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)<br>

[Manifold : _Extensions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)<br>

[Manifold : _Parts_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-parts)<br>

[Manifold : _Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)<br>

[Manifold : _Optional parameters & named arguments_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-params)<br>

[Manifold : _Tuples_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple)<br>

[Manifold : _SQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql)<br>
[Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)<br>
[Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)<br>
[Manifold : _XML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml)<br>
[Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)<br>
[Manifold : _CSV_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv)<br>
[Manifold : _Property Files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)<br>
[Manifold : _Image_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)<br>
[Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)<br>
[Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)<br>

[Manifold : _Java Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)<br>

[Manifold : _String Interpolation_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings)<br>
[Manifold : _(Un)checked Exceptions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)<br>

[Manifold : _Preprocessor_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)<br>

[Manifold : _Science_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)<br>

[Manifold : _Collections_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)<br>
[Manifold : _I/0_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-io)<br>
[Manifold : _Text_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-text)<br>

>Experiment with sample projects:<br>
>* [Manifold : _Sample App_](https://github.com/manifold-systems/manifold-sample-project)<br>
>* [Manifold : _Sample SQL App_](https://github.com/manifold-systems/manifold-sql-sample-project)<br>
>* [Manifold : _Sample GraphQL App_](https://github.com/manifold-systems/manifold-sample-graphql-app)<br>
>* [Manifold : _Sample REST API App_](https://github.com/manifold-systems/manifold-sample-rest-api)<br>
>* [Manifold : _Sample Web App_](https://github.com/manifold-systems/manifold-sample-web-app)
>* [Manifold : _Gradle Example Project_](https://github.com/manifold-systems/manifold-simple-gradle-project)
>* [Manifold : _Sample Kotlin App_](https://github.com/manifold-systems/manifold-sample-kotlin-app)

---

# Platforms

Manifold supports:
* Java SE (8 - 26)
* [Android](http://manifold.systems/android.html)
* [Kotlin](http://manifold.systems/kotlin.html) (limited)

Comprehensive IDE support is also available for IntelliJ IDEA and Android Studio.

---

# [Chat](https://discord.gg/9x2pCPAASn)
Join our [Discord server](https://discord.gg/9x2pCPAASn) to start
a discussion, ask questions, provide feedback, etc. Someone is usually there to help.

<br>
