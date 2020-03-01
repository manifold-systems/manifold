# GraphQL Made Easy

GraphQL has thoroughly captivated the API world, due in no small part to its uniquely expressive API query language. But
using it type-safely from Java poses a challenge, as language barriers tend to do. Fortunately, the
[GraphQL Manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql)
eliminates the language divide granting Java with comprehensive GraphQL fluency. Here Scott McKinney demonstrates how
this pioneering framework can boost your productivity with seamless, type-safe GraphQL support. 

# Preview

Here's a quick screencast to give you a sense of how it all works. The article covers what's happening here, but watch
closely. Notice the `.graphql` schema and query files are used _**directly**_ from Java. You can type-safely use queries
without engaging code generation steps, without maintaining POJOs, and without compiling between GraphQL changes.
Perhaps equally impressive is the high level of integration available in the IDE -- you can navigate from Java types and
methods directly to and from corresponding definitions in GraphQL files. You can deterministically search and refactor
usages as well. In essence with Manifold your Java project speaks fluent GraphQL to deliver a truly seamless developer
experience.        
<br>
<p>
  <video height="80%" width="80%" controls="controls" preload="auto" onclick="this.paused ? this.play() : this.pause();" autoplay loop>
    <source type="video/mp4" src="/images/graphql.mp4">
  </video>
</p>
<br>

>Note although the preview demonstrates some features specific to the Manifold plugin for IntelliJ IDEA, you can still
>use Manifold without it; Manifold fully supports GraphQL as well as other schemas as a [standard Java compiler plugin](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold).

# Setting Up a Client

I'm going to use Manifold's sample GraphQL application for this article, but in general you can follow these simple
steps to setup any project and configure your development environment for GraphQL with Manifold.
 
* **Configure your build to use the Manifold libraries**

  Configure your project to use Manifold using the GraphQL [setup instructions](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql#using-this-project).
It's a simple matter of adding the `manifold-graphql` dependency to your build and adding the Manifold plugin argument
to javac. If you are using Maven or Gradle, you can cut and paste what you need from the examples.

* **Install plugins to maximize your productivity**

  While it's possible to use Manifold without an IDE, you'll get the most out of it using the [Manifold plugin](https://plugins.jetbrains.com/plugin/10057-manifold)
for IntelliJ IDEA. You can install it free directly from the IntelliJ [plugin settings UI](https://www.jetbrains.com/help/idea/plugins-settings.html).
If you don't already have IntelliJ installed, you can [download](https://www.jetbrains.com/idea/download/) it free.

  While you're at it install the [JS GraphQL plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) too. It
provides solid GraphQL editing support and the Manifold plugin pairs exceptionally well with it. Personally, I don't
leave home without it.

* **Obtain a GraphQL schema**
 
  A standard GraphQL schema file defines the API you'll use in your client. By convention this file is named
*schema.graphql* and can be obtained using an introspection query against the GraphQL server endpoint. There are [command
line tools](https://github.com/Urigo/graphql-cli) available for this, but do yourself a favor and install the
[JS GraphQL](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) plugin for IntelliJ and [configure it](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files)
to maintain your schema files for you. It truly is a brilliant plugin.


# The GraphQL Sample Application

Manifold's sample GraphQL application provides a simple movie service to query for movies and write reviews. For demo
purposes it's backed by a simple in-memory data structure. The project includes both a client and server and also
provides a schema file, *movies.graphql*. This article's focus is the client side.

Clone the app to follow along:

`git clone https://github.com/manifold-systems/manifold-sample-graphql-app.git`
                                                                          
Alternatively, you can open the project directly from IntelliJ:

<kbd>File</kbd> ➜ <kbd>New</kbd> ➜ <kbd>Project from version control</kbd> ➜ <kbd>Git</kbd>: `https://github.com/manifold-systems/manifold-sample-graphql-app.git`

>Note the sample application uses Java 11, be sure to set up your environment accordingly.

See the [readme file](https://github.com/manifold-systems/manifold-sample-graphql-app) for complete information.

 
# Writing and Using Queries

Once you have a client application set up and a GraphQL schema to query, you can begin writing and using queries. You
write GraphQL queries directly with standard *.graphql* resource files. Here's the query from the preview above:
```graphql
query MoviesQuery($title: String, $genre: Genre) {
    movies(title: $title, genre: $genre) {
        title     
        genre
        releaseDate
        starring {
            ... on Actor {
                name
            }  
        }
    }
}
```      
It resides in the [queries.graphql](https://github.com/manifold-systems/manifold-sample-graphql-app/blob/master/src/main/resources/manifold/graphql/sample/schema/queries.graphql)
file in the resource directory. All the queries in the file are directly and type-safely accessible to Java
using the qualified name of the file, as if it were a Java class. For example, access the *MoviesQuery* by name as a
Java type like this:
```java
import graphql.queries.MoviesQuery;
```                         
>Note you can use any names you like for resource directories and query files e.g., the *graphql* directory is a chosen
>name for this demo.
 
You can create a *MoviesQuery* using a convenient builder pattern:
```java
var query = MoviesQuery.builder()
  .withGenre(Action)
  .build();         
```        
The parameters on the builder method reflect required (non-null) parameters from *MoviesQuery* defined in the
[queries.graphql](https://github.com/manifold-systems/manifold-sample-graphql-app/blob/master/src/main/resources/manifold/graphql/sample/schema/queries.graphql)
file. Thus since *MoviesQuery* does not specify any non-null parameters (using the _**!**_ operator) the builder
method has an empty parameter list. Optional (nullable) parameters are configured using *withXxx()* methods such as
*withGenre()* for the nullable *$genre* parameter.

As with query definitions the GraphQL enum definition in the [movies.graphql](https://github.com/manifold-systems/manifold-sample-graphql-app/blob/master/src/main/resources/manifold/graphql/sample/schema/queries.graphql)
schema maps directly to the *Action* Java enum constant, so you can statically import it like this:
```java
import static graphql.movies.Genre.Action;
```
In the same way, Manifold maps the entire schema directly into Java's type system -- types, inputs, interfaces, enums,
unions, queries, mutations, etc. are all type-safely accessible from Java. **Importantly there are no code generation
build steps involved, no POJOs to maintain, and no compilation required between GraphQL changes.** Instead Manifold
plugs into the Java compiler to make it all happen as if by magic. As the preview above demonstrates you simply drop a
schema file into your project and begin coding against it.

You can easily execute a query against a server endpoint using an HTTP POST request and receive results.
```java
var result = query.request("http://localhost:4567/graphql").post();
```
Here, the sample GraphQL app server runs locally on port 4567 and handles the *graphql* endpoint. Notice the request API
is conveniently built directly into the query model.

>Note, the examples leverage type inference using the _**var**_ keyword provided with Java 11. You can also specify the
>types explicitly if you prefer. 

With the query results in hand you can type-safely process them.
```java
for (var actionMovie: result.getMovies()) {
  out.println( 
    "Title: " + actionMovie.getTitle() + "\n" +
    "Genre: " + actionMovie.getGenre()} + "\n" +
    "Year: " + actionMovie.getReleaseDate().getYear() + "\n" +
    "Starring: " + actionMovie.getStarring().getName() );
}
``` 
 
# Using Mutations

GraphQL mutations look and behave the same as queries. Here's the *ReviewMutation* used in [MovieClient.java](https://github.com/manifold-systems/manifold-sample-graphql-app/blob/master/src/main/java/manifold/graphql/sample/client/MovieClient.java)
from the sample application, it's defined in the [queries.graphql](https://github.com/manifold-systems/manifold-sample-graphql-app/blob/master/src/main/resources/manifold/graphql/sample/schema/queries.graphql)
file.
```graphql
mutation ReviewMutation($movieId: ID!, $review: ReviewInput!) {
  createReview(movieId: $movieId, review: $review) {
    id
    stars
    comment
  }
}
```
Mutations usually entail a three step process.
* Query for information to identify the object to update, typically you query for an ID
* Create an input object with the new and/or changed information
* Create and post the mutation object parameterized with your ID and input object

You can glean this process upon inspection of the *ReviewMutation* type. The *createReview* member is parameterized
with the *ID* of the movie to review and the *ReviewInput* type containing the *star* rating and an optional *comment*.

Following the sample app you can see how to create a review for the movie *"Le Mans"*.

First, find the movie *"Le Mans"*. 
```java
// Find the movie to review ("Le Mans")
var movie = MovieQuery.builder()
  .withTitle("Le Mans").build()
  .request(ENDPOINT).post()
  .getMovies().first();
```
Next create a review for *"Le Mans"* using the *ReviewInput* defined the *movies.graphql* schema file.
```graphql
input ReviewInput {
  stars: Int!
  comment: String
}
```
You use input objects the same way you use query objects. Here we make a *ReviewInput* using the *builder()* method. In
this case since the *stars* property is non-null, the builder method exposes it as a non-null parameter, making it a
required property. The *comment* property is nullable, hence the usage of the optional *withComment()* method. 
```java
var review = ReviewInput.builder(5)
  .withComment("Topnotch racing film.")
  .build();
```
Having built the *ReviewInput*, the next step involves passing it along in a *ReviewMutation*, again using a *builder()*
method. Since *ID* and *review* are defined as non-null parameters to the mutation definition, the *builder()* method
reflects them as required parameters.
```java
var mutation = ReviewMutation.builder(movie.getId(), review).build();
```
Just as with a query you invoke the mutation using an HTTP POST request.
```java
var createdReview = mutation.request(ENDPOINT).post().getCreateReview();
```                                                                  
The post returns the resulting *Review* object you can type-safely process.
```java
out.println(
  "Review for: ${movie.getTitle()}\n" +
  "Stars: ${createdReview.getStars()}\n" +
  "Comment: ${createdReview.getComment()}\n"
);                                                                  
``` 

# Request Authentication

In practice a GraphQL client often needs to specify some form of user authentication when making requests to protected
resources. By convention this is handled using the *Authorization* HTTP header. Manifold makes this easy with
*Request* API methods such as *withBearerAuthorization()*.
```java
var result = query.request("https://api.github.com/graphql")                           
               .withBearerAuthorization("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
               .post();
```  
This example demonstrates how you can configure a request with a security token using the Bearer authentication scheme.
Note Bearer authentication should only be used over HTTPS (SSL).

# Embedding Queries

If you like to experiment with bleeding edge technology, have a look at Manifold [fragments](https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#embedding-with-fragments-experimental).
This new experimental feature lets you embed resources directly and *type-safely* into your Java source. It is
particularly effective for single-use GraphQL queries where you edit and maintain a query definition closer to where it
is used in code.

It's pretty simple. You embed a query definition in a comment like this:

<p><img src="http://manifold.systems/images/fragment_declaration.png" alt="declaration fragment" width="80%" height="80%"/></p>

The `[>MyQuery.graphql<]` tag tells Java this is an embedded fragment of resource type *graphql* with name *MyQuery*, as
if it were defined in a resource file of the same name. As such, you reference the fragment by its declared name,
*MyQuery*.

Of particular importance is fragment type-safety. Fragments are statically typed, which allows the compiler to verify
code that uses them. As the code snippet illustrates with the IDE plugin you not only have code completion etc. in
your Java code, but you also have rich editing capabilities *within the fragment itself*!

As with other Manifold features, you don't need IntellIJ to use fragments because Manifold works directly with the Java
Compiler as a plugin. As such, you can use text editors or other IDEs of your preference. However, as you can imagine
your productivity with these features enabled in IntelliJ can vastly improve your overall dev experience.
  
# New Life
 
An API for APIs. That's essentially what GraphQL is. In hindsight it's the obvious remedy to maintenance and performance
ailments plaguing service providers -- a declarative language enabling service consumers to precisely structure
the information they need. And, importantly, to do it all type-safely.

As more service providers embrace GraphQL, pressures mount for a solid Java solution particularly on the
client. Here I've demonstrated how the GraphQL Manifold reaches beyond expectations to provide truly seamless, type-safe
GraphQL access from Java. Indeed, its extraordinary metaprogramming faculties afford a level of flexibility normally
reserved for dynamic languages such as Javascript. Most importantly, these capabilities exist _statically_ when and where
you need them most: while you're writing code in your IDE! As a consequence GraphQL feels light, connected,
and approachable.

I'm hopeful to have piqued your interest and that you'll dig a little deeper into the [GraphQL Manifold](https://github.com/manifold-systems/manifold/tree/master/manifold-deps-parent/manifold-graphql).
Thanks for reading!
