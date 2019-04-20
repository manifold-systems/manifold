package manifold.graphql.sample;

import manifold.api.templ.DisableStringLiteralTemplates;
import manifold.ext.api.Jailbreak;
import manifold.graphql.request.Executor;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static manifold.graphql.sample.Sample.*;
import static manifold.graphql.sample.SampleQueries.ActorQuery;
import static manifold.graphql.sample.SampleQueries.MovieQuery;
import static org.junit.Assert.assertEquals;

@DisableStringLiteralTemplates
public class QueryTest {
  @Test
  public void testMoviesQuery() {
    List<ActorInput> actors = Collections.singletonList(ActorInput
      .builder("McQueen")
      .withNationality("American")
      .build());

    MovieQuery movieQuery = MovieQuery
      .builder()
      .withTitle("Le Mans")
      .withActors(actors)
      .withGenre(Genre.Action)
      .withYear(1971)
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
        "  \"year\": 1971\n" +
        "}",
      movieQuery.write().toJson());
    assertEquals(actors, movieQuery.getActors());
    assertEquals(Genre.Action, movieQuery.getGenre());
    assertEquals("Le Mans", movieQuery.getTitle());
    assertEquals(1971, movieQuery.getYear());

    @Jailbreak Executor<MovieQuery.Result> request = movieQuery.request("");
    String query = request._reqArgs.getQuery();
    assertEquals(
      "query MovieQuery($title: String, $genre: Genre, $year: Int, $actors: [ActorInput!]) { movies(title: $title, genre: $genre, year: $year, actors: $actors) { id title genre releaseDate starring { ... on Person { id name } ... on Animal { id name kind } } cast { id name type actor { id name } } } }",
      query);
  }

  @Test
  public void testActorsQuery() {
    ActorQuery actorQuery = ActorQuery
      .builder(MovieInput
        .builder("The Getaway", Sample.Genre.Drama)
        .withReleaseDate(LocalDate.ofYearDay(1972, 1))
        .build())
      .build();
    assertEquals(
      "{\n" +
        "  \"movie\": {\n" +
        "    \"title\": \"The Getaway\",\n" +
        "    \"genre\": \"Drama\",\n" +
        "    \"releaseDate\": \"1972-01-01\"\n" +
        "  }\n" +
        "}",
      actorQuery.write().toJson());
    assertEquals(Genre.Drama, actorQuery.getMovie().getGenre());
    assertEquals("The Getaway", actorQuery.getMovie().getTitle());
    assertEquals(LocalDate.ofYearDay(1972, 1), actorQuery.getMovie().getReleaseDate());

    @Jailbreak Executor<ActorQuery.Result> request = actorQuery.request("");
    String query = request._reqArgs.getQuery();
    assertEquals(
      "query ActorQuery($movie: MovieInput!) { actors(movie: $movie) { ... on Person { id name } ... on Animal { id name kind } } }",
      query);
  }
}
