package manifold.graphql.sample;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import manifold.api.DisableStringLiteralTemplates;
import manifold.ext.rt.api.Jailbreak;
import manifold.graphql.rt.api.request.Executor;
import manifold.json.rt.api.Requester;
import org.junit.Test;


import static manifold.graphql.sample.movies.Genre.*;
import static manifold.graphql.sample.movies.*;
import static manifold.graphql.sample.queries.*;
import static manifold.internal.javac.FragmentProcessor.ANONYMOUS_FRAGMENT_PREFIX;
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

    @Jailbreak Executor<MovieQuery.Result> request = movieQuery.request( "" );
    String query = request._reqArgs.getQuery();
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

    @Jailbreak Executor<ActorQuery.Result> request = actorQuery.request( "" );
    String query = request._reqArgs.getQuery();
    String expected = "query ActorQuery($title:String!,$genre:Genre) {actors(title:$title,genre:$genre) {... on Person {id name} ... on Animal {id name kind}}}";
    assertEquals( expected.replaceAll( "\\s+", "" ), query.replaceAll( "\\s+", "" ) );
  }

  @Test
  public void testGraphQLFragmentIncludedWithQuery()
  {
    CompareRoles compareRoles = CompareRoles.builder("", "").build();
    @Jailbreak Executor<CompareRoles.Result> request = compareRoles.request("");
    // ensure the fragment used in the query is included with the query
    assertTrue( request._reqArgs.getQuery().contains( "fragment comparisonFields" ) );
    // ensure only the fragment used in the query is included
    // e.g., the 'otherComparisonFields' fragments should not be included
    assertFalse( request._reqArgs.getQuery().contains( "fragment otherComparisonFields" ) );
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
    assertTrue( value.getClass().getSimpleName().startsWith( ANONYMOUS_FRAGMENT_PREFIX ) );
  }

  @Test
  public void testBearerAuthorization()
  {
    MovieQuery query = MovieQuery.builder().build();
    @Jailbreak Executor exec = query.request( "" ).withBearerAuthorization( "xyz" );
    @Jailbreak Requester req = exec._requester;
    String bearerAuth = (String)req._headers.get( "Authorization" );
    assertEquals( "Bearer xyz", bearerAuth );
  }
}
