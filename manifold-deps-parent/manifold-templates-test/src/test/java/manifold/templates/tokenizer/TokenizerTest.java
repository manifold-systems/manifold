package manifold.templates.tokenizer;

import java.util.Collections;
import java.util.List;
import manifold.strings.api.DisableStringLiteralTemplates;
import manifold.templates.tokenizer.Token.TokenType;
import org.junit.Test;

import static manifold.templates.tokenizer.Token.TokenType.*;
import static org.junit.Assert.assertEquals;

@DisableStringLiteralTemplates
public class TokenizerTest
{
  @Test
  public void bootStrapTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    assertEquals( Collections.emptyList(), tokenizer.tokenize( "" ) );

    asssertTokenTypesAre( tokenizer.tokenize( "<html></html>" ), CONTENT );

    asssertTokenTypesAre( tokenizer.tokenize( "<html>${2 + 2}</html>" ),
      CONTENT, EXPR_BRACE_BEGIN, EXPR, EXPR_BRACE_END, CONTENT );

    asssertTokenTypesAre( tokenizer.tokenize( "<html><% if(true) { %> foo <% } else { %> bar <% } %></html>" ),
      CONTENT, STMT_ANGLE_BEGIN, STMT, ANGLE_END, CONTENT, STMT_ANGLE_BEGIN, STMT, ANGLE_END,
      CONTENT, STMT_ANGLE_BEGIN, STMT, ANGLE_END, CONTENT );
  }

  @Test
  public void contentTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    assertEquals( Collections.emptyList(), tokenizer.tokenize( "" ) );

    assertEquals( "<html></html>", tokenizer.tokenize( "<html></html>" ).get( 0 ).getText() );

    assertContentsAre( tokenizer.tokenize( "<html>${2 + 2}</html>" ), "<html>", "${", "2 + 2", "}", "</html>" );

    assertContentsAre( tokenizer.tokenize( "<html><% if(true) { %> \n foo <% } else { %> bar <% } %></html>" ),
      "<html>", "<%", " if(true) { ", "%>", " \n foo ", "<%", " } else { ", "%>", " bar ", "<%", " } ", "%>", "</html>" );
  }

  @Test
  public void lineColPosTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    assertEquals( Collections.emptyList(), tokenizer.tokenize( "" ) );

    assertLineColPosAre( tokenizer.tokenize( "<html></html>" ), 1, 1, 0 );

//        assertLineColPosAre(tokenizer.tokenize("<html>${2 + 2}</html>"),1, 1, 0, 1, 7, 6, 1, 15, 14);

    //assertLineColPosAre(tokenizer.tokenize("<html><% if(true) { %> foo <% } else { %> bar <% } %></html>"),
    //        1, 1, 0, 1, 1, 8, 1, 1, 14);

  }

  private void assertLineColPosAre( List<Token> tokenize, int... vals )
  {
    assertEquals( tokenize.size() * 3, vals.length );
    for( int i = 0; i < tokenize.size(); i++ )
    {
      Token token = tokenize.get( i );
      assertEquals( vals[i * 3], token.getLine() );
      assertEquals( vals[i * 3 + 1], token.getColumn() );
      assertEquals( vals[i * 3 + 2], token.getOffset() );
    }
  }

  private void assertContentsAre( List<Token> tokenize, String... content )
  {
    assertEquals( tokenize.size(), content.length );
    for( int i = 0; i < tokenize.size(); i++ )
    {
      Token token = tokenize.get( i );
      assertEquals( content[i], token.getText() );
    }
  }

//
//    @Test
//    public void statementErrorTest() {
//        Tokenizer tokenizer = new Tokenizer();
//        tokenizer.tokenize("<% foo");
//        tokenizer.tokenize("<% abc <% abc %> %>");
//        tokenizer.tokenize("<% ${ } %>");
//        tokenizer.tokenize("<% Abc <%@ abc %> %>");
//
//        List<String> expectedMessages = new ArrayList<>();
//        expectedMessages.add("Tokenization Error: STATEMENT is not closed");
//        expectedMessages.add("Attempted to open new statement within STATEMENT");
//        expectedMessages.add("Attempted to open new expression within STATEMENT");
//        expectedMessages.add("Attempted to open new directive within STATEMENT");
//
//        assertEquals(tokenizer.getIssues().size(), expectedMessages.size());
//        for(int i = 0; i < expectedMessages.size(); i += 1) {
//            assertEquals(tokenizer.getIssues().get(i).getMessage(), expectedMessages.get(i));
//        }
//
//    }
//
//    @Test
//    public void directiveErrorTest() {
//        Tokenizer tokenizer = new Tokenizer();
//        tokenizer.tokenize("<%@ foo");
//        tokenizer.tokenize("<%@ abc <% abc %> %>");
//        tokenizer.tokenize("<%@ ${ } %>");
//        tokenizer.tokenize("<%@ Abc <%@ abc %> %>");
//
//        List<String> expectedMessages = new ArrayList<>();
//        expectedMessages.add("Tokenization Error: DIRECTIVE is not closed");
//        expectedMessages.add("Attempted to open new statement within DIRECTIVE");
//        expectedMessages.add("Attempted to open new expression within DIRECTIVE");
//        expectedMessages.add("Attempted to open new directive within DIRECTIVE");
//
//        assertEquals(tokenizer.getIssues().size(), expectedMessages.size());
//        for(int i = 0; i < expectedMessages.size(); i += 1) {
//            assertEquals(tokenizer.getIssues().get(i).getMessage(), expectedMessages.get(i));
//        }
//    }
//
//    @Test
//    public void expressionErrorTest() {
//        Tokenizer tokenizer = new Tokenizer();
//        tokenizer.tokenize("${ foo");
//        tokenizer.tokenize("${ abc <% abc %> }");
//        tokenizer.tokenize("${ ${ } }");
//        tokenizer.tokenize("${ Abc <%@ abc %> }");
//
//        List<String> expectedMessages = new ArrayList<>();
//        expectedMessages.add("Tokenization Error: EXPRESSION is not closed");
//        expectedMessages.add("Attempted to open new statement within EXPRESSION");
//        expectedMessages.add("Attempted to open new expression within EXPRESSION");
//        expectedMessages.add("Attempted to open new directive within EXPRESSION");
//
//        assertEquals(tokenizer.getIssues().size(), expectedMessages.size());
//        for(int i = 0; i < expectedMessages.size(); i += 1) {
//            assertEquals(tokenizer.getIssues().get(i).getMessage(), expectedMessages.get(i));
//        }
//
//    }

  @Test
  public void testQuotedExpressions()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> doubleQuotedExpression = tokenizer.tokenize( "<html>${\"}\"}</html>" );
    List<Token> singleQuotedExpression = tokenizer.tokenize( "<html>${\'}\'}</html>" );
    asssertTokenTypesAre( doubleQuotedExpression, CONTENT, EXPR_BRACE_BEGIN, EXPR, EXPR_BRACE_END, CONTENT );
    assertEquals( "\"}\"", doubleQuotedExpression.get( 2 ).getText() );
    assertEquals( "\'}\'", singleQuotedExpression.get( 2 ).getText() );

  }

  @Test
  public void testNestedQuotedExpressions()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> nestedSingleExpression = tokenizer.tokenize( "<html>${\"'hello }'\"}</html>" );
    List<Token> nestedDoubleExpression = tokenizer.tokenize( "<html>${'\"hello }\"'}</html>" );
    assertEquals( "\"'hello }'\"", nestedSingleExpression.get( 2 ).getText() );
    assertEquals( "'\"hello }\"'", nestedDoubleExpression.get( 2 ).getText() );

  }

  @Test
  public void testQuotedStatement()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> doubleQuotedStatement = tokenizer.tokenize( "<% \"%>\" %>" );
    assertEquals( " \"%>\" ", doubleQuotedStatement.get( 1 ).getText() );

  }

  @Test
  public void testNestedQuotedStatement()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> nestedSingleStatement = tokenizer.tokenize( "<%\"'hello }'\"%>" );
    List<Token> nestedDoubleStatement = tokenizer.tokenize( "<%'\"hello }\"'%>" );
    assertEquals( "\"'hello }'\"", nestedSingleStatement.get( 1 ).getText() );
    assertEquals( "'\"hello }\"'", nestedDoubleStatement.get( 1 ).getText() );

  }

  /**
   * Tests that within string literals in statements, expressions
   * and directives, \" is recognized as an escape character.
   */
  @Test
  public void testEscape()
  {
    Tokenizer tokenizer = new Tokenizer();
    tokenizer.tokenize( "<%\"\\\"%>\"%>" );
    tokenizer.tokenize( "${\"\\\"}\"}" );
    tokenizer.tokenize( "<%@\"\\\"%>\"%>" );
  }

  /**
   * Tests that ending files with various types of tokens doesn't create errors
   */
  @Test
  public void endFileTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    tokenizer.tokenize( "HELLO" );
    tokenizer.tokenize( "${ else }" );
    tokenizer.tokenize( "<% foo bar %>" );
    tokenizer.tokenize( "<%@ foo bar %>" );
  }

  @Test
  public void escapingDirectivesWorks()
  {

    Tokenizer tokenizer = new Tokenizer();
    List<Token> tokenize = tokenizer.tokenize( "\\${ foo }" );
    assertEquals( 1, tokenize.size() );
    assertEquals( "${ foo }", tokenize.get( 0 ).getText() );

    tokenize = tokenizer.tokenize( "${1}\\${ foo }${2}" );
    assertEquals( 7, tokenize.size() );
    assertEquals( "${ foo }", tokenize.get( 3 ).getText() );


    tokenize = tokenizer.tokenize( "${1}\\\\${ foo }${2}" );
    assertEquals( 7, tokenize.size() );
    assertEquals( "\\${ foo }", tokenize.get( 3 ).getText() );

    tokenize = tokenizer.tokenize( "\\<%= foo %>" );
    assertEquals( 1, tokenize.size() );
    assertEquals( "<%= foo %>", tokenize.get( 0 ).getText() );

    tokenize = tokenizer.tokenize( "<%=1%>\\<%= foo %><%=2%>" );
    assertEquals( 7, tokenize.size() );
    assertEquals( "<%= foo %>", tokenize.get( 3 ).getText() );


    tokenize = tokenizer.tokenize( "<%=1%>\\\\<%= foo %><%=2%>" );
    assertEquals( 7, tokenize.size() );
    assertEquals( "\\<%= foo %>", tokenize.get( 3 ).getText() );
  }


  @Test
  public void longerTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> tokens = tokenizer.tokenize( "<html>\n" +
                                             "   <head><title>Hello World</title></head>\n" +
                                             "   \n" +
                                             "   <body>\n" +
                                             "      Hello World!<br/>\n" +
                                             "      <%\n" +
                                             "         out.println(\"Your IP address is \" + request.getRemoteAddr());\n" +
                                             "      %>\n" +
                                             "   </body>\n" +
                                             "</html>" );
    asssertTokenTypesAre( tokens, CONTENT, STMT_ANGLE_BEGIN, STMT, ANGLE_END, CONTENT );
  }

  @Test
  public void testDirectiveBasic()
  {
    Tokenizer tokenizer = new Tokenizer();
    asssertTokenTypesAre( tokenizer.tokenize( "<html><%@ directives, yo%></html>" ),
      CONTENT, DIR_ANGLE_BEGIN, DIRECTIVE, ANGLE_END, CONTENT );
  }


  @Test
  public void emptyTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> tokens = tokenizer.tokenize( "${}<%%><%@%>" );
    List<Token> tokensWhiteSpace = tokenizer.tokenize( "${  }<%  %><%@    %>" );

    asssertTokenTypesAre( tokens, EXPR_BRACE_BEGIN, EXPR_BRACE_END, STMT_ANGLE_BEGIN, ANGLE_END, DIR_ANGLE_BEGIN, ANGLE_END );
    asssertTokenTypesAre( tokensWhiteSpace, EXPR_BRACE_BEGIN, EXPR, EXPR_BRACE_END, STMT_ANGLE_BEGIN, STMT, ANGLE_END, DIR_ANGLE_BEGIN, DIRECTIVE, ANGLE_END );
  }

  @Test
  public void blankStringTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> tokens = tokenizer.tokenize( "       " );
    assertEquals( "       ", tokens.get( 0 ).getText() );
    assertEquals( CONTENT, tokens.get( 0 ).getType() );
  }

  @Test
  public void commentTest()
  {
    Tokenizer tokenizer = new Tokenizer();
    List<Token> tokens = tokenizer.tokenize( "<%-- This is a comment test. --%>" );
    assertEquals( " This is a comment test. ", tokens.get( 1 ).getText() );
    assertEquals( COMMENT, tokens.get( 1 ).getType() );
  }


  private void asssertTokenTypesAre( List<Token> tokenize, TokenType... stringContent )
  {
    assertEquals( tokenize.size(), stringContent.length );
    for( int i = 0; i < tokenize.size(); i++ )
    {
      Token token = tokenize.get( i );
      assertEquals( stringContent[i], token.getType() );
    }
  }

//    private void assertTokenContentsAre(List<Token> tokenize, String... stringContent) {
//        assertEquals(tokenize.size(), stringContent.length);
//        for (int i = 0; i < tokenize.size(); i++) {
//            Token token = tokenize.get(i);
//            assertEquals(stringContent[i], token.getText());
//        }
//    }
//  }

//    private void assertTokenContentsAre(List<Token> tokenize, String... stringContent) {
//        assertEquals(tokenize.size(), stringContent.length);
//        for (int i = 0; i < tokenize.size(); i++) {
//            Token token = tokenize.get(i);
//            assertEquals(stringContent[i], token.getText());
//        }
//    }
}
