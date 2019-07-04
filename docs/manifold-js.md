---
layout: default
---
# The Javascript Manifold

The Javascript Manifold (Manifold.js) is a [Manifold](http://manifold.systems/) extension library that allows for seamless interaction with javascript
resources from Java using the [Rhino](https://github.com/mozilla/rhino) project.

The library supports the use of javascript programs from Java, the use of ES6-flavored javascript classes from
java, the use of Java classes from javascript, as well as the creation of type-safe javascript expressions for
use in Java as a scripting layer.

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

### Parameter & Return Types

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

### Typescript-Style Typing (Parameters & Return Types)

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

### ES6-style Arrow Functions

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

## Experimental Features

The following features are experimental.

### Javascript Class Support

Javascript classes are exposed as regular classes in Java. They have the same functionality as Java classes,
including constructors, methods, static methods, and properties.

Javascript: foo.js

```javascript
    class Foo {

        //Constructor
        constructor(a) {
            this.foo = a;
            this._bars = 5;
        }

        //Methods
        function bar() {
            return this.foo * 2;
        }

        function baz(a,b) {
            return a+b + this.foo;
        }

        //Static Methods
        static hello() {
            return "hello";
        }

        //Properties
        get bars() {
            return this._bars*2;
        }

        set bars(a) {
            this._bars = a;
        }

    }
```

####  Constructor
The constructor is called when a new object is created. It initializes the properties within the object.

Javascript:

```javascript
    class Foo {
        constructor(a) {
            this.bar = a;
        }
    }
```

Java:

```java
    Foo foo = new Foo(5); // Creates new Foo object and sets this.bar to a
```

#### Methods

Methods are functions that are assigned to classes. They can interact with properties of the class and call other
internal methods.

Javascript:

```javascript
    class Foo {

        constructor(a) {
            this.foo = a;
        }

        function bar() {
            return this.foo * 2;
        }
    }
```

Java:

```java
    Foo foo = new Foo(21);
    System.out.println(foo.bar); // prints 42
```
#### Static Methods

Javascript:

```javascript
    class Foo {
        constructor(a) {
            this.foo = a;
            this._bars = 5;
        }

        static function staticFoo() {
            return 42;
        }
    }
```

Java:

```java
    System.out.println(Foo.staticFoo()); // Prints  42
```

#### Properties
Classes can have getter and setter properties which abstract the properties held within the class.

Javascript:

```javascript
    class Foo {
        constructor(a) {
            this.foo = a;
            this._bars = 5;
        }
        get bars() {
            return this._bars*2;
        }

        set bars(a) {
            this._bars = a;
        }
    }
```

Java:

```javascript
    Foo foo = new Foo();
    foo.setBars(21);
    System.out.println(foo.getBars()) // Prints 42
```

### Javascript Template Support

Javascript templates are supported as first class citizens. A Javascript String Template is a file that ends
in the .jst extension.

Javascript Template: SampleJSTemplate.jst

    <%@ params(names) %>

    All Names: <%for (var i = 0; i < names.length; i++) { %>
        ${names[i]}
    <% } %>

The template declares the parameters using the `<%@ params() %>` directive, and can import Java classes using
the <%@ import %> directive.

Javascript statements can be added between the `<%` and `%>` punctuators, which are evaluated as Javascript but
added directly to the generated string.

Javascript expressions can be added either between the `${` and `}` punctuators or the `<%=` and `%>` punctuators,
and are evaluated and added to the generated string.

Javascript templates can then be rendered from Java like so:

Java:

```java

    String str = SampleJSTemplate.renderToString({"Carson", "Kyle", "Lucca"});
    System.out.println(str)

```

### Accessing Javascript Classes from Java

Javascript classes can be accessed using the same syntax as Java classes.

Java:

```java
    Foo foo = new Foo(10);
    System.out.println(foo.bar()); // 20
    System.out.println(foo.getBars()); // 5

    foo.setBars(20);
    System.out.println(foo.getBars()) // 40
    System.out.println(Foo.hello()) // Hello
```

#### Accessing Java Classes from Javascript

The (non-standard javascript) import statement is used to extend Java classes with javascript methods.

Here is some example javascript: hello.js

```javascript
    import java.util.ArrayList;

    function hello() {
        var arrlist = new ArrayList();
        arrlist.add(1);
        arrlist.add(2);
        arrlist.add(3);
        System.out.println(arrlist.toArray(new Integer[arrlist.size()]));
    }
```

This can be invoked from Java like so:

```java
    hello.hello(); //prints [1,2,3]
```

NB: The import statement in Manifold.js acts like the java import statement, not the (unsupported) javascript version.

#### Extending Java Classes from Javascript

Java classes can be extended using javascript, allowing for the creation of modified classes. One
known limitation is that the constructor of the superclass cannot be overwritten.

Javascript:

```javascript
    import java.util.ArrayList;

    class SizePrints extends ArrayList {
        printSize() {
            System.out.println(super.size());
        }
    }
```

Java:

```java
    SizePrints demo = new sizePrints();
    demo.add(1);
    demo.add(2);
    demo.add(3);
    demo.printSize(); // Prints 3
```
