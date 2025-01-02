> **⚠ Experimental**

# Optional parameters & named arguments

![latest](https://img.shields.io/badge/latest-v2024.1.43-royalblue.svg)
[![chat](https://img.shields.io/badge/discord-manifold-seagreen.svg?logo=discord)](https://discord.gg/9x2pCPAASn)
[![GitHub Repo stars](https://img.shields.io/github/stars/manifold-systems/manifold?logo=github&style=flat&color=tan)](https://github.com/manifold-systems/manifold)

The `manifold-params` project is a compiler plugin that implements optional parameters and named arguments for methods,
constructors, and records. Use it with any Java project to add clarity and flexibility to call sites and to reduce boilerplate
in class definitions.
                                                                         
```java
public String valueOf(char[] data, 
                      int offset = 0, 
                      int count = data.length - offset) {...}

valueOf(array) // use default values for offet and count
valueOf(array, 2) // use default value for count
valueOf(data: array, count: 20) // use default value for offset
valueOf((array, count: 20)) // use a tuple expression to mix positional and named arguments
```

Optional parameters and named arguments are fully integrated in both **IntelliJ IDEA** and **Android Studio**. Use the IDE's
features to be more productive with optional parameters and named arguments.

## Optional parameters

An optional parameter has a default value assigned to it, much like a field or local variable with an initial value.
```java
void println(String text = "") {...}
```
The default value `""` makes the `text` parameter optional so that `println` may be called with or without an argument.
```java
println(); // same as calling println("");
println("flubber");
println(text: "flubber"); // named argument syntax
```

A default value may be an arbitrary expression of any complexity and can reference preceding parameters.
```java
public record Destination(Country country, String city = country.capital()) {}
```

A method override automatically inherits all the super method's default parameter values. The default values are fixed in
the super class and may not be changed in the overriding method. 
```java
public interface Contacts {
  Contact add(String name, String address = null, String phone = null);
}

public class MyContacts implements Contacts {
  @Override // no default parameter values allowed here
  public Contact add(String name, String address, String phone) {...}
}

Contacts contacts = new MyContacts();
contacts.add("Fred");
// calling directly inherits defaults
((MyContacts)contacts).add("Bob");
```

If an optional parameter precedes a required parameter, positional arguments may still be used with all the parameters.
```java
public Item(int id = -1, String name) {...}

new Item("Chair"); // default value id = -1 is used
new Item(123, "Table");  
```

## Named arguments

Arguments may be named when calling any method, constructor, or record having optional parameters. The naming format follows
manifold's [tuple syntax](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-tuple).

```java
void configure(String name,
               boolean showName = true,
               int color = themeColor(),
               int size = 64, 
               boolean autoSave = true) {...}
```

Naming arguments adds clarity to call sites.
```java
configure(name: "MyConfig",
          showName: false,
          color: 0x7393B3,
          size: 128,
          autoSave: false);
```
But with optional parameters you only have to supply the arguments you need.
```java
configure("Config");
configure(name:"Config", showName:false);
```
And you can order arguments to your liking.
```java
configure(name: "MyConfig",
          color: 0x7393B3,
          showName: false,
          autoSave: false);
```
You can use a tuple expression to pass arguments as a means to mix positional and named arguments.
```java
configure(("Config", showName:false)); 
```
