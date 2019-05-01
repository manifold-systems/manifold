package manifold.graphql.sample;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.ext.api.Jailbreak;
import manifold.graphql.request.Executor;
import org.junit.Test;


import static manifold.graphql.sample.movies.Genre.*;
import static manifold.graphql.sample.movies.*;
import static manifold.graphql.sample.queries.*;
import static org.junit.Assert.assertEquals;

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
}
