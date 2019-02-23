# Building REST APIs with Manifold & JSON Schema

Developing a REST service can benefit from focusing on the API you'd like to provide.  Some have formalized this concept
with _API-first_.  Regardless of how you apply the concept, your Java development experience along the way should remain
as smooth as possible.  This post presents [Manifold](http://manifold.systems/) as a simple new way to maintain an API-centric
development process without sacrificing your Java development experience.

## The REST API Design Problem

Most REST APIs are described in terms of the request/response formats they comprise using a schema definition
language such as JSON Schema.  A complete REST API provides the following information:
* Language-neutral **type definitions**
* Formal type and data **constraints**
* Comprehensive **documentation**

As a language neutral format, however, JSON Schema is not directly accessible from Java. The preferred, decades-old solution
to this kind of problem involves running a code generator in a separate build step to transform structured data to Java.
But this trades one big problem for another -- a code generation step most often hinders what could otherwise be
a streamlined API development experience. The problems involved with code generators include:
* No feedback: JSON schema changes are invisible Java until you rebuild the API
* Stale generated classes
* Increased build times
* Scalability issues: interdependencies, caching, integrations, etc.
* Interrupts train of thought
* Poor IDE integration:
  * No immediate feedback from changes
  * No incremental compilation
  * Can’t navigate from code reference to corresponding JSON schema element
  * Can’t find code usages from JSON schema elements
  * Can’t refactor / rename JSON schema elements

The collective impact on your day-to-day development experience can be heavy.  But most in the Java community shrug it
off because the alternative is to face the reality that the dynamic language guys can sometimes be right.  What else can
we do?

## The REST API Design Solution

A programming language fantasy world would have the Java compiler directly understand JSON Schema and eliminate the code
generation step, similar to the way metaprogramming magically connects code with structured data in dynamic languages.
This is precisely what [Manifold](http://manifold.systems/) accomplishes, all without sacrificing type-safety. Manifold is a general
framework to type-safely expose all kinds of data sources to Java. Using this framework Manifold also provides support for several specific
data formats including JSON, JSON Schema, YAML, and others.  Essentially, Manifold supplies Java with full-spectrum JSON Schema _vision_:
* JSON Schema type definitions are Java types -- the Java compiler _sees_ JSON Schema
* Your JSON Schema is the API *Single Source of Truth (SSoT)*
* Eliminates the code generation step in your build!
* Scalable: JSON files are source files
* Top-notch IDE integration:
  * Make JSON Schema changes and immediately use the changes in your code, no compiling
  * Incremental, processes only the JSON Schema changes you've made, faster builds
  * Navigate from a code reference to a JSON Schema element
  * Perform usage searches on JSON Schema elements to find code references
  * Rename / refactor Schema elements
  * Hotswap debugging support

>The IDE experience is paramount as most professional Java developers live and breathe within the IDE. Manifold does not
>disappoint, it provides a complete, seamless dev experience with the IntelliJ IDEA Manifold plugin.  You can install
>it directly from within IntelliJ IDEA:
>
><kbd>Settings</kbd> ➜ <kbd>Plugins</kbd> ➜ <kbd>Browse repositories</kbd> ➜ search: `Manifold`


## Seeing is Believing

Drop the JSON Schema file `com/example/api/User.json` into your project's `resources` directory:
```json
{
  "$id": "https://example.com/restapi/User.json",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "User",
  "description": "A simple user type to uniquely identify a secure account",
  "type": "object",
  "definitions": {
    "Gender": {"enum": ["male", "female"]}
  },
  "properties": {
    "id": {
      "description": "Uniquely identifies a user",
      "type": "string",
      "format": "email",
      "readOnly": true
    },
    "password": {
      "description": "The user's password",
      "type": "string",
      "format": "password",
      "writeOnly": true
    },
    "name": {
      "type": "string",
      "description": "A public name for the user",
      "maxLength": 128
    },
    "dob": {
      "type": "string",
      "description": "The user's date of birth",
      "format": "date"
    },
    "gender": {
      "$ref": "#/definitions/Gender"
    }
  },
  "required": ["id", "password", "name"]
}
```

Immediately begin using the `com.example.api.User` type directly in your code, without a compilation step:
```java
User user = User.builder("scott", "mypassword", "Scott")
  .withGender(male)
  .build();
```
This code uses the `builder()` method available on all JSON Schema types. Parameters on `builder()` reflect required
types specified in the schema. The `withGender()` method reflects the `gender` property, which is not required.

You can make REST calls directly from `User`:
```java
Requester<User> req = User.request("http://localhost:4567/users");

// Get all Users via HTTP GET
IJsonList<User> users = req.getMany();

// Add a User with HTTP POST
User user = User.builder("scott", "mypassword", "Scott")
  .withGender(male)
  .build();
req.postOne(user);

// Get a User with HTTP GET
String id = user.getId();
user = req.getOne("/$id"); // string interpolation too :)

// Update a User with HTTP PUT
user.setDob(LocalDate.of(1980, 7, 7));
req.putOne("/$id", user);

// Delete a User with HTTP DELETE
req.delete("/$id");
```
All JSON Schema types have a `request()` method that takes a URL representing a base location from which REST calls can
be made.  As shown `request()` provides methods to conveniently invoke HTTP GET, POST, PUT, PATCH, and DELETE. You can
also specify authentication, header values, etc. via `request()`.

You can make changes to `User.json` and immediately use the changes in your code.  You can refactor/rename, change types,
etc. and immediately see the changes take effect in your code.  Easily navigate from code usages to the declarations in
`User.json`.  Find your code usages from any declared element in `User.json`.

> Watch the screencast to see all of this in action:
<p>
  <video height="60%" width="60%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();">
    <source type="video/mp4" src="/images/json.mp4">
  </video>
</p>

## Fluent API

JSON types are defined as a set of fluent _interface_ APIs.  For example, the `User` JSON type is a Java interface and
provides type-safe methods to:
* **create** a `User`
* **build** a `User`
* **modify** properties of a `User`
* **load** a `User` from a string, a file, or a URL using HTTP GET
* **request** Web service operations using HTTP GET, POST, PUT, PATCH, & DELETE
* **write** a `User` as formatted JSON, YAML, or XML
* **copy** a `User`
* **cast** to `User` from any structurally compatible type including `Map`s, all *without proxies*

Additionally, the API fully supports the JSON Schema format including:
* Properties marked `readOnly` or `writeOnly`
* Nullable properties
* `additionalProperties` and `patternProperties`
* Nested types
* Recursive types
* `format` types
* `allOf` composition types
* `oneOf`/`anyOf` union types

JSON Schema constraints are *not* enforced in the API, however, because:
* This is the purpose of a JSON Schema _**validator**_
* Validating constraints can be expensive -- it should be optional

> Learn more about Manifold's [JSON and JSON Schema support](http://manifold.systems/docs.html#json-and-json-schema).


## What About the API Server?

You can use Manifold with [SparkJava](http://sparkjava.com/) to build a nice REST service. SparkJava is a micro framework for creating web
applications in Java with minimal effort.  Here is an HTTP PUT request route to update a `User`:
```java
    // Update existing User
    put("/users/:id", (req, res) ->
      UserDao.updateUser(req.params(":id"),
        User.load().fromJson(req.body())).write().toJson());
```

Here is a more thorough example defining multiple routes for a `User` CRUD API:
```java
public class UserServer {
  public static void main(String[] args) {
    port(4567);
    UserDao.init();

    // GET all Users
    get("/users", (req, res) -> UserDao.getAll(), JsonUtil::toJson);

    // GET User by Id
    get("/users/:id", (req, res) -> {
      String id = req.params(":id");
      User user = UserDao.findUser(id); // <~~~ The User.json file **is** the User type! No code gen, no POJOs.
      if (user != null) {
        return user.write().toJson(); // <~~~ Manifold JSON Schema types provide a powerful fluent API!
      }
      res.status(400);
      return ResponseError.create("No user with id '$id' found") // <~~~ String interpolation!
        .write().toJson();
    });

    // POST new User
    post("/users", (req, res) ->
      UserDao.createUser(User.load().fromJson(req.body())).write().toJson());

    // Update existing User
    put("/users/:id", (req, res) ->
      UserDao.updateUser(req.params(":id"), User.load().fromJson(req.body())).write().toJson());

    // Delete User by Id
    delete("/users/:id", (req, res) -> {
      String id = req.params(":id");
      User user = UserDao.deleteUser(id);
      if (user != null) {
        return user.write().toJson();
      }
      res.status(400);
      return ResponseError.create("No user with id '$id' found").write().toJson();
    });

    // Error response for IllegalStateException
    exception(IllegalArgumentException.class, (e, req, res) -> {
      res.status(400);
      res.body(ResponseError.create(e.getMessage()).write().toJson()); // <~~~ The ResponseError.json file!
    });

    after((req, res) -> res.type("application/json"));
  }
```

> Clone the source code for the [manifold-sample-rest-api](https://github.com/manifold-systems/manifold-sample-rest-api)
project and experiment with Manifold yourself.

## To Sum Up
In this post I've introduced Manifold as a breakthrough Java framework you can use to streamline your REST API development
process.  With it you can eliminate code generators from your build script and the host of problems associated with them.
Using the Manifold IntelliJ IDEA plugin you can further increase your development experience: directly navigate to your
JSON Schema elements, find usages of them in your code, and use deterministic refactor and rename tooling. I also covered
some of the JSON API features such as the `request()` method for direct HTTP functionality. Finally, I demonstrated how
you can create a REST service using Manifold with SparkJava -- a lightweight, potent combination.

Manifold doesn't stop there, however.  JSON Schema support is just a small sampling of what the Manifold framework can do.
Learn more about [Manifold](http://manifold.systems).


