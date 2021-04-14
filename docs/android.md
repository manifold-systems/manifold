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
Get the Manifold plugin directly from within Android Studio:
<br>
`Settings | Plugins | Marketplace | search "Manifold"`
<br>
You must restart Android Studio to enable the plugin. 
 
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
between *annotationProcessor* and *implementation* scoping. Manifold dependencies that operate exclusively within the
Java compiler are only accessible from the processor path, therefore they have no impact on your runtime distribution.

#### Manifold core
```groovy
annotationProcessor 'systems.manifold:manifold:.2021.1.11'
implementation 'systems.manifold:manifold-rt:.2021.1.11'
```
#### Manifold : Extensions
```groovy
annotationProcessor 'systems.manifold:manifold-ext:.2021.1.11'
implementation 'systems.manifold:manifold-ext-rt:.2021.1.11'
```
#### Manifold : GraphQL
```groovy
annotationProcessor 'systems.manifold:manifold-graphql:.2021.1.11'
implementation 'systems.manifold:manifold-graphql-rt:.2021.1.11'
```
#### Manifold : JSON
```groovy
annotationProcessor 'systems.manifold:manifold-json:.2021.1.11'
implementation 'systems.manifold:manifold-json-rt:.2021.1.11'
```
#### Manifold : XML
```groovy
annotationProcessor 'systems.manifold:manifold-xml:.2021.1.11'
implementation 'systems.manifold:manifold-xml-rt:.2021.1.11'
```
#### Manifold : YAML
```groovy
annotationProcessor 'systems.manifold:manifold-yaml:.2021.1.11'
implementation 'systems.manifold:manifold-yaml-rt:.2021.1.11'
```
#### Manifold : CSV
```groovy
annotationProcessor 'systems.manifold:manifold-csv:.2021.1.11'
implementation 'systems.manifold:manifold-csb-rt:.2021.1.11'
```
#### Manifold : Properties
```groovy
annotationProcessor 'systems.manifold:manifold-properties:.2021.1.11'
```
#### Manifold : Image
```groovy
annotationProcessor 'systems.manifold:manifold-image:.2021.1.11'
```
#### Manifold : JavaScript
```groovy
annotationProcessor 'systems.manifold:manifold-js:.2021.1.11'
implementation 'systems.manifold:manifold-js-rt:.2021.1.11'
```
#### Manifold : Templates
```groovy
annotationProcessor 'systems.manifold:manifold-templates:.2021.1.11'
implementation 'systems.manifold:manifold-templates-rt:.2021.1.11'
```
#### Manifold : String Interpolation
```groovy
annotationProcessor 'systems.manifold:manifold-strings:.2021.1.11'
```
#### Manifold : (Un)checked Exceptions
```groovy
annotationProcessor 'systems.manifold:manifold-exceptions:.2021.1.11'
```
#### Manifold : Preprocessor
```groovy
annotationProcessor 'systems.manifold:manifold-preprocessor:.2021.1.11'
```
#### Manifold : Science
```groovy
implementation 'systems.manifold:manifold-science:.2021.1.11'
```
#### Manifold : Collections Extension
```groovy
implementation 'systems.manifold:manifold-collections:.2021.1.11'
```
#### Manifold : IO Extensions
```groovy
implementation 'systems.manifold:manifold-io:.2021.1.11'
```
#### Manifold : Text Extensions
```groovy
implementation 'systems.manifold:manifold-text:.2021.1.11'
```

## Resources

If you use a [type manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#the-big-picture)
that is based on resource files such as GraphQL, JSON, Templates, etc. you must place the resource files in the 
*source* directory along with your Java files.  Do **not** place them in the *res* or *assets* directories.
 
<p><img src="http://manifold.systems/images/android_resources.png" alt="echo method" width="50%" height="50%"/></p> 

