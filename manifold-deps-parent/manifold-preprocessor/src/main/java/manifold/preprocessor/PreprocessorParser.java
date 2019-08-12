/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor;

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.expression.EmptyExpression;
import manifold.preprocessor.expression.Expression;
import manifold.preprocessor.expression.Identifier;
import manifold.preprocessor.expression.StringLiteral;
import manifold.preprocessor.statement.DefineStatement;
import manifold.preprocessor.statement.EmptyStatement;
import manifold.preprocessor.statement.IssueStatement;
import manifold.preprocessor.statement.FileStatement;
import manifold.preprocessor.statement.IfStatement;
import manifold.preprocessor.statement.SourceStatement;
import manifold.preprocessor.statement.Statement;
import manifold.preprocessor.statement.UndefStatement;
import manifold.api.util.Stack;


import static manifold.preprocessor.TokenType.*;

public class PreprocessorParser
{
  private final Tokenizer _tokenizer;
  private BiConsumer<String, Integer> _issueConsumer;
  private Stack<TokenType> _ifState;

  public PreprocessorParser( CharSequence source, Consumer<Tokenizer> consumer )
  {
    this( source, 0, source.length(), consumer );
  }

  public PreprocessorParser( CharSequence source, int startOffset, int endOffset, Consumer<Tokenizer> consumer )
  {
    _tokenizer = new Tokenizer( source, startOffset, endOffset, consumer );
    _tokenizer.advance();
    _ifState = new Stack<>();
  }

  public FileStatement parseFile()
  {
    return parseFile( null );
  }

  public FileStatement parseFile( BiConsumer<String, Integer> issueConsumer )
  {
    _issueConsumer = issueConsumer;
    List<Statement> statements = new ArrayList<>();
    int pos = _tokenizer.getTokenStart();
    while( _tokenizer.getTokenType() != null )
    {
      statements.add( parseStatement() );
      if( pos == _tokenizer.getTokenStart() && _tokenizer.getTokenType() != null )
      {
        throw new IllegalStateException();
      }
      pos = _tokenizer.getTokenStart();
    }
    return new FileStatement( statements, 0, _tokenizer.getTokenEnd() );
  }

  public Statement parseStatement()
  {
    TokenType tokenType = _tokenizer.getTokenType();
    if( tokenType == null )
    {
      // EOF
      return new EmptyStatement( TokenType.Source, _tokenizer.getTokenStart() );
    }

    switch( tokenType )
    {
      case Whitespace:
      case LineComment:
      case BlockComment:
      case StringLiteral:
      case TextBlock:
      case CharLiteral:
      case Source:
      {
        SourceStatement stmt = new SourceStatement( tokenType, _tokenizer.getTokenStart(), _tokenizer.getTokenEnd() );
        _tokenizer.advance();
        return stmt;
      }

      case Define:
        return parseDefineStatement();

      case Undef:
        return parseUndefStatement();

      case Error:
      case Warning:
        return parseIssueStatement( tokenType == Error );

      case If:
        return parseIfStatement();

      case Elif:
      {
        if( parseErrantIfState( tokenType ) )
        {
          return parseErrantElif();
        }
        break;
      }
      case Else:
      {
        if( parseErrantIfState( tokenType ) )
        {
          return parseErrantElse();
        }
        break;
      }
      case Endif:
      {
        if( parseErrantIfState( tokenType ) )
        {
          return parseErrantEndif();
        }
        break;
      }
    }
    return new EmptyStatement( tokenType, _tokenizer.getTokenStart() );
  }

  private boolean parseErrantIfState( TokenType tokenType )
  {
    TokenType ifState = peekParsingIf();
    return ifState == null ||
           (tokenType == Elif
            ? ifState.ordinal() > tokenType.ordinal()
            : ifState.ordinal() >= tokenType.ordinal());
  }

  private IfStatement parseIfStatement()
  {
    pushParsingIf( If );
    try
    {
      int ifStart = _tokenizer.getTokenStart();
      Expression ifExpr = _tokenizer.getExpression();
      _tokenizer.advance();
      addErrors( ifExpr );
      ArrayList<Statement> ifBlock = new ArrayList<>();
      for( Statement stmt = parseStatement(); !(stmt instanceof EmptyStatement); stmt = parseStatement() )
      {
        ifBlock.add( stmt );
      }

      ArrayList<IfStatement> elifs = parseElifs();

      ArrayList<Statement> elseBlock = parseElse();

      IfStatement ifStmt;
      if( _tokenizer.getTokenType() == Endif )
      {
        ifStmt = new IfStatement( If, ifStart, _tokenizer.getTokenEnd(), ifExpr, ifBlock, elifs, elseBlock );
        _tokenizer.advance();
      }
      else
      {
        ifStmt = new IfStatement( If, ifStart, _tokenizer.getTokenStart(), ifExpr, ifBlock, elifs, elseBlock );
        addError( "Expecting '#endif' to close '#if'" );
      }

      return ifStmt;
    }
    finally
    {
      popParsingIf( If );
    }
  }

  private ArrayList<IfStatement> parseElifs()
  {
    pushParsingIf( Elif );
    try
    {
      ArrayList<IfStatement> elifs = new ArrayList<>();
      while( _tokenizer.getTokenType() == Elif )
      {
        int elifStart = _tokenizer.getTokenStart();
        int elifEnd = _tokenizer.getTokenEnd();
        Expression elifExpr = _tokenizer.getExpression();
        _tokenizer.advance();
        addErrors( elifExpr );
        ArrayList<Statement> elifIfBlock = new ArrayList<>();
        for( Statement stmt = parseStatement(); !(stmt instanceof EmptyStatement); stmt = parseStatement() )
        {
          elifEnd = stmt.getTokenEnd();
          elifIfBlock.add( stmt );
        }
        IfStatement elif = new IfStatement( Elif, elifStart, elifEnd, elifExpr, elifIfBlock, Collections.emptyList(), Collections.emptyList() );
        elifs.add( elif );
      }
      return elifs;
    }
    finally
    {
      popParsingIf( Elif );
    }
  }

  private ArrayList<Statement> parseElse()
  {
    pushParsingIf( Else );
    try
    {
      ArrayList<Statement> elseBlock = new ArrayList<>();
      if( _tokenizer.getTokenType() == Else )
      {
        _tokenizer.advance();
        for( Statement stmt = parseStatement(); !(stmt instanceof EmptyStatement); stmt = parseStatement() )
        {
          elseBlock.add( stmt );
        }
      }
      return elseBlock;
    }
    finally
    {
      popParsingIf( Else );
    }
  }

  private IfStatement parseErrantElif()
  {
    pushParsingIf( Elif );
    try
    {
      addError( "'#" + _tokenizer.getTokenType().getDirective() + "' without '#if'" );
      int elifStart = _tokenizer.getTokenStart();
      Expression elifExpr = _tokenizer.getExpression();
      _tokenizer.advance();
      addErrors( elifExpr );
      ArrayList<Statement> ifBlock = new ArrayList<>();
      for( Statement stmt = parseStatement(); !(stmt instanceof EmptyStatement); stmt = parseStatement() )
      {
        ifBlock.add( stmt );
      }

      ArrayList<IfStatement> elifs = parseElifs();

      ArrayList<Statement> elseBlock = parseElse();

      IfStatement ifStmt;
      if( _tokenizer.getTokenType() == Endif )
      {
        ifStmt = new IfStatement( If, elifStart, _tokenizer.getTokenEnd(), elifExpr, ifBlock, elifs, elseBlock );
        _tokenizer.advance();
      }
      else
      {
        ifStmt = new IfStatement( If, elifStart, _tokenizer.getTokenStart(), elifExpr, ifBlock, elifs, elseBlock );
        addError( "Expecting '#endif' to close '#if'" );
      }

      return ifStmt;
    }
    finally
    {
      popParsingIf( Elif );
    }
  }

  private IfStatement parseErrantElse()
  {
    pushParsingIf( Else );
    try
    {
      addError( "'#" + _tokenizer.getTokenType().getDirective() + "' without '#if'" );
      int elseStart = _tokenizer.getTokenStart();
      int elseEnd = _tokenizer.getTokenEnd();
      _tokenizer.advance();
      ArrayList<Statement> elseBlock = new ArrayList<>();
      for( Statement stmt = parseStatement(); !(stmt instanceof EmptyStatement); stmt = parseStatement() )
      {
        elseBlock.add( stmt );
      }

      IfStatement ifStmt;
      if( _tokenizer.getTokenType() == Endif )
      {
        ifStmt = new IfStatement( If, elseStart, _tokenizer.getTokenEnd(), new EmptyExpression( elseEnd ),
          Collections.emptyList(), Collections.emptyList(), elseBlock );
        _tokenizer.advance();
      }
      else
      {
        ifStmt = new IfStatement( If, elseStart, _tokenizer.getTokenStart(), new EmptyExpression( elseEnd ),
          Collections.emptyList(), Collections.emptyList(), elseBlock );
        addError( "Expecting '#endif' to close '#if'" );
      }

      return ifStmt;
    }
    finally
    {
      popParsingIf( Else );
    }
  }

  private IfStatement parseErrantEndif()
  {
    addError( "'#" + _tokenizer.getTokenType().getDirective() + "' without '#if'" );
    int endifStart = _tokenizer.getTokenStart();
    int endifEnd = _tokenizer.getTokenEnd();
    _tokenizer.advance();

    return new IfStatement( If, endifStart, endifEnd, new EmptyExpression( endifStart ),
      Collections.emptyList(), Collections.emptyList(), Collections.emptyList() );
  }

  private DefineStatement parseDefineStatement()
  {
    Expression expr = _tokenizer.getExpression();
    String name;
    if( !(expr instanceof Identifier) )
    {
      name = "";
      addError( "Expecting a symbol name here" );
    }
    else
    {
      name = ((Identifier)expr).getName();
    }
    DefineStatement defineStmt = new DefineStatement( _tokenizer.getTokenStart(), _tokenizer.getTokenEnd(), name );
    _tokenizer.advance();
    return defineStmt;
  }

  private UndefStatement parseUndefStatement()
  {
    Expression expr = _tokenizer.getExpression();
    String name;
    if( !(expr instanceof Identifier) )
    {
      name = "";
      addError( "Expressions not allowed here" );
    }
    else
    {
      name = ((Identifier)expr).getName();
    }
    UndefStatement defineStmt = new UndefStatement( _tokenizer.getTokenStart(), _tokenizer.getTokenEnd(), name );
    _tokenizer.advance();
    return defineStmt;
  }

  private IssueStatement parseIssueStatement( boolean isError )
  {
    Expression expr = _tokenizer.getExpression();
    StringLiteral messageExpr;
    if( !(expr instanceof StringLiteral) )
    {
      messageExpr = null;
      addError( "Expecting a quoted messageExpr", expr.getStartOffset() );
    }
    else
    {
      messageExpr = (StringLiteral)expr;
    }
    IssueStatement issueStatement = new IssueStatement(
      _tokenizer.getTokenStart(), _tokenizer.getTokenEnd(), messageExpr, isError );
    _tokenizer.advance();
    return issueStatement;
  }

  private void addError( String message )
  {
    addError( message, _tokenizer.getTokenStart() );
  }

  private void addError( String message, int pos )
  {
    if( _issueConsumer != null )
    {
      _issueConsumer.accept( message, pos );
    }

    if( JavacPlugin.instance() == null )
    {
      // IDE
      return;
    }

    IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ),
      new JCDiagnostic.SimpleDiagnosticPosition( pos ),
      "proc.messager", message );
  }

  private void addErrors( Expression expression )
  {
    expression.visitErrors( e -> {
      addError( e.getMessage(), e.getPosition() );
      return true;
    } );
  }

  private void pushParsingIf( TokenType ifType )
  {
    switch( ifType )
    {
      case If:
      case Elif:
      case Else:
        _ifState.push( ifType );
        break;
      default:
        throw new IllegalArgumentException( ifType + " is not a component of '#if'" );
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private TokenType popParsingIf( TokenType ifType )
  {
    switch( ifType )
    {
      case If:
      case Elif:
      case Else:
        if( ifType != _ifState.peek() )
        {
          throw new IllegalStateException( "Unbalanced stack. Found " + _ifState.peek() + " but expected " + ifType );
        }
        return _ifState.pop();
      default:
        throw new IllegalArgumentException( ifType + " is not a component of '#if'" );
    }
  }

  private TokenType peekParsingIf()
  {
    return _ifState.isEmpty() ? null : _ifState.peek();
  }
}
