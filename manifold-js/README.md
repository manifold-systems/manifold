Manifold.js is a [Manifold](http://manifold.io/) extension library that allows for seamless interaction with javascript
resources from Java using the [Java Nashorn](http://openjdk.java.net/projects/nashorn/) project.

The library supports the use of javascript programs from Java.

## Javascript Program Support

Manifold.js makes standard ES5-style Javascript programs available as types in Java.

The entire javascript program is evaluated when the type is first accessed.  Top level functions
are accessible as static methods on the program type.

### Functions

Here is an example top-level function found in `ExampleProgram.js`:

```javascript
    function hello(name) {
        return "Hello " + name;
    }
```

This function could be invoked from Java like so:

```java
    System.out.println( ExampleProgram.hello("Java Programmers") )
```

#### Parameter & Return Types

The parameters and the return type of javascript functions are all of type `Object`.

### Variables

Top level variables in javascript programs are treated as global variables and will retain their values
between evaluation.  Given this function:

```javascript
    var i = 0;

    function nextNum() {
        return i++;
    }
```

The following code

```javascript
    System.out.println( ExampleProgram.nextNum() )
    System.out.println( ExampleProgram.nextNum() )
```

will print

    0.0
    1.0

### Language Extensions

#### Typescript-Style Typing (Parameters & Return Types)

In order to allow for greater control and readability in Java, Manifold.js allows you to specify the types parameters and return
types using Typescript syntax.

Javascript:

```javascript
    import java.util.ArrayList;
    class Sample {
        constructor(a : String) {
            this.foo = a;
        }

        foo (bar: String, baz : Integer) : ArrayList {
           var arrlist = new ArrayList();
           for(var i = 0 ; i < baz ; i ++) {
               arrlist.add(bar);
           }
           return arrlist;
        }
    }
```

Java:

```java
    Sample sample = new Sample();
    System.out.println(foo("Hello", 5)) // ["Hello","Hello","Hello","Hello","Hello"]
```

#### ES6 Arrow Functions

Manifold.js supports the use of ES6 Arrow Functions inside any Javascript program or class.

Javascript:

```javascript
    //Arrow function expression
    function filterEvens(list) {
        return list.filter( a => a % 2 == 0);
    }

    //Arrow function statements
    function incrementList(list) {
        return list.map( a => {return a + 1});
    }
```

### Threading

Manifold.js is subject to the same threading restrictions that the Nashorn javascript engine is.  All programs and classes use
a ConcurrentHashMap for their Bindings, and should thus be safe for inter-thread use [per this SO article](https://stackoverflow.com/questions/30140103/should-i-use-a-separate-scriptengine-and-compiledscript-instances-per-each-threa/30159424#30159424).
