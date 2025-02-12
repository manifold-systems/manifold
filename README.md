<br>

<img width="500" height="121" align="top" src="./docs/images/manifold_green.png">

![latest](https://img.shields.io/badge/latest-v2024.1.55-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

---

## What is Manifold?
Manifold is a Java compiler plugin. Use it to supplement your Java projects with highly productive features.

Advanced compile-time <b>metaprogramming</b> type-safely integrates any kind of data, metadata, or DSL directly into Java.
* [SQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql/readme.md) _**(New!)**_
* [GraphQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
* [JSON & JSON Schema](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
  [YAML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml),
  [XML](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml)
* [CSV](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv)
* [JavaScript](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)
* etc.


Powerful **language enhancements** significantly improve developer productivity.
* [Extension methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
* [_True_ delegation](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-delegation)
* [Properties](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)
* [Optional parameters](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-params) _**(New!)**_
* [Tuple expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple)
* [Operator overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
* [Unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
* [A *Java* template engine](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)
* [A preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)
* ...and more

Each feature is available as a separate dependency. Simply add the Manifold dependencies of your choosing to your existing project and begin taking advantage of them.

All fully supported in JDK LTS releases 8 - 21 + latest with comprehensive IDE support in **IntelliJ IDEA** and **Android Studio**.

># _**What's New...**_
> 
>[<img width="40%" height="40%" align="top" src="./docs/images/manifoldsql.png">](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql/readme.md)
>
>### [Type-safe SQL](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql/readme.md)
> Manifold SQL lets you write native SQL _directly_ and _type-safely_ in your Java code.
>- Query types are instantly available as you type native SQL of any complexity in your Java code
>- Schema types are automatically derived from your database, providing type-safe CRUD, decoupled TX, and more
>- No ORM, No DSL, No wiring, and No code generation build steps
> <br><br>
> [![img_3.png](./docs/images/img_3.png)](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql/readme.md)
                       
## Who is using Manifold?

Sampling of companies using Manifold:

<img width="80%" height="80%" src="./docs/images/companies.png">

## What can you do with Manifold?

### [Meta-programming](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)
Use the framework to gain direct, type-safe access to *any* type of resource, such as
[**SQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-sql),
[**JSON**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
[**GraphQL**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql),
[**XML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml),
[**YAML**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml),
[**CSV**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv), and even
other languages such as [**JavaScript**](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js).
Remove the code gen step in your build process. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/graphql.mp4)

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

### [Extension Methods](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
Add your own methods to existing Java classes, even *String*, *List*, and *File*. Eliminate boilerplate code.
[&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/ExtensionMethod.mp4)
```java
String greeting = "hello";
greeting.myMethod(); // Add your own methods to String!
```

### [Delegation](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-delegation)
Favor composition over inheritance. Use `@link` and `@part` for automatic interface implementation forwarding and _true_ delegation.
> ```java
> class MyClass implements MyInterface {
>   @link MyInterface myInterface; // transfers calls on MyInterface to myInterface
>
>   public MyClass(MyInterface myInterface) {
>     this.myInterface = myInterface; // dynamically configure behavior
>   }
>
>   // No need to implement MyInterface here, but you can override myInterface as needed
> }
> ```

### [Properties](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)
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

### [Optional parameters & named arguments](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-params)
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

### [Operator Overloading](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#operator-overloading)
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

### [Tuple expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple)
Tuple expressions provide concise syntax to group named data items in a lightweight structure.
```java
var t = (name: "Bob", age: "35");
System.out.println("Name: " + t.name + " Age: " + t.age);

var t = (person.name, person.age);
System.out.println("Name: " + t.name + " Age: " + t.age);
```
You can also use tuples with new [`auto` type inference](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-inference-with-auto) to enable multiple return values from a method.
### [Unit Expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions)
Unit or *binding* operations are unique to the Manifold framework. They provide a powerfully concise syntax and can be
applied to a wide range of applications.
```java
import static manifold.science.util.UnitConstants.*; // kg, m, s, ft, etc
...
Length distance = 100 mph * 3 hr;
Force f = 5.2 kg m/s/s; // same as 5.2 N
Mass infant = 9 lb + 8.71 oz;
```  

### [Ranges](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections#ranges)
Easily work with the *Range* API using [unit expressions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#unit-expressions).
Simply import the *RangeFun* constants to create ranges.
```java
// imports the `to`, `step`, and other "binding" constants
import static manifold.collections.api.range.RangeFun.*;
...
for (int i: 1 to 5) {
  out.println(i);
}

for (Mass m: 0kg to 10kg step 22r unit g) {
  out.println(m);
}
```

### [Science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
Use the [manifold-science](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)
framework to type-safely incorporate units and precise measurements into your applications.
```java
import static manifold.science.util.UnitConstants.*; // kg, m, s, ft, etc.
...
Velocity rate = 65mph;
Time time = 1min + 3.7sec;
Length distance = rate * time;
```  

### [Preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)
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

### [Structural Typing](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
Unify disparate APIs. Bridge software components you do not control. Access maps through type-safe interfaces. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/structural%20typing.mp4)
```java
Map<String, Object> map = new HashMap<>();
MyThingInterface thing = (MyThingInterface) map; // O_o
thing.setFoo(new Foo());
Foo foo = thing.getFoo();
out.println(thing.getClass()); // prints "java.util.HashMap"
```
 
### [Type-safe Reflection](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#type-safe-reflection-via-jailbreak)
Access private features with <b>@Jailbreak</b> to avoid the drudgery and vulnerability of Java reflection. [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/jailbreak.mp4) 
```java
@Jailbreak Foo foo = new Foo();
// Direct, *type-safe* access to *all* foo's members
foo.privateMethod(x, y, z);
foo.privateField = value;
```

### [Checked Exception Handling](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)
You now have an option to make checked exceptions behave like unchecked exceptions!  No more unintended exception
swallowing. No more *try*/*catch*/*wrap*/*rethrow* boilerplate!
```java
List<String> strings = ...;
List<URL> urls = strings.stream()
  .map(URL::new) // No need to handle the MalformedURLException!
  .collect(Collectors.toList());
```

### [String Templates](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings)
Inline variables and expressions in String literals, no more clunky string concat! [&nbsp;**▶**&nbsp;Check&nbsp;it&nbsp;out!](http://manifold.systems/images/string_interpolation.mp4)
```java
int hour = 15;
// Simple variable access with '$'
String result = "The hour is $hour"; // Yes!!!
// Use expressions with '${}'
result = "It is ${hour > 12 ? hour-12 : hour} o'clock";
``` 
       
### [A *Java* Template Engine](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)
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

## [IDE Support](https://github.com/manifold-systems/manifold)
Use the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) to fully leverage
Manifold with **IntelliJ IDEA** and **Android Studio**. The plugin provides comprehensive support for Manifold including code
completion, navigation, usage searching, refactoring, incremental compilation, hotswap debugging, full-featured
template editing, integrated preprocessor, and more.

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="manifold ij plugin" width="60%" height="60%"/></p>

[Get the plugin from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/10057-manifold)

## [Projects](https://github.com/manifold-systems/manifold)
The Manifold project consists of the core Manifold framework and a collection of sub-projects implementing SPIs provided
by the core framework. Each project consists of one or more **dependencies** you can easily add to your project:

[Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)<br>

[Manifold : _Extensions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)<br>

[Manifold : _Delegation_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-delegation)<br>

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

## Platforms

Manifold supports:
* Java SE (8 - 21)
* [Android](http://manifold.systems/android.html)
* [Kotlin](http://manifold.systems/kotlin.html) (limited)

Comprehensive IDE support is also available for IntelliJ IDEA and Android Studio.

## [Chat](https://discord.gg/9x2pCPAASn)
Join our [Discord server](https://discord.gg/9x2pCPAASn) to start
a discussion, ask questions, provide feedback, etc. Someone is usually there to help.

<br>
