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

package manifold.js.parser;

import java.util.HashMap;
import java.util.Stack;
import manifold.api.fs.IFile;

public class TemplateTokenizer extends Tokenizer
{
  private boolean _inRawString;
  private boolean _inExpression;
  private boolean _inStatement;
  private boolean _isJST; //true if a JST template file, false if a template literal in a Util file
  private String exprStart; //token that enters an expressionOrStatement
  private HashMap<String, String> _puncEnterExitMap; //maps enter punctuation to exit punctuation (ex: "${" : "}")
  private Stack<String> _curlyStack; //used to match curlies when exiting an expression

  public TemplateTokenizer( IFile file, boolean isJST )
  {
    super( file );
    init( isJST );
  }

  TemplateTokenizer( String subtext, boolean isJST )
  {
    super( subtext );
    init( isJST );
  }

  private void init( boolean isJST )
  {
    _isJST = isJST;
    _inRawString = true;
    _curlyStack = new Stack<>();
    _puncEnterExitMap = new HashMap<>();
    _puncEnterExitMap.put( "${", "}" );
    if( isJST )
    {
      _puncEnterExitMap.put( "<%", "%>" );
      _puncEnterExitMap.put( "<%=", "%>" );
      _puncEnterExitMap.put( "<%@", "%>" );
    }
  }

  public boolean isJST()
  {
    return _isJST;
  }

  @Override
  public Token next()
  {
    Token toke;
    if( reachedEOF() )
    {
      toke = newToken( TokenType.EOF, "EOF" );
    }
    else if( _inRawString )
    {
      toke = consumeRawString();
    }
    else if( _inExpression || _inStatement )
    {
      if( checkForExpressionExit() )
      {
        return consumeTemplatePunc(); //transition from expression to rawstring
      }
      Token supe = super.next();
      if( supe.getType() == TokenType.PUNCTUATION && supe.getValue().equals( "}" ) )
      {
        _curlyStack.pop();
      }
      if( supe.getType() == TokenType.PUNCTUATION && supe.getValue().equals( "{" ) )
      {
        _curlyStack.push( "{" );
      }
      return supe; //if in statement, tokenize as normal
    }
    else
    {
      toke = consumeTemplatePunc(); //transition from rawstring to expression; ${, <%, <%@, or <%=
    }
    return toke;
  }

  private Token consumeTemplatePunc()
  {
    String punc = String.valueOf( currChar() );
    switch( currChar() )
    {
      //
      // Entrance punctuations
      //

      case '$':
        nextChar();
        punc += currChar(); //should be '{'
        nextChar();
        setInExpression();
        _curlyStack.push( "${" );
        break;
      case '<':
        nextChar();
        punc += currChar();
        nextChar();
        if( currChar() == '@' )
        {
          punc += currChar();
          nextChar();
          setInStatement();
        }
        else if( currChar() == '=' )
        {
          punc += currChar();
          nextChar();
          setInExpression();
        }
        else
        {
          setInStatement();
        }
        break;


      //
      // Exit punctuations
      //

      case '%':
        nextChar();
        punc += currChar(); //should be '>'
        nextChar();
        punc += consumeNewLineFollowingStatement();
        setInRawString();
        break;
      case '}':
        nextChar();
        setInRawString();
        break;
    }
    if( _inExpression || _inStatement )
    {
      exprStart = punc;
    }
    return newToken( TokenType.TEMPLATEPUNC, punc );
  }

  private String consumeNewLineFollowingStatement()
  {
    if( !_inStatement )
    {
      return "";
    }

    String newLine = "";
    if( currChar() == '\r' )
    {
      newLine += currChar();
      nextChar();
    }
    if( currChar() == '\n' )
    {
      newLine += currChar();
      nextChar();
    }
    return newLine;
  }

  private Token consumeRawString()
  {
    StringBuilder val = new StringBuilder();
    while( !reachedEOF() )
    {
      if( checkForExpressionEnter() )
      {
        _inRawString = false;
        break;
      }
      if( !_isJST && TokenType.isLineTerminator( currChar() ) )
      {
        if( currChar() == '\r' && peek() == '\n' )
        {
          nextChar(); //skip over the \r in \r\n for windows files
        }
        val.append( "\\n" ); //escape newlines since template literals can be multiline
      }
      else
      {
        val.append( currChar() );
      }
      nextChar();
    }
    return newToken( TokenType.RAWSTRING, val.toString() );
  }

  private boolean checkForExpressionEnter()
  {
    //If escaped, skip escape character and return false
    if( currChar() == '\\' && (peek() == '<' || peek() == '$') )
    {
      nextChar();
      return false;
    }

    exprStart = (_puncEnterExitMap.get( String.valueOf( currChar() ) + peek() ));
    return exprStart != null;
  }

  private boolean checkForExpressionExit()
  {
    //'}' only exits expression if it matches with at the top of the stack ${
    if( exprStart.equals( "${" ) && currChar() == '}' && _curlyStack.peek().equals( "${" ) )
    {
      return true;
    }
    if( !exprStart.equals( "${" ) && currChar() == '%' && peek() == '>' )
    {
      return true;
    }
    return false;
  }

  private void setInRawString()
  {
    _inRawString = true;
    _inStatement = false;
    _inExpression = false;
  }

  private void setInStatement()
  {
    _inStatement = true;
    _inRawString = false;
  }
  private void setInExpression()
  {
    _inExpression = true;
    _inRawString = false;
  }
}
