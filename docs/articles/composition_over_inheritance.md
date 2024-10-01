# Favor composition over inheritance... with Java?

The advantages of composition (or "true" delegation) over implementation inheritance are well documented, but many languages
remain poorly suited for it, particularly Java.

In the influential book "Effective Java" the author advocates favoring composition over inheritance, yet Java itself provides
scant support for this feature. One is left having to manually write large amounts of error-prone boilerplate code to achieve
the most basic form of interface composition. Further, practical application of compositional design is not possible using
Java without language intervention, as is necessary with true delegation and traits. This isn't to say the author's claims
about inheritance and composition aren't well-intentioned, obviously they are, however in practice it is difficult to make
use of this advice when the Java language is still, nearly a quarter-century later, incapable of supporting his position.

The compiler plugin presented here is by no means a comprehensive solution for composition based design. The objective is
to take a step in that direction by building composition features that existing Java projects can easily experiment with.
                                  
# Call forwarding and _true_ delegation

The [manifold-delegation](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-delegation/README.md)
project provides simple constructs to make composition a natural alternative to inheritance with Java.

Use `@link` and `@part` for automatic interface implementation forwarding and _true_ delegation.
```java
class MyClass implements MyInterface {
    @link MyInterface myInterface; // transfers calls on MyInterface to myInterface

    public MyClass(MyInterface myInterface) {
        this.myInterface = myInterface; // dynamically configure behavior
    }

    // No need to implement MyInterface methods, but you can override myInterface as needed.
}
```

See [manifold-delegation](https://github.com/manifold-systems/manifold/blob/master/manifold-deps-parent/manifold-delegation/README.md) for details.
