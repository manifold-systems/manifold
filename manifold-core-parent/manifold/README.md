# Manifold : Core

The core framework plugs directly into the Java compiler via the [Javac plugin API](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.compiler/com/sun/source/util/Plugin.html)
as a universal *type* adapter to allow for a direct and seamless supply of types and features otherwise inaccessible to
Java's type system. The core library provides a foundation and plugin SPI to dynamically resolve type names and produce
Java sources on demand, and to more generally augment Java's type system. A plugin implementing the SPI is called a *type manifold*.

A type manifold can provide new types corresponding with any type of structured data such as JSON, XML, GraphQL, and even
languages like JavaScript. Additionally, a type manifold can supplement existing types with additional methods, properties,
interfaces, annotations, etc.

## Table of Contents
* [The Big Picture](#the-big-picture)
* [The API](#the-api)
* [Anatomy of a Type Manifold](#anatomy-of-a-type-manifold)
* [Configuring Manifold](#configuring-manifold)
* [Explicit Resource Compilation](#explicit-resource-compilation)
* [Modes](#modes)
* [Programming Language Manifolds](#programming-language-manifolds)
* [Embedding with _Fragments_ (experimental)](#embedding-with-fragments-experimental)
* [IDE Support](#ide-support)
* [Projects](#projects)
* [Sample Projects](#sample-projects)
* [Setup](#setup)
* [Platforms](#platforms)
* [Javadoc](#javadoc)
* [License](#license)
* [Versioning](#versioning)
* [Author](#author)


# The Big Picture

You can think of a type manifold as a **_just-in-time_ code generator**. Essentially, the Manifold framework plugs in and
overrides the compiler's type resolver so that, via the `ITypeManifold` SPI, a type manifold can claim ownership of
type names as the compiler encounters them and dynamically provide source code corresponding with the types. This core functionality
serves as a productive alternative to conventional code generation techniques. 

Because the framework plugs directly into the compiler, a code generator written as a type manifold *is no longer
a separate build step*, it generates code on-demand as the compiler asks for types. This significantly reduces
the complexity of code generation and enables it to function *incrementally*. Thus, contrary to conventional code generation
techniques, type manifolds:
* require **_zero_ build steps**
* produce **_zero_ on-disk source code** (but can if desired)
* are by definition **always in sync** with resources
* are **inherently incremental** resulting in **optimal build times** 
* are **dead simple to use** - just add a dependency to your project

Additionally, type manifolds can cooperate and contribute source/types in different ways. Most often a type manifold
registers as a *primary* contributor to supply the main body of the type. The [JSON type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json),
for example, is a *primary* contributor because it supplies the full type definition corresponding with a JSON Schema
file or sample file. Alternatively, a type manifold can be a *partial* or *supplementary* contributor. For instance, the
[Extension type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)
is a *supplementary* contributor because it augments an existing type with additional methods, interfaces, and other
features.  Thus, both the JSON and Extension type manifolds can contribute to the same type, where the JSON manifold
supplies the main body and the Extension type manifold contributes custom methods and other features provided by [extension classes](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext##extension-classes-via-extension).
As a consequence any number of type manifolds can operate in concert to form an efficient and powerful type building
pipeline, therefore, unlike conventional code generators, type manifolds:
* can easily **cooperate**
* are inherently **extensible**

The core framework can also be used from IDEs and other tooling to provide consistent, unified access to
the types and features produced from all type manifolds. For instance, the [Manifold plugin for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/10057-manifold)
provides comprehensive support for the Manifold framework. All types and features produced from type manifolds and other
Manifold SPIs are fully supported. You can edit resources such as JSON and GraphQL files and immediately use the changes
in Java without a compilation step. Features like code completion, resource/code navigation, deterministic usage
searching, refactoring/renaming, incremental compilation, hotswap debugging, etc. work seamlessly with *all* type
manifolds past, present, and future. This represents a tremendous leap in productivity compared with the conventional
code generation world where the burden is on the code generator author or third party to invest in one-off IDE tooling
projects, which typically results in poor or no IDE representation. Thus another big advantage type manifolds possess
over conventional code generators is:
* a **unified framework**...
* ...which enables **comprehensive IDE support** and more

To summarize, the Manifold framework provides a clear advantage over conventional code generation techniques. Type
manifolds do not entail build steps, are always in sync, operate incrementally, and are simple to add to any project.
They also cooperate naturally to form a powerful type building pipeline, which via the core framework is uniformly
accessible to IDEs such as IntelliJ IDEA and Android Studio. Putting it all together, the synergy resulting from these improvements has the
potential to significantly increase Java developer productivity and to open minds to new possibilities. 

# The API

The framework consists of the several SPIs:

* [ITypeManifold]() This SPI is the basis for implementing a _type manifold_. See existing type manifold projects such as [`manifold-graphql`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql). 
* [ICompilerComponent]() Implement this low-level SPI to supplement Java with new or enhanced behavior e.g., [`manifold-strings`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings) and [`manifold-exceptions`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions).
* [IPreprocessor]() Implement this SPI to provide a _preprocessor_ to filter source before it enters Java's parser e.g., [`manifold-preprocessor`](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor).

# Anatomy of a Type Manifold

Any data resource is a potential type manifold.  These include file schemas, query languages, database definitions, 
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
ultimately your only job here is to produce a `String` consisting of Java source for your class.


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

>Note, with Java 9+ with named modules you register a service provider in your `module-info.java` file using the
`provides` keyword:
>```java
>provides manifold.api.type.ITypeManifold with com.abc.MyTypeManifold
>```

# Configuring Manifold

There are three ways you can configure Manifold in your build: *static*, *dynamic*, and *mixed*. Each configuration type
is determined by the following criteria:
* Use of `compileOnly` (or `provided`) scoping on *all* Manifold dependencies your project uses that are specific to
compilation
* Use of the `dynamic` argument to the Manifold javac plugin e.g., `-Xplugin:Manifold` vs. `-Xplugin:Manifold dynamic`

|                      |`static` |`dynamic`|`mixed`  |
|----------------------|---------|---------|---------|
| `compileOnly` scope  | &#10003;|         |         |
| `dynamic` plugin arg |         | &#10003;|         |

>`static` is the recommended configuration for most projects. Using it results in smaller, faster, more versatile
>projects.

* **`static`**: Compiles all resource types statically at compile-time. Distributes only Manifold runtime dependencies.

    **Advantages**:
    * **Performance**. Since resource types are compiled ahead of time as .class files, load time for a resource type
    is the same a Java type. 
    * **Zero startup time**. The dynamic compilation subsystem and other services exclusive to compilation are not
    present at runtime, therefore Manifold has ZERO impact on startup time.  
    * **Footprint**. Since only runtime modules are distributed in this configuration, the resulting runtime footprint is more
    than 10x smaller than dynamic and mixed configurations, which translates to faster downloads.
    * **Android**. Applications using Manifold statically fully support Android. 
    * **Kotlin**. Applications using Manifold statically fully support Kotlin and other JVM languages. 
    
    **Limitations**:
    * Dynamic compilation (compilation at runtime) is not supported. Usage of dynamic features such as Dark Java can't be used. Considering Dark Java is the only remaining exclusively dynamic feature of Manifold, this is a rather minor limitation. 
    
* **`dynamic`**: Compiles all resource types dynamically at runtime. No resource types are compiled to .class files
at compile-time. Both compile-time and runtime binaries are included in your distribution.

    **Advantages**:
    * **Dynamic**. Purely dynamic features such as Dark Java enable dynamic runtime compilation of resources.
     
    **Limitations**:
    * Slower startup and initialization. Manifold dynamic services contribute to a slower startup time. Additionally,
    initialization times for resource types are slower due to dynamic compilation on initial load. 
    * Larger footprint. Dynamic services used at compile-time are also distributed with the runtime, the additional
    files significantly impact the overall footprint (more than 10x). 
    * Limited use. Only Java SE environments are supported. Other JVM environments such as Android and Kotlin are not
     supported using this configuration. 
    
* **`mixed`**: Compiles project resource types statically such as resource files, and compiles dynamic resource types
dynamically such as Dark Java.  Both compile-time and runtime binaries are included in your distribution.

    **Advantages**:
    * **Dynamic**. Purely dynamic features such as Dark Java enable dynamic runtime compilation of resources.
    * **Performance**. Since resource types are compiled ahead of time as .class files, load time for a resource type
    is the same a Java type. 
    
    **Limitations**:
    * Slower startup and initialization. Manifold dynamic services contribute to a slower startup time. Additionally,
    initialization times for resource types are slower due to dynamic compilation on initial load. 
    * Larger footprint. Dynamic services used at compile-time are also distributed with the runtime, the additional
    files significantly impact the overall footprint (more than 10x). 
    * Limited use. Only Java SE environments are supported. Other JVM environments such as Android and Kotlin are not
     supported using this configuration.
      
>Configurations by supported features
>
> |                  |`static` |`dynamic`|`mixed` |
> |------------------|---------|---------|---------|
> | Performance      | &#10003;|         | &#10003;|
> | Small footprint  | &#10003;|         |         |
> | Zero startup time| &#10003;|         |         |
> | Java SE          | &#10003;| &#10003;| &#10003;|
> | Android          | &#10003;|         |         |
> | Kotlin**         | &#10003;|         |         |
> | Dynamic Java     |         | &#10003;| &#10003;|
>
>**All Manifold resource types such as GraphQL and JSON are fully supported with Kotlin, however Manifold Java
>Extension features such as @Jailbreak, unit expressions, and the preprocessor are specific to the Java compiler.  

## Configuring Dependencies

Manifold components are structured to support both static and dynamic use.  For instance, the Manifold _core_ component
consists of two modules: `manifold` and `manifold-rt`, where `manifold` contains all the compile-time functionality and
`manifold-rt` contains code exclusive to runtime APIs and internal runtime implementation. As such, to use the Manifold
core modules statically with your project you add a _compile-only_ dependency on `manifold` and a default dependency on
`manifold-rt`.

All of Manifold modules are designed in this way. As a result, components like Manifold core that provide both
compile-time and runtime functionality consist of two modules `manifold-xxx` and `manifold-xxx-rt`, whereby a
_compile-only_ scope is used on `manifold-xxx` and a default scope is used for `manifold-xxx-rt`.  A component that is
exclusive to compile-time or that does not provide any features exclusive to compile-time does not define a separate
"rt" module. For instance, the `manifold-preprocessor` is exclusive to compile-time use, therefore you always add it
with a _compile-only_ scope. Conversely, the `manifold-science` library does not define any features exclusive to
compile-time, so you always use it with default scoping so it is packaged with your executable artifact. 

### Static Configuration (default)

* Add the `-Xplugin:Manifold` javac argument
* If using Kotlin or other alternative JVM language, put Manifold resources in a separate Java compiled module and add
`-Amanifold.source.<file-ext>=<type-name-regex>` javac arguments to explicitly compile the resources. See
[Explicit Resource Compilation](#explicit-resource-compilation) below.
* Use _compile-only_ (Gradle) or _provided_ (Maven) scoping for Manifold dependencies exclusive to compile-time features
* Use default scoping for "rt" dependencies and any dependencies that are needed at runtime

See [Setup]() for examples of using Manifold statically.

>`static` is the recommended configuration for most projects. Using it results in smaller, faster, more versatile
>projects.
   
### Dynamic Configuration

* Add the `-Xplugin:Manifold dynamic` javac argument
* Use default scoping for Manifold dependencies, there is no need to add dependencies to "rt" modules.

See [Setup]() for examples of using Manifold dynamically.

### Mixed Configuration

* Add the `-Xplugin:Manifold` javac argument
* Use default scoping for Manifold dependencies, there is no need to add dependencies to "rt" modules.
* You can statically compile as much or as little as preferred.

See [Setup]() for examples using Manifold both statically and dynamically.

>Note, the only difference between dynamic and mixed configuration is the `dynamic` Manifold plugin argument, which
>prevents Manifold from compiling resource types to disk.
 
### Runtime Dependencies

If a project uses any manifold runtime dependencies ("rt" dependencies) by default manifold inserts a static block into
all your classes to automatically initialize some runtime services:
```java
static {
  IBootstrap.dasBoot();
}
``` 
The `dasBoot()` call invokes all registered `IBootstrap` services. The number and nature of the implementations
depends on what your application does with manifold. For instance, if you are using manifold *dynamically*, this call
initializes the dynamic compilation services among other tasks. Most projects, however, use manifold *statically* which
means `dasBoot()` typically does only the following:

1. Disables the Java 9 warning "An illegal reflective access operation has occurred", because that message has a history
of unnecessarily alarming users. This is a noop when running on Java 8 / Android.
2. Dynamically open the java.base module to the manifold module for common reflection access, which is a noop running on
Java 8 / Android

`dasBoot()` performs these tasks one time, subsequent calls simply return i.e., there is no performance penalty.

If you know your code will never run on Java 9+ and/or you don't mind the Java 9+ warning message, you can eliminate the
`dasBoot()` static initializer via the `no-bootstrap plugin` argument:
  
**Gradle**
```groovy
options.compilerArgs += ['-Xplugin:Manifold no-bootstrap']
```  
**Maven**
```xml
<compilerArgs>
    <arg>-Xplugin:Manifold no-bootstrap</arg>
</compilerArgs>
```
If you need finer grained control over which classes have the static block, you can use the `@NoBootstrap` annotation
to filter specific classes.

>Note, compile-only dependencies such as `manifold-preprocessor`, `manifold-exceptions`, and `manifold-strings` don't
>involve any runtime dependencies, thus if your project's exposure to manifold is limited to these dependencies, the
>static block is never inserted in any of your project's classes.
>
# Explicit Resource Compilation

By default, Manifold compiles resource types to disk _as the Java compiler encounters them in your code_. As a consequence,
a resource that is never used in your code as a type is not compiled. For example, if you have hundreds of JSON resource
files, but your project only uses a handful type-safely with Manifold, only the handful is compiled to disk as .class
files.

This scheme is unsuitable, however, for modules intended for use as a dependency or library where the set of resources that
needs to be compiled may include more than the ones used inside the module. For instance, an API that defines a query
model in terms of GraphQL files may not use any of the resource files directly, but a module using the API would. Although
Manifold can still work in situations like this (see [Modes](#modes) above), it does so by compiling types *dynamically* at
runtime, which entails a performance bump the first time each type loads. If need be, you can avoid the runtime compilation
cost using _explicit_ static compilation with `javac` command line options.

Similar to javac's source file list, Manifold provides `-Akey=value` javac command line options to explicitly compile
resources either by _type name_ using regular expressions or by _file name_ using file system paths. Constraining by
type name is the simplest and more flexible of the two, especially in terms of build systems such as Maven and Gradle.

>See the [Sample Kotlin App](https://github.com/manifold-systems/manifold-sample-kotlin-app) for an example of using
>explicit resource compilation.

## By Type Name

Use the `-Amanifold.source.<file-ext>=<type-name-regex>` javac command line option to specify Manifold types that should
statically compile, whether or not they are referenced elsewhere in Java source. 

An easy way to tell the Java compiler to compile *all* the files corresponding with all the type manifolds enabled in
your project:
```java
javac -Amanifold.source.*=.* ...
```
The `*` wildcard selects all type manifolds and the `.*` regular expression selects all types for each type manifold,
therefore all types expressible through Manifold are statically compiled to disk with javac. 

You can limit compilation to types relating to a specific file extension. For instance, if you are using the [JSON manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)
and you want all your JSON files to be statically compiled:
```
javac -Amanifold.source.json=.* ...
```

Define several arguments and use any regular expression to refine the set of types to compile:
```
javac -Amanifold.source.json=.* -Amanifold.source.graphql=^com\.example\..*Queries$ ...
```
This tells the compiler to compile all JSON files and to compile [GraphQL types](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
in package `com.example` ending with `Queries`.

If need be, you can use regular expressions to invert or "black list" inclusions.
```
javac -Amanifold.source.graphql=^(?!(com\.example\..*Queries)).*$ ...
```
Here all GraphQL types compile _except_ those in package `com.example` ending with `Queries`.
 
Using the `class:` prefix you can constrain compilation by the class name of a type manifold. This is useful for
type manifolds not based on files or file extensions. 
```
javac -Amanifold.source.class:com.example.MySpecialTypeManifold=.* ...
```
>As a reminder, the javac command line arguments are additive with respect to types compiled to disk. As a general
>rule Manifold types referenced in Java source are _always_ compiled, regardless of command line argument constraints.

## Adding Source Paths

If you use the `-Amanifold.source.<ext>=<regex` argument, but your resources reside a directory other than Java source
directories or `resource` directories, you can specify additional source paths that are exclusive to Manifold type
compilation using the `-Amanifold.source=<paths>` argument.
```
javac -Amanifold.source=/myproject/src/main/stuff ...
```  
This example adds `/myproject/src/main/stuff` as an additional Manifold source path. Your `-Amanifold.source.<ext>=<regex`
arguments apply to this directory.

## By File Name

Using path-based javac `-Aother.source.files` argument you can enumerate resource files that should compile statically
regardless of whether or not the files are referenced in your code.
```
javac -Aother.source.files=/myproject/src/main/resources/com/example/Queryies.gql /myproject/src/main/resources/com/example/Mutations.gql ...
``` 

Use `other.source.list` to specify a file that contains a list of resource files that should compile statically
regardless of whether or not the files are referenced in your code. The file contains a single resource path per line.
```
javac -Aother.source.list=/myproject/target/otherfiles.txt ...
```

## Build Tooling

If you define your project with **Maven**, you can explicitly compile resources with javac arguments like this:
```xml
  <!-- Configure Manifold as a Javac plugin -->
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.0</version>
    <configuration>
      <source>8</source>
      <target>8</target>
      <encoding>UTF-8</encoding>
      <compilerArgs>
        <arg>-Xplugin:Manifold</arg>
        <arg>-Amanifold.source.json=.*</arg>
        <arg>-Amanifold.source.graphql=^com\.example\..*Queries$</arg>
      </compilerArgs>
    </configuration>
  </plugin>
```

Similarly, with Gradle you add the arguments like this:
```groovy
compileJava {
  options.compilerArgs += 
    ['-Xplugin:Manifold',
     '-Amanifold.source.json=.*', 
     '-Amanifold.source.graphql=^com\\.example\\..*Queries$']
}
```

## Exposing Resource Types

Another benefit from statically compiling resources relates to resource exposure. If your resources are statically
compiled, they are available for use as .class files, not only from your own code, but also from potential consumers
of your code. This is an important distinction to make because if you _don't_ statically compile resources that are
intended for use outside your project, say as part of an API you provide, those resources are not discoverable from
another module using Manifold unless you explicitly expose them from your JAR-based artifact. You can do that using the
`Contains-Sources` manifest entry.

```xml
  <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-jar-plugin</artifactId>
     <configuration>
       <archive>
         <manifestEntries>
           <!--expose GraphQL files if they are NOT statically compiled in your project-->
           <Contains-Sources>graphql</Contains-Sources>
           <!--JPMS module name-->
           <Automatic-Module-Name>mymodule</Automatic-Module-Name>
         </manifestEntries>
       </archive>
     </configuration>
   </plugin>
```
Although Manifold could use the entire class path as the domain of potential resources types, doing so may impact
performance. That's why you must opt-in your module's resources for external use. It bears repeating, if you
statically compile all the resources intended for use outside your project, you _**do not**_ need to opt-in your JAR for
processing -- the resources are already available as .class files.

# Modes

You can use Manifold in one of two modes which you control as an optional argument to the Manifold plugin for javac:

* **static**: `-Xplugin:Manifold` (default) compiles resource types statically at compile-time

* **dynamic**: `-Xplugin:Manifold dynamic` compiles resource types _dynamically_ at _runtime_
(alternatively `-Xplugin:"Manifold dynamic"`, some tools may require quotes)

Most projects benefit most using the default (static) mode. Dynamic mode in most cases should be reserved for specific
type manifolds that are better suited to dynamic compilation.

> If you're not sure which mode to use, try the default static mode -- it's usually the right choice.

General information considering the static v. dynamic mode:

* Both modes operate _lazily_: a type is not compiled unless it is used. For example, if you are using the [JSON manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json), 
only the JSON files you reference in your code will be processed and compiled. This means Manifold will not try to
compile resources your project does not expect to use directly as types.

* Even if you use static mode, you can still reference type manifold classes dynamically e.g., _reflectively_.
In such a case Manifold will dynamically compile the referenced class as if you were operating in dynamic mode. In
general, your code will work regardless of the mode you're using, hence the general recommendation to stay with static
mode where you get the best of both worlds. 

* Dynamic mode requires `tools.jar` at runtime for **Java 8**.

* Static mode is generally faster at runtime since it pre-compiles all the type manifold resources along with Java 
sources when you build your project

* Static mode automatically supports **incremental compilation** and **hotswap debugging** of modified resources in IntelliJ
   
> You can use `javac` command line arguments to statically compile a set of specified types whether you use
> them directly in your code e.g., if type-safe resources are part of an API.  See [Explicit Resource Compilation](#explicit-resource-compilation).
 

# Programming Language Manifolds

todo: use the Javascript manifold as a simple impl, and the Gosu manifold as a more complex one as reference material for
this section  

# Embedding with Fragments (experimental)

You can now *embed* resource content such as JSON, GraphQL, XML, YAML, CSV, etc. directly in a Java source file as a **type-safe**
resource fragment.  A fragment has the same format and grammar as a resource file and, if used with the Manifold
IDE plugins, can be authored with rich editor features like code highlighting, parser feedback, code completion, etc.
This means you can directly embed resources closer to where you use them in your code.  Write a query in the query language
your application uses directly in the Java method that uses the query.

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
declaration.  Here the Javascript type is declared as `Barker` with a `.js` extension indicating the resource type. A fragment must use `[>` and `<]` at the beginning of the comment to delimit the type name and extension.  A fragment
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

>Even though the declared type is package scoped, for the sake of readability it is best to define the fragment
nearest to its intended use. In a future release this level of scoping may be enforced.

## Rich Editing
If used with the Manifold IntelliJ IDEA plugin or the Android Studio plugin, you can edit fragments as if they were in a separate file with all the
editor features you'd expect like highlighting, parser feedback, code completion, etc.  This is especially useful with
GraphQL, SQL, and similar resources where editing a fragment in place provides a more fluid development experience. 
 
## Value Fragments
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

Not all manifold resources can be used as fragment values. The fragment value concept is not always a good fit.
For instance, the Properties manifold does not implement fragment values because a properties type is used statically.

Fragments as values are more useful with multiline String literals via the new [Text Blocks](https://openjdk.java.net/jeps/378)
feature in Java 15:

<p><img src="http://manifold.systems/images/graphql_fragment.png" alt="graphql value fragment" width="70%" height="70%"/></p>

>**Note to Type Manifold service providers**
>
>To support fragments as *values* you must annotate your toplevel types with `@FragmentValue`.
This annotation defines two parameters: `methodName` and `type`, where `methodName` specifies the name of a static 
method to call on the top-level type that represents the type's value, and where `type` is the qualified name of the
value type, which must be contravariant with the `methodName` return type. See the 
[GraphQL type manifold implementation](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-graphql/src/main/java/manifold/graphql/type/GqlParentType.java)
for a reference.


# IDE Support 

Manifold is fully supported in [IntelliJ IDEA](https://www.jetbrains.com/idea/download) and [Android Studio](https://developer.android.com/studio).

## Install

Get the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold) directly from within the IDE via:

<kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Marketplace</kbd> ➜ search: `Manifold`

<p><img src="http://manifold.systems/images/ManifoldPlugin.png" alt="echo method" width="60%" height="60%"/></p>


# Projects
The Manifold framework consists of the core project and a collection of sub-projects implementing SPIs provided
by the core. Each project represents a separate **dependency** you can easily add to your project:

[Manifold : _Core_](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold)<br>

[Manifold : _Java Extensions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-ext)<br>

[Manifold : _GraphQL_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)<br>
[Manifold : _JSON_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-json)<br>
[Manifold : _XML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-xml)<br>
[Manifold : _YAML_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-yaml)<br>
[Manifold : _CSV_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-csv)<br>
[Manifold : _Property files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-properties)<br>
[Manifold : _Image files_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-image)<br>
[Manifold : _Dark Java_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-darkj)<br>
[Manifold : _JavaScript_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-js)<br>

[Manifold : _Properties_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-props)<br>

[Manifold : _Java Templates_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-templates)<br>

[Manifold : _String Interpolation_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-strings)<br>
[Manifold : _(Un)checked Exceptions_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-exceptions)<br>

[Manifold : _Preprocessor_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor)<br>

[Manifold : _Science_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-science)<br>

[Manifold : _Collections_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-collections)<br>
[Manifold : _I/0_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-io)<br>
[Manifold : _Text_](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-text)<br>

>Use the sample projects for nice working examples of how to configure and use Manifold in your project.
>* [Manifold : _Sample App_](https://github.com/manifold-systems/manifold-sample-project)<br>
>* [Manifold : _Sample GraphQL App_](https://github.com/manifold-systems/manifold-sample-graphql-app)<br>
>* [Manifold : _Sample REST API App_](https://github.com/manifold-systems/manifold-sample-rest-api)<br>
>* [Manifold : _Sample Web App_](https://github.com/manifold-systems/manifold-sample-web-app)
>* [Manifold : _Sample Kotlin App_](https://github.com/manifold-systems/manifold-sample-kotlin-app)
>* [Manifold : _Gradle Example Project_](https://github.com/manifold-systems/manifold-simple-gradle-project)

# Setup

## Building this project

The `manifold` project is defined with Maven.  To build it install Maven and run the following command.

```
mvn compile
```

## Using this project

The `manifold` core dependency works with all build tooling, including Maven and Gradle. It also works with Java
versions 8 - 19.

This project consists of two modules:
* `manifold`
* `manifold-rt`

For optimal performance and to work with Android and other JVM languages it is recommended to:
* Add a dependency on `manifold-rt` (Gradle: "implementation", Maven: "compile")
* Add `manifold` to the annotationProcessor path (Gradle: "annotationProcessor", Maven: "annotationProcessorPaths")

See [Gradle](#gradle) and [Maven](#maven) examples below.

## Binaries

If you are *not* using Maven or Gradle, you can download the latest binaries [here](http://manifold.systems/docs.html#download).

## Gradle

>If you are targeting **Android**, please see the [Android](http://manifold.systems/android.html) docs.

>If you are using **Kotlin**, please see the [Kotlin](http://manifold.systems/kotlin.html) docs.

Here is a sample `build.gradle` script. Change `targetCompatibility` and `sourceCompatibility` to your desired Java
version (8 - 19), the script takes care of the rest. 
```groovy
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

targetCompatibility = 11
sourceCompatibility = 11

repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
    implementation 'systems.manifold:manifold-rt:2023.1.3'
    testImplementation 'junit:junit:4.12'
    // Add manifold to -processorpath for javac
    annotationProcessor group: 'systems.manifold', name: 'manifold', version: '2023.1.3'
    testAnnotationProcessor group: 'systems.manifold', name: 'manifold', version: '2023.1.3'
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
    <artifactId>my-manifold-app</artifactId>
    <version>0.1-SNAPSHOT</version>

    <name>My Manifold App</name>

    <properties>
        <!-- set latest manifold version here --> 
        <manifold.version>2023.1.3</manifold.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>systems.manifold</groupId>
            <artifactId>manifold-rt</artifactId>
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

# Platforms

Manifold supports:
* Java SE (8 - 19)
* [Android](http://manifold.systems/android.html)
* [Kotlin](http://manifold.systems/kotlin.html) (limited)

Comprehensive IDE support is also available for IntelliJ IDEA and Android Studio.

# Javadoc 

`manifold`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold/2023.1.3)

`manifold-rt`:<br>
[![javadoc](https://javadoc.io/badge2/systems.manifold/manifold-rt/2023.1.3/javadoc.svg)](https://javadoc.io/doc/systems.manifold/manifold-rt/2023.1.3)

# License

Open source Manifold is free and licensed under the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license.  

# Versioning

For the versions available, see the [tags on this repository](https://github.com/manifold-systems/manifold/tags).

# Author

* [Scott McKinney](mailto:scott@manifold.systems)
