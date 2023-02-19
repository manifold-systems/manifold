---
layout: default
---

# Using Manifold with Android Studio

Android Studio's support for [Java 8 features](https://developer.android.com/studio/write/java8-support.html) enables
Android applications to work directly with Manifold. This document provides supplementary configuration information to
elp you setup your Android project to use Manifold.

>🛈 Note, you can also develop Android applications with IntelliJ IDEA using the Android plugin along with the Manifold
>plugin. 
>
>### Get the Manifold plugin
>Get the Manifold plugin directly from within Android Studio:
><br>
>`Settings | Plugins | Marketplace | search "Manifold"`
><br>
> 
>You must restart Android Studio to enable the plugin. 
 
## Java 8 Source and Target Compatibility 
Set your project's source and target compatibility values to Java 8:

```groovy
android {

  ...

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

## Javac Plugin
Add the *-Xplugin:Manifold* javac argument to connect Manifold with the compiler.

```groovy
// Manifold Javac plugin
getTasks().withType(JavaCompile) {
    options.compilerArgs += ['-Xplugin:Manifold']
}
```    

## Manifold Dependencies
You can conveniently copy/paste from the following list of the latest Manifold dependencies. Note the distinction
between *compileOnly* and *implementation* scoping. Manifold dependencies that operate exclusively within the
Java compiler are only accessible from the processor path, therefore they have no impact on your runtime distribution.

#### Manifold Core
```groovy
compileOnly 'systems.manifold:manifold:2023.1.3'
implementation 'systems.manifold:manifold-rt:2023.1.3'
```
#### Manifold : Extensions
```groovy
compileOnly 'systems.manifold:manifold-ext:2023.1.3'
implementation 'systems.manifold:manifold-ext-rt:2023.1.3'
```
#### Manifold : Props
```groovy
compileOnly 'systems.manifold:manifold-props:2023.1.3'
implementation 'systems.manifold:manifold-props-rt:2023.1.3'
```
#### Manifold : GraphQL
```groovy
compileOnly 'systems.manifold:manifold-graphql:2023.1.3'
implementation 'systems.manifold:manifold-graphql-rt:2023.1.3'
```
#### Manifold : JSON
```groovy
compileOnly 'systems.manifold:manifold-json:2023.1.3'
implementation 'systems.manifold:manifold-json-rt:2023.1.3'
```
#### Manifold : XML
```groovy
compileOnly 'systems.manifold:manifold-xml:2023.1.3'
implementation 'systems.manifold:manifold-xml-rt:2023.1.3'
```
#### Manifold : YAML
```groovy
compileOnly 'systems.manifold:manifold-yaml:2023.1.3'
implementation 'systems.manifold:manifold-yaml-rt:2023.1.3'
```
#### Manifold : CSV
```groovy
compileOnly 'systems.manifold:manifold-csv:2023.1.3'
implementation 'systems.manifold:manifold-csb-rt:2023.1.3'
```
#### Manifold : Properties Files
```groovy
compileOnly 'systems.manifold:manifold-properties:2023.1.3'
```
#### Manifold : Image Files
```groovy
compileOnly 'systems.manifold:manifold-image:2023.1.3'
```
#### Manifold : JavaScript
```groovy
compileOnly 'systems.manifold:manifold-js:2023.1.3'
implementation 'systems.manifold:manifold-js-rt:2023.1.3'
```
#### Manifold : Templates
```groovy
compileOnly 'systems.manifold:manifold-templates:2023.1.3'
implementation 'systems.manifold:manifold-templates-rt:2023.1.3'
```
#### Manifold : String Interpolation
```groovy
compileOnly 'systems.manifold:manifold-strings:2023.1.3'
```
#### Manifold : (Un)checked Exceptions
```groovy
compileOnly 'systems.manifold:manifold-exceptions:2023.1.3'
```
#### Manifold : Preprocessor
```groovy
compileOnly 'systems.manifold:manifold-preprocessor:2023.1.3'
```
#### Manifold : Preprocessor : Android Symbols
```groovy
compileOnly 'systems.manifold:manifold-preprocessor-android-syms:2023.1.3'
```
#### Manifold : Science
```groovy
implementation 'systems.manifold:manifold-science:2023.1.3'
```
#### Manifold : Collections Extension
```groovy
implementation 'systems.manifold:manifold-collections:2023.1.3'
```
#### Manifold : IO Extensions
```groovy
implementation 'systems.manifold:manifold-io:2023.1.3'
```
#### Manifold : Text Extensions
```groovy
implementation 'systems.manifold:manifold-text:2023.1.3'
```

## Resources

If you use a [type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture)
that is based on resource files such as GraphQL, JSON, Templates, etc. you must place the resource files in the 
*source* directory along with your Java files.  Do **NOT** place them in the *res* or *assets* directories.
 
<p><img src="http://manifold.systems/images/android_resources.png" alt="echo method" width="50%" height="50%"/></p> 

## Preprocessor and build variant symbols

If you use the [preprocessor](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor),
you can directly reference Android build variant symbols with the [manifold-preprocessor-android-syms](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-preprocessor-android-syms)
dependency.
```java
#if FLAVOR == "paid"
  @Override
  public void specialMethod(Foo foo) {
  ...
  }
#endif
```
build.gradle
```groovy
dependencies {
    ...
    compileOnly 'systems.manifold:manifold-preprocessor:2023.1.3'
    compileOnly 'systems.manifold:manifold-preprocessor-android-syms:2023.1.3'
}
```