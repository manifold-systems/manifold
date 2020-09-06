package manifold.graphql.sample;

import manifold.rt.api.DisableStringLiteralTemplates;
import manifold.ext.rt.api.Jailbreak;
import manifold.graphql.rt.api.request.Executor;
import org.junit.Test;

import static manifold.graphql.sample.movies.ReviewInput;
import static manifold.graphql.sample.queries.ReviewMutation;
import static org.junit.Assert.assertEquals;

public class MutationTest {
  @Test @DisableStringLiteralTemplates
  public void testReviewMutation() {
    ReviewMutation review = ReviewMutation
      .builder("100", ReviewInput
        .builder(4)
        .withComment("Superb")
        .build())
      .build();
    assertEquals(
      "{\n" +
        "  \"movie\": \"100\",\n" +
        "  \"review\": {\n" +
        "    \"stars\": 4,\n" +
        "    \"comment\": \"Superb\"\n" +
        "  }\n" +
        "}",
      review.write().toJson());
    assertEquals("100", review.getMovie());
    assertEquals(4, review.getReview().getStars());
    assertEquals("Superb", review.getReview().getComment());

    @Jailbreak Executor<ReviewMutation.Result> request = review.request("");
    String query = request._reqArgs.getQuery();
    String expected = "mutation ReviewMutation($movie: ID!, $review: ReviewInput!) { createReview(movie: $movie, review: $review) { id stars comment } }";
    assertEquals( expected.replaceAll("\\s+",""), query.replaceAll("\\s+","") );
  }
}
