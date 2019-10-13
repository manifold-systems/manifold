![manifold framwork](http://manifold.systems/images/manifold_framework.png)

# Manifold : Core

The core framework plugs directly into the Java compiler via the [Javac plugin API](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.compiler/com/sun/source/util/Plugin.html)
as a universal *type* adapter to allow for a direct and seamless supply of types and features otherwise inaccessible to
Java's type system. As such the Manifold core framework provide's a foundation and plugin SPI to dynamically resolve
type names and produce corresponding Java sources, and to more generally augment Java's type system. Such a plugin is
called a *type manifold* and implements the `ITypeManifold` SPI. 

## Table of Contents
* [The Big Picture](#the-big-picture)
* [The API](#the-api)
* [Anatomy of a Type Manifold](#anatomy-of-a-type-manifold)
* [Modes](#modes)
* [Embedding with _Fragments_ (experimental)](#embedding-with-fragments-experimental)
* [Using `@Precompile`](#using-precompile)
* [IDE Support](#ide-support)
* [Building](#building)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)


# The Big Picture

You can think of a type manifold as a **_just-in-time_ code generator**. Essentially the Manifold framework plugs in and
overrides the compiler's type resolver so that, via the `ITypeManifold` SPI, a type manifold can claim ownership of
type names as the compiler encounters them and dynamically provide source code corresponding with the types. As a
consequence this core functionality serves as a productive alternative to conventional code generation techniques, a
long overdue advancement for static typing. 

To begin with, because the framework plugs directly into the compiler, a code generator as a type manifold *is no longer
a separate build step*. What's more a type manifold generates code on-demand as the compiler asks for types. Not only
does this significantly reduce the complexity of code generators, it also enables them to function *incrementally*. This
means resources on which the generated sources are based can be edited and only the types corresponding with the changes
will be regenerated and recompiled. Thus, contrary to conventional code generators, type manifolds:
* require **_zero_ build steps**
* produce **_zero_ on-disk source code**
* are by definition **always in sync** with resources
* are **inherently incremental** resulting in **optimal build times** 
* are **dead simple to use** - just add a dependency to your project

Moreover, type manifolds can *cooperate* and contribute source to types in different ways. Most often a type manifold
registers as a *primary* contributor to supply the main body of the type. The [JSON type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
for example, is a *primary* contributor because it supplies the full type definition corresponding with a JSON Schema
file or sample file. Alternatively, a type manifold can be a *partial* or *supplementary* contributor. For instance, the
[Extension type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
is a *supplementary* contributor because it augments an existing type with additional methods, interfaces, and other
features.  Thus both the JSON and Extension type manifolds can contribute to the same type, where the JSON manifold
supplies the main body and the Extension type manifold contributes custom methods and other features provided by [extension classes](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext##extension-classes-via-extension).
As a consequence any number of type manifolds can operate in concert to form an efficient and powerful type building
pipeline, thus unlike conventional code generators, type manifolds:
* can easily **have dependencies on one another**
* can easily **contribute to one another's types**
* are **customizable with _extensions_**

Finally, the Manifold core framework can be used from IDEs and other tooling to provide consistent, unified access to
the types and features produced from all type manifolds. For instance, the [Manifold plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold)
provides comprehensive support for the Manifold framework. All types and features produced from type manifolds and other
Manifold SPIs are fully supported. You can edit resources such as JSON and GraphQL files and immediately use the changes
in Java without a compilation step. Features like code completion, resource/code navigation, deterministic usage
searching, refactoring/renaming, incremental compilation, hotswap debugging, etc. work seamlessly with *all* type
manifolds past, present, and future. This represents an unimaginable leap in productivity compared with the conventional
code generation world where the burden is on the code generator author or third party to invest in one-off IDE tooling
projects, which typically results in poor or no IDE representation. Thus another big advantage type manifolds possess
over conventional code generators is:
* a **unified framework**...
* ...which enables **comprehensive IDE support** and more

To summarize, the Manifold framework provides a clear advantage over conventional code generation techniques. Type
manifolds do not entail build steps, are always in sync, operate incrementally, and are simple to add to any project.
They also cooperate naturally to form a powerful type building pipeline, which via the core framework is uniformly
accessible to IDEs such as IntelliJ IDEA. Putting it all together, the synergy resulting from these improvements has the
potential to significantly increase Java developer productivity and to open minds to new possibilities. 

# The API

The framework consists of the several SPIs:

* [ITypeManifold]() This SPI is the basis for implementing a _type manifold_. See existing type manifold projects such as [`manifold-graphql`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql). 
* [ICompilerComponent]() Implement this low-level SPI to supplement Java with new or enhanced behavior e.g., [`manifold-strings`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings) and [`manifold-exceptions`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions).
* [IPreprocessor]() Implement this SPI to provide a _preprocessor_ to filter source before it enters Java's parser e.g., [`manifold-preprocessor`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor).
* [IProxyFactory]() This SPI addresses _structural interface_ performance; you implement this to provide a compile-time
proxy for a specific structural interface.

# Anatomy of a Type Manifold

Almost any data source is a potential type manifold.  These include file schemas, query languages, database definitions, 
data services, templates, spreadsheets, programming languages, and more.  So while the Manifold team provides several 
type manifolds out of the box the domain of possible type manifolds is virtually unlimited.  Importantly, their is 
nothing special about the ones we provide -- you can build type manifolds using the same public API with which ours
are built.

The API is comprehensive and aims to fulfill the 80/20 rule -- the common use-cases are straightforward to implement,
but the API is flexible enough to achieve almost any kind of type manifold. For instance, since most type manifolds are
resource file based the API foundation classes handle most of the tedium with respect to file management, caching, and
modeling. Also, since the primary responsibility for a type manifold is to dynamically produce Java source, Manifold
provides a simple API for building and rendering Java classes. But the API is flexible so you can use other tooling as
you prefer.

Most resource file based type manifolds consist of three basic classes:
* `JavaTypeManifold` subclass
* A class to produce Java source 
* `Model` subclass
 
The `Image` manifold is relatively simple and nicely illustrates this structure:


**JavaTypeManifold Subclass**
```java
public class ImageTypeManifold extends JavaTypeManifold<Model> {
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "png", "bmp", "wbmp", "gif"));

  @Override
  public void init(IModule module) {
    init(module, Model::new);
  }

  @Override
  public boolean handlesFileExtension(String fileExtension) {
    return FILE_EXTENSIONS.contains(fileExtension.toLowerCase());
  }

  @Override
  protected String aliasFqn(String fqn, IFile file) {
    return fqn + '_' + file.getExtension();
  }

  @Override
  protected boolean isInnerType(String topLevel, String relativeInner) {
    return false;
  }

  @Override
  protected String produce(String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler) {
    SrcClass srcClass = new ImageCodeGen(model._url, topLevelFqn).make();
    StringBuilder sb = srcClass.render(new StringBuilder(), 0);
    return sb.toString();
  }
}
```
Like most type manifolds the image manifold is file extension based, specifically it handles the domain of files having 
image extensions: jpg, png, etc.  As you'll see, the `JavaTypeManifold` base class is built to handle this use-case.
First, `ImageTypeManifold` overrides the `init()` method to supply the base class with its `Model`.  We'll cover
that shortly.  Next, it overrides `handlesFileExtension()` to tell the base class which file extensions it handles.
Next, since the image manifold produces classes with a slightly different name than the base file name, it overrides 
`aliasFqn()` to provide an alias for the qualified name of the form `<package>.<image-name>_<ext>`.  The name must
match the class name the image manifold produces. There are no inner classes produced by this manifold, therefore
it overrides `isInnerType()` returning false; the base class must ask the subclass to resolve inner types.  Finally,
the image manifold overrides `contribute()`, this is where you contribute Java source for a specified class name.


**Source Production Class**

Most often, you'll want to create a separate class to handle the production of Java source.  The image manifold does 
that with `ImageCodeGen`:

```java
public class ImageCodeGen {
  private final String _fqn;
  private final String _url;

  ImageCodeGen(String url, String topLevelFqn) {
    _url = url;
    _fqn = topLevelFqn;
  }

  public SrcClass make() {
    String simpleName = ManClassUtil.getShortClassName(_fqn);
    return new SrcClass(_fqn, SrcClass.Kind.Class).imports(URL.class, SourcePosition.class)
      .superClass(new SrcType(ImageIcon.class))
      .addField(new SrcField("INSTANCE", simpleName).modifiers(Modifier.STATIC))
      .addConstructor(new SrcConstructor()
        .addParam(new SrcParameter("url")
          .type(URL.class))
        .modifiers(Modifier.PRIVATE)
        .body(new SrcStatementBlock()
          .addStatement(new SrcRawStatement()
            .rawText("super(url);"))
          .addStatement(new SrcRawStatement()
            .rawText("INSTANCE = this;"))))
      .addMethod(new SrcMethod().modifiers(Modifier.STATIC)
        .name("get")
        .returns(simpleName)
        .body(new SrcStatementBlock()
          .addStatement(
            new SrcRawStatement()
              .rawText("try {")
              .rawText("  return INSTANCE != null ? INSTANCE : new " + simpleName + 
                "(new URL("\\" + ManEscapeUtil.escapeForJavaStringLiteral(_url) + "\\"));")
              .rawText("} catch(Exception e) {")
              .rawText("  throw new RuntimeException(e);")
              .rawText("}"))));
  }
}
```

Here the image manifold utilizes `SrcClass` to build a Java source model of image classes.  `SrcClass` is a
source code production utility in the Manifold API.  It's simple and handles basic code generation use-cases.
Feel free to use other Java source code generation tooling if `SrcClass` does not suit your use-case, because
ultimately you're only job here is to produce a `String` consisting of Java source for your class.


**Model Subclass**

The third and final class the image manifold provides is the `Model` class:

```java
class Model extends AbstractSingleFileModel {
  String _url;

  Model(String fqn, Set<IFile> files) {
    super(fqn, files);
    assignUrl();
  }

  private void assignUrl() {
    try {
      _url = getFile().toURI().toURL().toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getUrl() {
    return _url;
  }

  @Override
  public void updateFile(IFile file) {
    super.updateFile(file);
    assignUrl();
  }
}
```

This class models the image data necessary for `ImageCodeGen` to produce source as a `AbstractSingleFileModel` subclass. 
In this case the model data is simply the URL for the image. Additionally, `Model` overrides `updateFile()` to keep the
URL up to date in environments where it can change, such as in an IDE.


**Registration**

In order to use a type manifold in your project, it must be registered as a service.  Normally, as a type
manifold provider to save users of your manifold from this step, you self-register your manifold in your 
META-INF directly like so:
```
src
-- main
---- resources
------ META-INF
-------- services
---------- manifold.api.type.ITypeManifold
```
Following standard Java [ServiceLoader protocol](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html)
you create a text file called `manifold.api.type.ITypeManifold` in the `service` directory under your `META-INF` directory.
The file should contain the fully qualified name of your type manifold class (the one that implements `ITypeManifold`) followed
by a new blank line:
```
com.abc.MyTypeManifold

```

As you can see building a type manifold can be relatively simple. The image manifold illustrates the basic structure of
most file-based manifolds. Of course there's much more to the API. Examine the source code for other manifolds such as
the GraphQL manifold ([manifold-graphql](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql))
and the JavaScript manifold ([manifold-js](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)).
These serve as decent reference implementations for wrapping parsers and binding to existing languages.

>Note with Java 9+ with named modules you register a service provider in your `module-info.java` file using the
`provides` keyword:
>```java
>provides manifold.api.type.ITypeManifold with com.abc.MyTypeManifold
>```

# Modes

You can use Manifold in one of two modes which you control as an optional argument to the Manifold plugin for javac:

* **static**: `-Xplugin:Manifold` (default) compiles resource types statically at compile-time

* **dynamic**: `-Xplugin:Manifold dynamic` compiles resource types _dynamically_ at _runtime_
(alternatively `-Xplugin:"Manifold dynamic"`, some tools may require quotes)

Most projects benefit most using the default (static) mode. Dynamic mode in most cases should be reserved for specific
type manifolds that are better suited to dynamic compilation.

> Note if you're not sure which mode to use, try the default static mode -- it's usually the right choice.

General information considering the static v. dynamic mode:

* Both modes operate _lazily_: a type is not compiled unless it is used. For example, if you are using the [JSON manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json), 
only the JSON files you reference in your code will be processed and compiled. This means Manifold will not try to
compile resources your project does not expect to use directly as types.

* Even if you use static mode, you can still reference type manifold classes dynamically e.g., _reflectively_.
In such a case Manifold will dynamically compile the referenced class as if you were operating in dynamic mode. In
general, your code will work regardless of the mode you're using, hence the general recommendation to stay with static
mode where you get the best of both worlds. 

* Dynamic mode requires `tools.jar` at runtime for **Java 8**.  Note tools.jar may still be required with static mode,
depending on the Manifold features you use.  For example, if you use [structural interfaces](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#structural-interfaces-via-structural)
your project _may_ require tools.jar, regardless of mode. This largely depends on whether or not the an [`IProxyFactory`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext#implementation-by-proxy)
implementations exist for the structural interfaces involved.

* Static mode is generally faster at runtime since it pre-compiles all the type manifold resources along with Java 
sources when you build your project

* Static mode automatically supports **incremental compilation** and **hotswap debugging** of modified resources in IntelliJ
   
> Note, you can use `@Precompile` to instruct the Java compiler to compile a set of specified types regardless of 
whether or not you use them directly in your code e.g., if your code is an API.  See [Using @Precompile](#using-precompile).
 

# Embedding with Fragments (experimental)

You can now *embed* resource content such as JSON, GraphQL, YAML, etc. directly in a Java source file as a type-safe
resource _**fragment**_.  A fragment has the same format and grammar as a resource file and, if used with the Manifold
IDE plugin, can be authored with rich editor features like code highlighting, parser feedback, code completion, etc.
This means you can directly embed resources closer to where you use them in your code.  For instance, you can
type-safely write a query in the query language your application uses directly in the Java method that uses the query.

You can embed a fragment as a *declaration* or a *value*.

## Type Declaration

You can type-safely embed resources directly in your Java code as declarations.  Here's a simple example using
Javascript:
```java
public class MyJavaClass {
  void foo() {
    /*[>Barker.js<]
    function callBark(aBarker) {
      aBarker.bark();
    }
    */
    Barker.callBark(new Dog());
    Barker.callBark(new Tree());
    class Dog {
      public String bark() {
        return "ruff";
      }
    }
    class Tree {
      public String bark() {
        return "rough";
      }
    }
  }
}
``` 
Notice the Javascript is embedded in a multiline comment. This is how you embed any kind of resource fragment as a type
declaration.  Here the Javascript type is declared as `Barker` with a `.js` extension indicating the resource type. Note
a fragment must use `[>` and `<]` at the beginning of the comment to delimit the type name and extension.  A fragment
covers the remainder of the comment and must follow the format of the declared extension.

You can embed any Manifold enabled resource as a fragment type declaration.  Here's another example using JSON:
```java
void foo() {
  /*[>Planet.json<]
  {
    "name": "Earth",
    "system": {
      "name": "Sol",
      "mass": 1.0014
    }
  }
  */
  // Work with the content type-safely
  Planet planet = Planet.fromSource();
  String name = planet.getName();
  Planet.system sys = planet.getSystem();
  
  // Make a REST call
  Planet.request(endpoint).postOne(planet);
  
  // Use the JSON bindings 
  Map<String, Object> jsonBindings = planet.getBindings();
  
  // Make a new Planet
  Planet mars = Planet.builder()
  .withName("Mars")
  .withSystem(sys)
  .build();
  
  // Transform to another format
  String yaml = planet.writer().toYaml();
}
```
 
## Scoping 
A fragment can be embedded anywhere in your code.  The type declared in the fragment is scoped to the package of the
enclosing class.  Thus in the example `Barker` is accessible anywhere in the enclosing `foo` method *as well as* foo's
declaring class and other classes in its package.

>Note, even though the declared type is package scoped, for the sake of readability it is best to define the fragment
nearest to its intended use. In a future release this level of scoping may be enforced.

## Rich Editing
If used with the Manifold IntelliJ IDEA plugin, you can edit fragments as if they were in a separate file with all the
editor features you'd expect like highlighting, parser feedback, code completion, etc.  This is especially useful with
GraphQL, SQL, and similar resources where editing a fragment in place provides a more fluid development experience. 
 
## Value Fragments (experimental)
Sometimes it's more convenient to use a fragment as a *value* as opposed to a type declaration. For example, you can
create a GraphQL query as a fragment value and assign it to a variable:
 
```java 
var moviesByGenre = "[>.graphql<] query MoviesByGenre($genre: genre) { movies(genre: $genre) { title } }";
var query = moviesByGenre.builder().withGenre(Action).build();
var actionMovies = query.request(ENDPOINT).post();
``` 

Here a GraphQL query is embedded directly in a String literal as a fragment *value*.  The resulting type is based on
the fragment type in use.  In this case the GraphQL type manifold provides a special type with the single purpose of
exposing a query `builder` method matching the one the `MoviesByGenre` query defines.

Note not all manifold resources can be used as fragment values. The fragment value concept is not always a good fit.
For instance, the Properties manifold does not implement fragment values because a properties type is used statically.

Note fragments as values will become more useful with multiline String literals via the new
[Text Blocks](https://openjdk.java.net/jeps/355) feature in Java 13:
```java
var query = """
  [>.graphql<]
  query Movies($genre: Genre!, $title: String, $releaseDate: Date) {
    movies(genre: $genre, title: $title, releaseDate: $releaseDate) {
      id
      title
      genre
      releaseDate
    }
  }
  """;
var result = query.create(Action).request(ENDPOINT).post();
```    

>**Note to Type Manifold service providers**
>
>To support fragments as *values* you must annotate your toplevel types with `@FragmentValue`.
This annotation defines two parameters: `methodName` and `type`, where `methodName` specifies the name of a static 
method to call on the top-level type that represents the type's value, and where `type` is the qualified name of the
value type, which must be contravariant with the `methodName` return type. See the 
[GraphQL type manifold implementation](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-graphql/src/main/java/manifold/graphql/type/GqlParentType.java)
for a reference.


# Using `@Precompile`

By default a Type Manifold compiles a resource type only if you use it somewhere in your code.  Normally this is 
desirable because if you don't use it as a Java class, why compile it?  There are cases, however, where *your*
code may not be the only code that potentially uses the resources.  For instance, if your project provides an API
in terms of JSON Schema files, there's a good chance your project doesn't use the JSON directly -- but consumers of your API do.
A similar case involves a mutli-module Java 11 project where a module provides resource files, but only dependent modules
use them as Manifold types.  Although Manifold works in both of these situations, it compiles the types dynamically,
which entails a one time performance bump the first time each class is used at runtime. For cases like these you can
avoid dynamic compilation using the `@Precompile` annotation.

You can annotate any class in your project/module with `@Precompile`. For example, if you are using the JSON manifold,
you can instruct the Java compiler to compile all `.json` files regardless of whether or not your module uses them as
types:
```java
@Precompile(fileExtension = "json")
public class Main {
  ...
}
```

You can refine `@Precompile` to compile only files matching a regex pattern:
```java
@Precompile(fileExtension = "yml", typeNames = "com.abc.(My)+")
```
This tells the compiler to precompile YAML files in package `com.abc` starting with `"My"`.

You can also specify the type manifold class.  This example is logically the same as the previous one:
```java
@Precompile(typeManifold = YamlTypeManifold.class, typeNames = "com.abc.(My)+")
```

You can also stack `@Precompile`:
```java
@Precompile(fileExtension = "json", typeNames = "com.abc.(My)+")
@Precompile(fileExtension = "yml", typeNames = "com.abc.(My)+")
```
This tells the compiler to precompile all JSON and YAML files in package `com.abc` starting with `"My"`.

Finally, an easy way to tell the Java compiler to compile *all* the files corresponding with all the type manifolds
enabled in your module:
```java
@Precompile
```

# IDE Support

Use the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) for IntelliJ IDEA to really boost your
productivity. 

# Projects
The Manifold framework consists of the core project and a collection of sub-projects implementing SPIs provided
by the core. Each project represents a separate **dependency** you can easily add to your project:

[Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)<br>

[Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)<br>
[Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)<br>
[Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)<br>
[Manifold : _Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)<br>
[Manifold : _Image_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)<br>
[Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)<br>
[Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)<br>

[Manifold : _Java Extension_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)<br>

[Manifold : _Templates (ManTL)_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)<br>

[Manifold : _String Interpolation_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings)<br>
[Manifold : _[Un]checked Exceptions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)<br>

[Manifold : _Preprocessor_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)<br>

[Manifold : _Collections_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)<br>
[Manifold : _I/0_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-io)<br>
[Manifold : _Text_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-text)<br>

[Manifold : _All&nbsp;&nbsp;(Über jar)_](https://github.com/manifold-systems/manifold/tree/master/manifold-all)<br>

# Sample Projects
Use the sample projects for nice working examples of how to configure and use Manifold in your project.  
* [Manifold : _Sample App_](https://github.com/manifold-systems/manifold-sample-project)<br>
* [Manifold : _Sample GraphQL App_](https://github.com/manifold-systems/manifold-sample-graphql-app)<br>
* [Manifold : _Sample REST API App_](https://github.com/manifold-systems/manifold-sample-rest-api)<br>
* [Manifold : _Sample Web App_](https://github.com/manifold-systems/manifold-sample-web-app)

# Building

## Building this project

The `manifold` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold` core dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 12.

Here are some sample build configurations references.

>Note you can replace the `manifold` core dependency with [`manifold-all`](https://github.com/manifold-systems/manifold/tree/master/manifold-all) as a quick way to gain access to all of
Manifold's features.

## Gradle

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 12), the script takes care of the rest. 
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
    compile group: 'systems.manifold', name: 'manifold', version: '2019.1.18'
    testCompile group: 'junit', name: 'junit', version: '4.12'

    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold', version: '2019.1.18'
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

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-manifold-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Manifold App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.18</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold</artifactId>
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
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-manifold-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Manifold App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2019.1.18</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold</artifactId>
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
                            <artifactId>manifold</artifactId>
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
