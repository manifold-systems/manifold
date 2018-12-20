---
layout: default
---

# ManTL (Manifold Template Language)

ManTL is a lightweight & *type-safe* template engine directly integrated with the Java compiler using [Manifold](http://manifold.systems/).
It supports the full Java language, type-safe arguments to templates, type-safe inclusion of other templates,
shared layouts for templates and custom base classes for application-specific logic, among other features.

ManTL files have the suffix `mtl`, often optionally preceded by the language that the template is targeting 
e.g., `index.html.mtl`.

What sets ManTL apart?  Templates compile directly in your build as if Java source files _without a code generation build step_, therefore
your Java source code can reference and use your template files by name directly as Java classes. This level of 
type-safety ensures both integrity and high performance, both at runtime and compile-time.  It also enables tooling like the [Manifold IntelliJ plugin](https://plugins.jetbrains.com/plugin/10057-manifold)
to provide deterministic code completion, usage searching, and refactoring.  Additionally Manifold professional quality support in IntelliJ
provides incremental compilation and hot swap debugging -- make incremental template changes and see them live on your 
running server.


## Table of Contents
- [Installing](#installing)
- [Usage](#usage)
- [Template Syntax](#basic-syntax)
  * [Statements](#statements)
  * [Expressions](#expressions)
  * [Directives](#directives)
  * [Comments](#comments)
- [Directives](#directives)
  * [`import`](#-import-)
  * [`extends`](#-extends-)
  * [`include`](#-include-)
  * [`params`](#-params-)
  * [`section`](#-section-)
  * [`layout`](#-layout-)
  * [`content`](#-layout-)
- [Spark Java Support](#spark)
  * [Tracing](#tracing)
  * [Spark Template Base Class](#spark-template)
  * [Demo](#demo)
  
<a id="installing" class="toc_anchor"></a>

# Installing #

ManTL can be used by simply adding the following dependency to your project:

```xml
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-templates</artifactId>
      <version>0.32-alpha</version>
    </dependency>
```

# Usage #

Once you have installed ManTL, you can begin using it by placing a new file that ends
a with the `mtl` suffix in your resources directory (nb: not in your source directory).  The file can have any sort of 
string content, as well as [dynamic content](#basic-syntax) and [directives](#directives) that change how the 
template behaves.

Consider the following Manifold Template named `HelloWorld.txt.mtl`, located in the `resources/templates` directory:

```jsp
Hello World!
```

This template could then be used directly from your java code like so:

```java
import templates.HelloWorld;

...
    public void demo() {
      System.out.println(HelloWorld.render());
    }
...
```

This would have the effect of printing "Hello World" to standard out.

If you wanted to add a parameter to the template, you can use the [`params`](#-params-) directive:

```jsp
<%@ params(String name) %>
Hello ${name}!
```

You could then call the template like so:

```java
import templates.HelloWorld;

...
    public void demo() {
      System.out.println(HelloWorld.render("ManTL"));
    }
...
```

Which would result in printing "Hello ManTL" to standard out.

If you do not wish to materialize the template as a string, you can use the `renderInto()` method to render templates
into any `Appendable` object.  The `renderInto()` method will have the same parameters as `render()` but will take
an additional `Appendable` object and return `void`.

<a id="basic-syntax" class="toc_anchor"></a>

# Syntax

As with most template engines, a ManTL consist of regular textual content with various scriptlets and
directives interspersed in that content.

<a id="statements" class="toc_anchor"></a>

## Statements

ManTL lets you control output of a template with Java language statements, also known as a *scriptlet*.  You embed 
statements or statement fragments in a template using this syntax:

```jsp
<% java-statements %>
```
 
ManTL supports all Java language statements including variable and method declarations and control structures. For 
example, the `if` statement: 

```jsp
<% if(total >= 90) { %>
Grade: A
<% } %>
```
results in the following output if `total` is `90` or greater:
```text
Grade: A
```
otherwise, the statement has no effect on the output.

Also notice the statement is fragmented between two sets of `<% %>` delimiters. You can leverage many of Java's
statements in this way to control the output, including `if-else`, `switch`, `for`, `while`, and  `do-while`.

<a id="expressions" class="toc_anchor"></a>

## Expressions

A ManTL expression contains a Java language expression, it is evaluated, coerced to a String, and
inserted where the expression appears in the ManTL file.

Use expressions with this syntax:
```jsp
<%= java-expression %>
```

Additionally, the following shorthand syntax is also valid:
```jsp
${ java-expression }
```

For example, you can do the following with expressions:
```jsp
<html>
  <head><title>Expression Example</title></head>
  <body>
    <% int y = 10; %>
    <p style="font-size: ${y}"> The font size of this paragraph is ${y}. </p>
  </body>
</html>
```

The above template generates the following HTML:

```html
<html>
  <head><title>Expression Example</title></head>
  <body>
      <p style="font-size: 10"> The font size of this paragraph is 10. </p>
  </body>
</html>
```

Note the statement declaring the `y` variable does not directly contribute to the resulting content. This is because a statement
does not produce a value to display.  On the other hand since the evaluation of an expression always produces a value, 
it renders as part of the template's resulting content, hence both `y` expressions render `10` in the output. 

<a id="comments" class="toc_anchor"></a>

## Comments
Comments are blocks of code the compiler ignores; they do not contribute to the template's output. Use them to comment 
a template as you like. They will *not* appear as comments in the generated Java code, they are exclusively template
file comments.

The syntax of a comment is as follows:
```jsp
<%-- This is a comment --%>
```

<a id="directives" class="toc_anchor"></a>

## Directives

Directives are commands you use to direct the compilation and resulting structure of a template.

Directives have the following syntax:

```jsp
<%@ directive-name [options] %>
```

Here is a summary of all the ManTL directives. More detailed descriptions of each follow.

<style>
table {
  font-family: arial, sans-serif;
  border-collapse: collapse;
  width: 100%;
}

td, th {
  border: 1px solid #eeeeee;
  text-align: left;
  padding: 8px;
}

tr:nth-child(even) {
  background-color: #f8f8f8;
}
</style>

| Directive&nbsp;&nbsp;&nbsp;&nbsp;      | Syntax              | Description                                                                         |
|----------------|---------------------------------------------|-------------------------------------------------------------------------------------|
| import         | `<%@ import type-name %>`                   | Imports Java types for use in template directives, statements, and expressions      |
| extends        | `<%@ extends class-name %>`                 | Extends a base class having features suitable for the template file                 |
| params         | `<%@ params(parameter-list) %>`             | Parameters for the template, arguments passed via the `render(arg-list)`method      |
| include        | `<%@ include template-name %>`              | Include a separate template in the template                                         |
| section        | `<%@ section section-name(parameter-list) %>` | Creates a sub-template within the template, that can be called from other templates |
| layout         | `<%@ layout template-name %>`               | Specifies the template in which the declaring template nests its content            |
| content        | `<%@ content %>`                            | Used in a `layout` template, denotes where the content of a nested template renders |


<a id="-import-" class="toc_anchor"></a>

### `import`
Use the `import` directive as you would a Java `import` statement so you can use Java classes without
having to qualify them with package names.

The syntax of the `import` directive:
```jsp
<%@ import type-name %>
```

This example imports the `java.util.HashSet` class and uses it to declare the `myHashSet` variable:
```jsp
<html>
<%@ import java.util.HashSet %>
  <head><title>Import Example</title></head>
  <body>
    <% int y = 10;
       HashSet<Integer> myHashSet = new HashSet<>();
       myHashSet.add(y);
       myHashSet.add(15);
       for(Integer a: myHashSet) { %>
         <p> myHashSet contains ${a}. </p>
       <% } %>
  </body>
</html>
```
The above template produces the following HTML:
```html
<html>
  <head><title>Import Example</title></head>
  <body>
    <p> myHashSet contains 10. </p>
    <p> myHashSet contains 15. </p
  </body>
</html>
```

>Note `import` directives must precede all other directives in your template.

<a id="-extends-" class="toc_anchor"></a>

### `extends`
Use the `extends` directive to make a template extend a custom base class, which you can use to provide
additional application specific functionality e.g., `Request` and `Response` objects in a web application.

A practical example of the `extends` directive:
```java
package demo;

import model.Contact;
import manifold.templates.runtime.BaseTemplate;

public class ExampleTemplate extends BaseTemplate {

  public String displayContact(Contact c) {
    if(c.hasName()) {
      return c.getName();
    } else {
      return c.getEmail();
    }
  }

}
```

This allows the developer to render a clean template:

```jsp
<%@ import model.Contact %>
<%@ extends demo.ExampleTemplate %>
<%@ params(Contact c)%>

<div>
  <div>
    Contact
  </div>
  <div>
    ${displayContact(c)}
  </div>
</div>
```

And easily callable:
```jsp
  get("/contact/:id", (req, resp) -> ShowContact.render(Contact.find(req.getParam("id")));
```


<a id="-include-" class="toc_anchor"></a>

### `include`

The `include` directive allows users to insert other templates inside of the given template in a type
safe manner.

The syntax looks like this:
```jsp
<%@ include [template to include] %>
```

For example, consider the following template, `myTemplate.html.mtl`:
```jsp
<% int fontSize = 0; %>
<html>
  <head><title>WHILE LOOP Example</title></head>
  <body>
    <% while (fontSize <= 3) { %>
      <font color = "green" size = "<%= fontSize %>">
        ManTL Tutorial
      </font><br />
      <%fontSize++;%>
    <%}%>
  </body>
</html>
```
We can then include it from another template as such:
```jsp
<%@ include myTemplate %>
```

Both statements will result in the following HTML code:
```html
<html>
  <head><title>WHILE LOOP Example</title></head>
  <body>
    <font color = "green" size = "0">
      ManTL Tutorial
    </font><
    <font color = "green" size = "1">
      ManTL Tutorial
    </font><br />
    <font color = "green" size = "2">
      ManTL Tutorial
    </font><br />
    <font color = "green" size = "3">
      ManTL Tutorial
    </font><br />
  </body>
</html>
```

#### Conditional Include
ManTL supports shorthand for conditional inclusion of templates. The following syntax:
```jsp
<% if (condition) { %>
  <%@ include myTemplate %>
<% } %>
```
Can be condensed to the following:
```jsp
<%@ include myTemplate if(condition) %>
```
(Note: In the above, parentheses are optional.)

<a id="-params-" class="toc_anchor"></a>

### `params`

Use the `params` directive to declare parameters in a template, similar to declaring parameters for a method. It is 
only allowed for the outermost class (not within sections).

The syntax of the `params` directive is as follows:
```jsp
<%@ params(parameter-list) %>
```

For example, you can create the template `NameDisplay.html.mtl` as the following:

```jsp
<%@ params(String name) %>
<p>Your name is: ${myName}</p>
```

You can then include it in another template as follows:

```jsp
<html>
  <head><title>PARAMS Example</title></head>
  <body>
    <%@ include NameDisplay("Robert") %>
    <%@ include NameDisplay("Scott") %>
  </body>
</html>
```

Then, the following HTML will be generated:
```html
<html>
  <head><title>PARAMS Example</title></head>
  <body>
    <p>Your name is: Robert </p>
    <p>Your name is: Scott </p>
  </body>
</html>
```

<a id="-section-" class="toc_anchor"></a>

### `section`

The `section` directive creates a subsection of the current template that can be added via an `include` directive in 
other templates.

The syntax of a `section` block:
```jsp
<%@ section section-name[(symbols-used-in-section)] %>
  SECTION CONTENT HERE
<%@ end section %>
```
Note the corresponding `<%@ end section %>` directive must be used to complete the section, otherwise
a compile error results.

For example, you can create the template `nestedImport.html.mtl` as the following:
```jsp
<%@ import java.util.* %>
<h1>Defines a section</h1>
<%@ section mySection %>
  <% HashSet<Integer> myHashSet = new HashSet<>();
  myHashSet.add(1);
  myHashSet.add(2);
  myHashSet.add(3);
  for(Integer a: myHashSet) { %>
  <h2 style="font-size: ${a}">Font size: ${a}</h2>
  <% } %>
<%@ end section %>
<p> The End </p>
```

The above code will generate the following HTML:
```html
  <h1>Defines a section</h1>
  <h2 style="font-size: 1">Font size: 1</h2>
  <h2 style="font-size: 2">Font size: 2</h2>
  <h2 style="font-size: 3">Font size: 3</h2>
  <p> The End </p>
```

Then, you can include `mySection` in a separate template:
```jsp
  <%@ include nestedImport.mySection %>
```

Which will result in the following HTML:
```html
  <h2 style="font-size: 1">Font size: 1</h2>
  <h2 style="font-size: 2">Font size: 2</h2>
  <h2 style="font-size: 3">Font size: 3</h2>
```


<a id="-layout-" class="toc_anchor"></a>

### `layout`

Layouts can be made and used with the `content` and `layouts` directives respectively.

The `content` directive splits the current template into the header and footer of a layout.

The `layouts` directive makes the header and footer of the layout frame the current template. 
The current template renders where at the location of the `content` directive.

Both the `content` directive and `layouts` directive are only valid in the outermost class
(not within sections) and can only appear once in a template.

The `params` directive is not yet supported for a template that contains the `content` directive.

The syntax of a content template is as follows:
```jsp
HEADER CONTENT HERE
<%@ content %>
FOOTER CONTENT HERE
```

For example, I can create the template `layoutEx.html.mtl` as the following:
```jsp
</html>
  </body>
    <%@ content %>
  </body>
</html>
```

And use the layout in the following template:
```jsp
<%@ layout layoutEx %>
<h1>This is a template that uses a layout.</h1>
<h2>The layout directive can appear anywhere in the template.</h2>
```


The above code will generate the following HTML:
```html
</html>
  </body>
    <h1>This is a template that uses a layout.</h1>
    <h2>The directive can appear anywhere in the template.</h2>
  </body>
</html>
```

#### Default Layouts

ManTL also supports the ability to set default layouts for templates in a given package via the
`ManifoldTemplates.java` configuration class:

```java
  // Sets default template for all templates
  ManifoldTemplates.setDefaultLayout(MyLayout.asLayout());
  
  // Sets default templates for all templates in "some.package"
  ManifoldTemplates.setDefaultLayout("some.package", AnotherLayout.asLayout());
```

By default, more specific layout declarations take precedence over less specific ones. For example, templates with a 
declared layout (using the layout directive) use the declared layout rather than any default layout.

Note the generated `asLayout()` static method on layout template classes.  This is useful when you override 
layouts, as specified below.

<a id="layout-overrides" class="toc_anchor"></a>

#### Layout Overrides

Sometimes you may want to manually override the layout of a given template in code,
or render a template with no layout.  ManTL classes include two fluent helper methods:
`withoutLayout` and `withLayout(ILayout)` to assist in these cases:

```java
  // Renders the template with no layout, regardless of the configuration
  MyTemplate.withoutLayout().render(); 

  // Renders MyTemplate with the MyLayout layout, regardless of other configuration
  MyTemplate.withLayout(MyLayout.asLayout()).render(); 
```


<a id="spark" class="toc_anchor"></a>

## Spark Java Support

ManTL is designed with web frameworks like [Spark](http://sparkjava.com/) in mind.

A sample Spark application making use of ManTL:

```java
package app;

import manifold.templates.ManifoldTemplates;

import static spark.Spark.*;

import views.*;
import views.layout.*;

public class WebApp { 
  public static void main(String[] args) {
    // Set up the default layout for the application
    ManifoldTemplates.setDefaultLayout(DefaultLayout.asLayout());

    // enable tracing
    ManifoldTemplates.trace();

    // Render the index template
    get("/", (req, resp) -> Index.render("Hello World"));
  }
}
```

There are two templates in the `resources` directory: one at `views/Index.html.mtl` and one at 
`views/layouts/DefaultLayout.html.mtl`.  Note the code takes advantage of the type-safe parameters available
in ManTL and no "TemplateEngine" is needed.

<a id="spark-template" class="toc_anchor"></a>

### SparkTemplate Base Class

Manifold provides base class `manifold.templates.sparkjava.SparkTemplate` for use with the `extends` directive
in your templates (or, more commonly, you extend the base class and add your own application functionality).  This
class provides various convenience methods to get the HTTP Request, Response, etc. and also automatically escapes
all string content for HTML, to help prevent malicious user input from causing a security issue in your application.

If you wish, you can output raw HTML in a template that extends `manifold.templates.sparkjava.SparkTemplate` using the
`raw()` function:

```jsp
  ${raw("<h1>Some Raw HTML</h1>")}
```
<a id="tracing" class="toc_anchor"></a>

### Tracing

ManTL supports performance tracing with the following syntax:
```java
  ManifoldTemplates.trace();
```
After invoking the `trace()` method, every following `render()` call prints the following to the console:
```
  - Template template-name rendered in time-to-render ms
```

<a id="demo" class="toc_anchor"></a>

### Demo

A demo Spark application can be found here:

[https://github.com/manifold-systems/manifold-sample-web-app](https://github.com/manifold-systems/manifold-sample-web-app)
