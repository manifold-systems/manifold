# Java's confounding symbol access rules <br/> <small>_(A code generator's perspective)_</small>

Writing code generators can be challenging, particularly with symbol naming. When appropriate, names should match the system of record as closely as possible. But I've found some holes in Java symbol resolve rules that stymie proper name usage. Here are a couple of examples.

## Field hides local var
```java
interface Base {
  int fun();
}

Base foobar() {
  int zap = 1;
  return new Base() {
    int zap = 2;

    public int fun() {
      return zap;   // 2, no way to reference foobar() local zap!
    }
  };
}
```
The `zap` reference resolves to the anonymous class' `zap` field, leaving no way to reference the local `zap` in `foobar()`. In my view the compiler should always favor locals because there is always a way to reference class members using `this`. Other languages, such as Kotlin, follow this line of reasoning. Again, this is more toward enabling code generators to align better with SoR names.

## Field hides inner type
```java
class Foo {
  address address;
  . . .
  public static class address {
    static int count = 0;
    . . .
  }
}
```
Here the `address` field completely hides the `address` inner class. For instance, `count` is unreachable.
```java
Foo.address.count = 1; 
// error: non-static variable address cannot be referenced from a static context
``` 
To my knowledge there is no way to access the `address` inner class. Would be nice if the compiler would instead backtrack and re-resolve the symbol accordingly. This one is a recent discovery I've bumped into in a code generation context.