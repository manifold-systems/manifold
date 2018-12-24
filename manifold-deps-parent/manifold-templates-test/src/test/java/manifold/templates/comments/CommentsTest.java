package manifold.templates.comments;

import org.junit.Test;
import comments.*;

import static org.junit.Assert.assertEquals;

public class CommentsTest
{
  @Test
  public void basicCommentsWork()
  {
    assertEquals( "", SimpleComment.render() );
  }

  @Test
  public void commentsIgnoreSyntax()
  {
    assertEquals( "", ExpressionInsideComment1.render() );
    assertEquals( "", ExpressionInsideComment2.render() );
    assertEquals( "", StatementInsideComment.render() );
    assertEquals( "", DirectiveInsideComment.render() );
  }
}