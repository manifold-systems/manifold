/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.js.rt.parser;

import manifold.js.rt.parser.tree.Node;
import manifold.js.rt.parser.tree.template.ExpressionNode;
import manifold.js.rt.parser.tree.template.JSTNode;
import manifold.js.rt.parser.tree.template.RawStringNode;
import manifold.js.rt.parser.tree.template.StatementNode;
import manifold.js.rt.parser.tree.template.TemplateLiteralNode;

public class TemplateParser extends Parser
{
  private Node _templateNode;

  public TemplateParser( TemplateTokenizer tokenizer )
  {
    super( tokenizer );
    if( tokenizer.isJST() )
    {
      _templateNode = new JSTNode();
    }
    else
    {
      _templateNode = new TemplateLiteralNode();
    }
  }

  @Override
  public Node parse()
  {
    nextToken();
    while( !match( TokenType.EOF ) )
    {
      if( match( TokenType.RAWSTRING ) )
      {
        _templateNode.addChild( new RawStringNode( currToken().getValue() ) );
        nextToken();
      }
      else if( matchTemplatePunc( "<%@" ) )
      {
        _templateNode.addChild( parseTemplateParams() );
        nextToken();
      }
      else if( matchTemplatePunc( "<%=" ) || matchTemplatePunc( "${" ) )
      {
        _templateNode.addChild( parseTemplateExpression() );
      }
      else if( matchTemplatePunc( "<%" ) )
      {
        _templateNode.addChild( parseTemplateStatement() );
      }
    }
    return _templateNode;
  }

  private Node parseTemplateStatement()
  {
    StatementNode statementNode = new StatementNode();
    Token startToken = currToken();
    skip( matchTemplatePunc( "<%" ) );
    statementNode.addChild( parseFillerUntil( () -> matchTemplatePunc( "%>" ) ) );
    expect( matchTemplatePunc( "%>" ) );
    statementNode.setTokens( startToken, currToken() );
    nextToken();
    return statementNode;
  }

  private Node parseTemplateExpression()
  {
    ExpressionNode expressionNode = new ExpressionNode();
    Token startToken = currToken();
    //Expressions either start with <% and and with %>; or start with ${ and end with }
    String exitString = matchTemplatePunc( "<%=" ) ? "%>" : "}";
    skip( matchTemplatePunc( "<%=" ) || matchTemplatePunc( "${" ) );
    expressionNode.addChild( parseFillerUntil( () -> matchTemplatePunc( exitString ) ) );
    expect( matchTemplatePunc( "%>" ) );
    expressionNode.setTokens( startToken, currToken() );
    nextToken();
    return expressionNode;
  }

  private Node parseTemplateParams()
  {
    skip( match( TokenType.TEMPLATEPUNC ) );
    if( match( TokenType.IDENTIFIER, "params" ) )
    {
      nextToken();
      return parseParams();
    }
    else if( match( TokenType.KEYWORD, "import" ) )
    {
      return parseImport();
    }
    return null;
  }

  /*Match template specific punctuation only*/
  private boolean matchTemplatePunc( String val )
  {
    return matchIgnoreWhitespace( TokenType.TEMPLATEPUNC, val );
  }

}