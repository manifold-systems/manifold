package manifold.graphql.sample;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import manifold.rt.api.Bindings;
import manifold.rt.api.DisableStringLiteralTemplates;
import manifold.graphql.rt.api.request.Executor;
import manifold.json.rt.api.Requester;
import org.junit.Test;


import static manifold.graphql.sample.movies.Genre.*;
import static manifold.graphql.sample.movies.*;
import static manifold.graphql.sample.queries.*;
import static org.junit.Assert.*;

@DisableStringLiteralTemplates
public class QueryTest
{
  @Test
  public void testMoviesQuery()
  {
    List<ActorInput> actors = Collections.singletonList( ActorInput
      .builder( "McQueen" )
      .withNationality( "American" )
      .build() );

    MovieQuery movieQuery = MovieQuery
      .builder()
      .withTitle( "Le Mans" )
      .withActors( actors )
      .withGenre( Action )
      .withReleaseDate( LocalDate.of( 1971, 6, 3 ) )
      .build();
    assertEquals(
      "{\n" +
      "  \"title\": \"Le Mans\",\n" +
      "  \"actors\": [\n" +
      "    {\n" +
      "      \"name\": \"McQueen\",\n" +
      "      \"nationality\": \"American\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"genre\": \"Action\",\n" +
      "  \"releaseDate\": \"1971-06-03\"\n" +
      "}",
      movieQuery.write().toJson() );
    assertEquals( actors, movieQuery.getActors() );
    assertEquals( Action, movieQuery.getGenre() );
    assertEquals( "Le Mans", movieQuery.getTitle() );
    assertEquals( LocalDate.of(1971, 6, 3), movieQuery.getReleaseDate() );

    Executor<MovieQuery.Result> request = movieQuery.request( "" );
    String query = request.getRequestBody().getQuery();
    String expected = "query MovieQuery($title:String,$genre:Genre,$releaseDate:Date,$actors:[ActorInput!]) {movies(title:$title,genre:$genre,releaseDate:$releaseDate,actors:$actors) {id title genre releaseDate starring {typename:__typename ... on Actor {id name} ... on Animal {kind}} cast {id name type actor {id name}}}}";
    assertEquals( expected.replaceAll( "\\s+", "" ), query.replaceAll( "\\s+", "" ) );
  }

  @Test
  public void testActorsQuery()
  {
    ActorQuery actorQuery = ActorQuery
      .builder( "The Getaway" )
      .withGenre( Drama )
      .build();
    assertEquals(
      "{\n" +
      "  \"title\": \"The Getaway\",\n" +
      "  \"genre\": \"Drama\"\n" +
      "}",
      actorQuery.write().toJson() );
    assertEquals( Drama, actorQuery.getGenre() );
    assertEquals( "The Getaway", actorQuery.getTitle() );

    Executor<ActorQuery.Result> request = actorQuery.request( "" );
    String query = request.getRequestBody().getQuery();
    String expected = "query ActorQuery($title:String!,$genre:Genre) {actors(title:$title,genre:$genre) {... on Person {id name} ... on Animal {id name kind}}}";
    assertEquals( expected.replaceAll( "\\s+", "" ), query.replaceAll( "\\s+", "" ) );
  }

  @Test
  public void testGraphQLFragmentIncludedWithQuery()
  {
    CompareRoles compareRoles = CompareRoles.builder("", "").build();
    Executor<CompareRoles.Result> request = compareRoles.request("");
    // ensure the fragment used in the query is included with the query
    assertTrue( request.getRequestBody().getQuery().contains( "fragment comparisonFields" ) );
    // ensure only the fragment used in the query is included
    // e.g., the 'otherComparisonFields' fragments should not be included
    assertFalse( request.getRequestBody().getQuery().contains( "fragment otherComparisonFields" ) );
  }

  @Test
  public void testEmbeddedQuery()
  {
    /* [> MyEmbedded.graphql <]
    query MyOneAnimal($id: ID!) {
      animal(id: $id) {
        id
        name
        kind
        nationality
      }
    }*/
    MyEmbedded.MyOneAnimal myOneAnimal = MyEmbedded.MyOneAnimal.builder( "1" ).build();
    assertNotNull( myOneAnimal );

    /**[>MyEmbedded2.graphql<]
    query MyOneAnimal2($id: ID!) {
      animal(id: $id) {
        id
        name
        kind
        nationality
      }
    }*/
    MyEmbedded2.MyOneAnimal2 myOneAnimal2 = MyEmbedded2.MyOneAnimal2.builder( "1" ).build();
    assertNotNull( myOneAnimal2 );
  }

  @Test
  public void testStringLiteralQuery()
  {
    Foo value = "[>Foo.graphql<] query MyOneAnimal($id: ID!) { animal(id: $id) { id name } }";
    Foo.MyOneAnimal myOneAnimal = value.builder( "1" ).build();
    assertNotNull( myOneAnimal );
  }

  @Test
  public void testAnonymousStringLiteralQuery()
  {
    Object value = "[>.graphql<] query MyOneAnimal2($id: ID!) { animal(id: $id) { id name } }";
    assertTrue( value.getClass().getSimpleName().startsWith( "Fragment_" ) ); //See ANONYMOUS_FRAGMENT_PREFIX in internal pkg
  }

  @Test
  public void testBearerAuthorization()
  {
    MovieQuery query = MovieQuery.builder().build();
    Executor exec = query.request( "" ).withBearerAuthorization( "xyz" );
    String bearerAuth = (String)exec.getHeaders().get( "Authorization" );
    assertEquals( "Bearer xyz", bearerAuth );
  }

  @Test
  public void testRawResponseHandler()
  {
    MovieQuery query = MovieQuery.builder().build();
    Function<Bindings, Object> handler = bindings -> null;
    Executor exec = query.request( "" ).withRawResponseHandler( handler );
    assertSame( handler, exec.getRawResponseHandler() );
  }
}
