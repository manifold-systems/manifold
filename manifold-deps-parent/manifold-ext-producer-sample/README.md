# Extension Class Producer Sample

The manifold-ext-producer-sample module demonstrates how to implement a 
[type manifold](http://manifold.systems/docs.html#what-is-a-type-manifold) 
to dynamically add [Extension Classes](http://manifold.systems/docs.html#extension-classes) from resource files.

This example creates the `ExtensionProducerSampleTypeManifold` class and 
uses the contrived `.favs` resource files with the following simple format:

```ebnf
grammar = entry | { entry "\n" };
entry = type-name "|" property-name "|" property-value 
type-name = Qualified Java type name
property-name = Java identifier rules
property-value = any text  
```  

For example, file `MyFavorites.favs`:
```text
java.lang.Integer|Color|Red
java.lang.Integer|Food|Chicago-style pizza
abc.MyClass|Color|Blue
```

There can be any number of `.favs` resource files in your project, each
having any number of entries.  `ExtensionProducerSampleTypeManifold`
reads all the `.favs` files, determines the full set of types and properties 
per type, and produces Extension classes for each type.  If `MyFavorites.favs` 
were the only `.favs` file, `ExtensionProducerSampleTypeManifold` would create
two Extension classes:

```java
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class ManIntegerExt {
  public static String favoriteColor(@This String thiz) {
    return "Red";
  }
 
  public static String favoriteFood(@This String thiz) {
    return "Chicago-style pizza";
  }
} 
``` 
and 
```java
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class ManMyClassExt {
  public static String favoriteColor(@This String thiz) {
    return "Blue";
  }
} 
```  

In turn the [Extension type manifold](http://manifold.systems/docs.html#the-extension-manifold)
consumes these extension classes and augments `Integer` and `MyClass` with `favorite` methods 
accordingly.