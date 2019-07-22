# Java Preprocessor

The Java Preprocessor is designed exclusively for *conditional compilation* of Java source code. It is directly
integrated into the Java compiler via the Javac _Plugin_ API. Unlike conventional preprocessors it does *not* incur
separate build steps or additional file I/O, instead it directly contributes as an efficient task, integral to the
compilation pipeline.

![](http://manifold.systems/images/compilerflow.png)

The preprocessor 
- filters code after the source file loads, but before it enters the parser
- preserves line numbers for debugging
- provides environmental symbols such as `JAVA_9_or_Later`
- supports in-line usage such as `class MyClass #if(FOO) extends MySuper #endif {`
- is fully supported by the Manifold IntelliJ plugin

<todo: simple screencast for IJ plugin>

## Setup

Enabling the preprocessor in your project is a simple matter of adding the `manifold-preprocessor` dependency to your
project and passing the `-Xplugin:Manifold` argument to javac:

### Gradle:
```groovy
dependencies {
  compile group: 'systems.manifold', name: 'manifold-preprocessor', version: '2019.1.8'

  // For JAVA 8 **only**
  compile files("${System.properties['java.home']}/../lib/tools.jar")
  //!!! For JAVA 9+ **only**
  annotationProcessor group: 'systems.manifold', name: 'manifold-preprocessor', version: '2019.1.8'
}

tasks.withType(JavaCompile) {
  // Add the Manifold javac plugin
  options.compilerArgs += '-Xplugin:Manifold'
  options.fork = true
}
```

>See Manifold's [Setup](http://manifold.systems/docs.html#setup) documentation for more detailed instructions including
samples for both Gradle and Maven.


## Directives

The Manifold preprocessor uses familiar directives to conditionally filter source code before it is parsed.  The
preprocessor supports the following directives:

* [`#define`](#define)
* [`#undef`](#undef)
* [`#if`](#if)
* [`#elif`](#elif)
* [`#else`](#else)
* [`#endif`](#endif)
* [`#error`](#error) 
* [`#warning`](#warning)

### `#define`
Use `#define` to define a symbol.  When a symbol evaluates it is either `true` or `false`, it is `true` if and only if
it is defined.  You use symbols in expressions as conditions for compilation with `#if` and `#elif` directives. 
 
>Note you don't use `#define` as a means for constant values or macros as you would with C and C++.

The preprocessor's symbols are not accessible to Java code, likewise variables in Java code are not accessible to the
preprocessor. This means symbols specified with `#define` never conflict with fields or variables of the same name.

The effects of `#define` are limited to the file scope, as such symbols defined in one file are not accessible to others.

`#define` directives can appear anywhere in a file following the `package` statement.

```java
package com.example;

#define EXPERIMENTAL

import java.math.BigDecimal;
#if EXPERIMENTAL
import com.example.features.NewFeature;
#endif

public class MyClass #if EXPERIMENTAL implements NewFeature #endif {
  ...
#if EXPERIMENTAL
  @Override
  public void newFeatureMethod() {
    ...
  }
#endif
}
``` 
  
>Note additional symbols are available to the preprocessor to access JVM and compiler settings as well as custom
build properties. These are covered below in the [Definitions](#definitions) section.

### `#undef`

Use `#undef` to *undefine* a symbol so that when evaluated, such as in an `#if` expression, its value is `false`.

A symbol can be defined either with the `#define` directive or other means including `-Akey[=value]` compiler arguments,
`build.properties` files, and environment settings such as the Java source mode (see [Definintions](#definitions)).
Regardless of a symbol's origin or scope, the `#undef` can be used to undefine it, however its effects are limited to
the scope of the containing file.

```java
package com.example;
  
#define FOO
#undef FOO
 
public class MyClass {  
  public static void main(Stirng[] args) {  
#if FOO  
    System.out.println("FOO is defined");
#else
    System.out.println("FOO is not defined");  
#endif  
  }  
} 
```

### `#if`

Code between `#if` and `#endif` directives is included for compilation based on the *expression* used with `#if` -- if
the expression is `true` the code compiles. The expression always evaluates as either `true` or `false`.  If the
expression is a symbol, such as one defined with `#define`, the symbol evaluates to `true` if the symbol is accessible
and has not been undefined with `#undef`.

This example uses `#if` to conditionally compile code based on whether `JAVA_8` is defined: 

```java
#if JAVA_8
  out.println("Compiled with Java source version 8");
#endif
``` 

You can use more than symbols for condition expressions.  They support operators `&&` (and), `||` (or), and `!` (not) to
evaluate whether multiple symbols have been defined. You can also group symbols and operators with parentheses.

Expressions can also test for equality with `==` and `!=`. In this case if both operands involved have values e.g., 
symbols defined via `build.properties` or `-Akey[=value]` javac arguments or String literals, the values are compared. 

### `#elif` <TODO:>

### `#else` <TODO:>

### `#endif` <TODO:>

### `#error` <TODO:> 

### `#warning` <TODO:>


## Expressions <TODO:>


## Definitions <TODO:>
todo:
- Default definitions are provided for
-- `build.properties` files in the source file's directory ancestry
-- `Custom compiler arguments provided by the `-Akey[=value]` javac command line options
-- Compiler and JVM environment settings such as Java source version, JPMS mode, operating system, etc.
- Line numbering is preserved
- Only source files using preprocessor directives are processed
- The Manifold IntelliJ plugin fully supports the preprocessor
- The preprocessor is designed for single project, conditional compilation use-cases such as:
-- Targeting many Java versions
-- Targeting different platforms
-- Debug v. Production
-- Free v. Licenced, Standard v. Professional
-- Experimental features
-- Customer-specific patch releases
-- etc.