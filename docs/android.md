---
layout: default
---

# Using Manifold with Android Studio

Android Studio's support for [Java 8 features](https://developer.android.com/studio/write/java8-support.html) enables
Android applications to work directly with Manifold. This document provides configuration information to help you setup
your project to use Manifold.

>Note, you can also develop Android applications with IntelliJ IDEA using the Android plugin along with the Manifold
>plugin. 

## Get the Manifold plugin
Get the Manifold plugin directly from within Android Studio via `Settings | Plugins | Marketplace | search "Manifold"`.
You must restart Android Studio to enable the plugin. 
 
## Java 8 Source and Target Compatibility 
Since Manifold requires Java 8 or later, set your project's source and target compatibility values to Java 8:

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
Add the `-Xplugin:Manifold` javac argument to connect Manifold with the compiler.

```groovy
// Manifold Javac plugin
getTasks().withType(JavaCompile) {
    options.compilerArgs += ['-Xplugin:Manifold']
}
```    

## Manifold Dependencies
You can conveniently copy/paste from the following list of the latest Manifold dependencies. Do not change the
`compileOnly` and `implementation` scoping of the dependencies, keep them exactly as they are provided here.

#### Manifold core
```groovy
compileOnly 'systems.manifold:manifold:2020.1.14'
implementation 'systems.manifold:manifold-rt:2020.1.14'
```
#### Manifold : Extensions
```groovy
compileOnly 'systems.manifold:manifold-ext:2020.1.14'
implementation 'systems.manifold:manifold-ext-rt:2020.1.14'
```
#### Manifold : GraphQL
```groovy
compileOnly 'systems.manifold:manifold-graphql:2020.1.14'
implementation 'systems.manifold:manifold-graphql-rt:2020.1.14'
```
#### Manifold : JSON
```groovy
compileOnly 'systems.manifold:manifold-json:2020.1.14'
implementation 'systems.manifold:manifold-json-rt:2020.1.14'
```
#### Manifold : XML
```groovy
compileOnly 'systems.manifold:manifold-xml:2020.1.14'
implementation 'systems.manifold:manifold-xml-rt:2020.1.14'
```
#### Manifold : YAML
```groovy
compileOnly 'systems.manifold:manifold-yaml:2020.1.14'
implementation 'systems.manifold:manifold-yaml-rt:2020.1.14'
```
#### Manifold : CSV
```groovy
compileOnly 'systems.manifold:manifold-csv:2020.1.14'
implementation 'systems.manifold:manifold-csb-rt:2020.1.14'
```
#### Manifold : Properties
```groovy
compileOnly 'systems.manifold:manifold-properties:2020.1.14'
```
#### Manifold : Image
```groovy
compileOnly 'systems.manifold:manifold-image:2020.1.14'
```
#### Manifold : JavaScript
```groovy
compileOnly 'systems.manifold:manifold-js:2020.1.14'
implementation 'systems.manifold:manifold-js-rt:2020.1.14'
```
#### Manifold : Templates
```groovy
compileOnly 'systems.manifold:manifold-templates:2020.1.14'
implementation 'systems.manifold:manifold-templates-rt:2020.1.14'
```
#### Manifold : String Interpolation
```groovy
compileOnly 'systems.manifold:manifold-strings:2020.1.14'
```
#### Manifold : (Un)checked Exceptions
```groovy
compileOnly 'systems.manifold:manifold-exceptions:2020.1.14'
```
#### Manifold : Preprocessor
```groovy
compileOnly 'systems.manifold:manifold-preprocessor:2020.1.14'
```
#### Manifold : Science
```groovy
implementation 'systems.manifold:manifold-science:2020.1.14'
```
#### Manifold : Collections Extension
```groovy
implementation 'systems.manifold:manifold-collections:2020.1.14'
```
#### Manifold : IO Extensions
```groovy
implementation 'systems.manifold:manifold-io:2020.1.14'
```
#### Manifold : Text Extensions
```groovy
implementation 'systems.manifold:manifold-text:2020.1.14'
```

## Resources

If you use a [type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture)
that is based on resource files such as GraphQL, JSON, Templates, etc. you must place the resource files in the 
`source` directory along with your Java files.  Do **not** place them in the `res` or `assets` directories.
 
<p><img src="http://manifold.systems/images/android_resources.png" alt="echo method" width="50%" height="50%"/></p> 

