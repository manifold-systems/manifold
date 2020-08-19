# Gosu, Build Performance, and 

![chart](https://dl.dropbox.com/s/xf8qddt6dfygwv8/gosu_java.png)
## Guidewire products are _**loaded**_ with Gosu
- **PC** consists of **~900k lines of Gosu** vs. ~200k lines of Java
  - Roughly **80%** of hand written code is Gosu
  - We generate boatloads more 

## Customer products are loaded with Gosu
- PZU alone manages > **1M lines of Gosu**
- We have a lot of customers...

## Our franken-gradle build is too slow 
- LOTs of code generation > 1M lines of code on PC, yet more Gosu
- Improvements wrt Gosu == potential productivity boost  

## Constraints
- Gosu has already undergone several periods of performance enhancements
  - Not much left to squeeze from perf work
- **Big Gosu changes are off the table**
  - Despite its prominence, **Gosu barely exists** today in terms of company investment in R&D.
  It is considered a "legacy" project now with just a single developer and is **constrained to maintenance**.
- **∴** Must think differently

## Loading and building *Java type information* from Gosu is *expensive*
- Gosu references a LOT of our own Java classes and [dependencies](https://dl.dropbox.com/s/ep0qypcvki5v1mb/pc_compile_deps.txt)
- Our Java codebase references a LOT of the <i><b><u>same</u></b></i> classes and dependencies
- All of those referenced classes are processed <i><b><u>twice</u></b></i>, once building Gosu and once building Java


- **What if Gosu compiled along with Java and could <i><u>share type information</u></i>?**

<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>

## Wut?
- We can use the *Manifold project*, a Java compiler plugin to augment javac's type system
- We already use it in our suite for dual Java 8 / Java 11 support 
- In short, we can use Manifold to compile Gosu classes inside javac:
```groovy
dependencies {
    // Gosu
    compileOnly 'org.gosu-lang.gosu:gosu-core-api:1.14.x'
    runtime 'org.gosu-lang.gosu:gosu-core-api-precompiled:1.14.x'
    runtime 'org.gosu-lang.gosu:gosu-core:1.14.x'
    // Manifold
    compileOnly 'systems.manifold:manifold:2020.1.24'
    implementation 'systems.manifold:manifold-rt:2020.1.24'
}

tasks.withType(JavaCompile) {
    // Enable Manifold plugin
    options.compilerArgs += ['-Xplugin:Manifold']
    // Compile all *.gs files
    options.compilerArgs += ['-Amanifold.source.gs=.*']
}
``` 
### Result
- The Gosu compiler can share Java type information with javac
- No separate Gosu build steps to manage, compiles Java and Gosu together
- Enables Java-to-Gosu interop -- directly access Gosu from Java

## How does this work?
- The Gosu compiler implements the _**type manifold**_ service interface 
- Type manifolds have first dibs to resolve types by name as Javac encounters them
- Example. As javac encounters the type name "gw.abc.MyGosuClass" Manifold overrides parts of the API and delegates
control to type manifolds.

### Three Compilation Stages in Javac

### 1. Load
javac -> findJavaFilesInPackage("gw.abc") 
- javacFileManager (ManifoldJavacFileManager) -> forEach type manifold
  - findJavaFilesIn("gw.abc")
    - GosuTypeManifold -> return *late bound* file for MyGosuClass
      (getContent() lazily loads source)

### 2. Parse / Enter / Resolve
javac -> resolve("gw.abc.MyGosuClass")
  - load late bound file for MyGosuClass   
  - GosuTypeManifold -> getSource()
    - GosuParser -> parses gw.abc.MyGosuClass -> return Java stub (for interop)

### 3. Generate
javac -> generateBytecode("gw.abc.MyGosuClass")
   - GosuTypeManifold -> compile("gw.abc.MyGosuClass) 
   - GosuCompiler -> compile and return byte[] (Gosu is "self compiled, its source is a stub")
   
## Seamless   
  - Java is unaware, Gosu is Java, Java/Gosu interop is effortless
  - Error reporting from Gosu flows to javac, warnings/errors same format
  - Manifold IntelliJ plugin provides comprehensive support for all type manifolds
  
## Lots of other Manifold gold

### Just-in-time code generator
- GraphQL, JSON, XML, etc.
- Like Gosu but generates full source and lets javac compile
- Zero build steps
- Zero intermediate files
- Always in sync with resources
- Inherently incremental resulting in optimal build times
- Dead simple to use - just add a dependency to your project

### Extensions
- todo

### More
- asdf


## Bolder Ideas

### Replace Gosu
Let's face it, Guidewire has been tying (and failing) to assassinate Gosu for years. Gosu is walking dead anyway.
Why not replace it now while we have the expertise?

### Java
Java has what all insurances crave.
- It's familiar, stable, and steadily improving
- It's a safe choice for our market
- Manifold already supplements Java with missing features, notably Extensions and Structural Interfaces
- Java gets it done well enough and compiles fast

### Kotlin
Kotlin has what all developers crave.
- It's feature-rich and compiles to everything
- JetBrains built it, so it's pretty solid and has good IJ support
- Google promotes it (Android)
- It has most of the features we need, but lacks Structural Interfaces :(

### Scala
No.

### JavaScript 
Hell no. 

