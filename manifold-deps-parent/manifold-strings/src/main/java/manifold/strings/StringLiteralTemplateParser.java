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

package manifold.strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;
import manifold.internal.javac.FragmentProcessor;

public class StringLiteralTemplateParser
{
  private final String _stringValue;
  private final boolean _simpleExprDisabled;
  private final IntPredicate _escapeMatcher;
  private int _index;

  /**
   * Parse a string from a string literal using standard template delimiters e.g., "$foo" and "${foo.bar()}",
   * and return the list of expressions.
   *
   * @param $escapeMatcher     Given the index of a '$' returns whether or not the '$' is escaped. Command line
   *                           compilers filter out the '\' char in the string, so the caller must keep track.
   *                           Other parsers, like many IDE parsers, preserve the '\' chars in the string, so
   *                           they have a different (and simpler) way of determining escaped '$' chars.
   * @param simpleExprDisabled If true, disables simple $ expressions to support ${} expressions only. Note, this
   *                           argument reflects the `manifold.strings.simple.disabled` javac command line option.
   * @param stringValue        The value of the string literal as returned by the tokenizer.
   * @return The list of expressions from the String
   */
  public static List<Expr> parse( IntPredicate $escapeMatcher, boolean simpleExprDisabled, String stringValue )
  {
    if( isFilterFromTemplateParsing( stringValue ) )
    {
      return Collections.emptyList();
    }

    return new StringLiteralTemplateParser( $escapeMatcher, simpleExprDisabled, stringValue ).parse();
  }

  private static boolean isFilterFromTemplateParsing( String stringValue )
  {
    // don't allow string templates in fragments e.g., interferes with graphql and awkward to use @DisableStringLiteralTemplates
    return stringValue.startsWith( FragmentProcessor.FRAGMENT_START ) ||
           stringValue.startsWith( '"' + FragmentProcessor.FRAGMENT_START ) ||
           stringValue.startsWith( "\"\"\"" ) && stringValue.contains( FragmentProcessor.FRAGMENT_START );
  }

  private StringLiteralTemplateParser( IntPredicate $escapeMatcher, boolean simpleExprDisabled, String stringValue )
  {
    _stringValue = stringValue;
    _simpleExprDisabled = simpleExprDisabled;
    _escapeMatcher = $escapeMatcher;
  }

  private boolean isSimpleExprDisabled()
  {
    return _simpleExprDisabled;
  }

  private List<Expr> parse()
  {
    List<Expr> comps = new ArrayList<>();
    StringBuilder contentExpr = new StringBuilder();
    int length = _stringValue.length();
    int offset = 0;
    for( _index = 0; _index < length; _index++ )
    {
      char c = _stringValue.charAt( _index );
      if( c == '$' )
      {
        if( !_escapeMatcher.test( _index ) )
        {
          Expr expr = parseExpr();
          if( expr != null )
          {
            if( contentExpr.length() > 0 )
            {
              // add
              comps.add( new Expr( contentExpr.toString(), offset, ExprKind.Verbatim ) );
              contentExpr = new StringBuilder();
              offset = _index + 1;
            }
            comps.add( expr );
            continue;
          }
        }
      }
      contentExpr.append( c );
    }

    if( !comps.isEmpty() && contentExpr.length() > 0 )
    {
      comps.add( new Expr( contentExpr.toString(), offset, ExprKind.Verbatim ) );
    }

    return comps;
  }

  private Expr parseExpr()
  {
    if( _index + 1 == _stringValue.length() )
    {
      return null;
    }

    return _stringValue.charAt( _index + 1 ) == '{'
           ? parseBraceExpr()
           : parseSimpleExpr();
  }

  private Expr parseBraceExpr()
  {
    int length = _stringValue.length();
    StringBuilder expr = new StringBuilder();
    int index = _index + 2;
    int offset = index;
    for( ; index < length; index++ )
    {
      char c = _stringValue.charAt( index );
      if( c != '}' )
      {
        expr.append( c );
      }
      else
      {
        if( expr.length() > 0 )
        {
          _index = index;
          return new Expr( expr.toString(), offset, ExprKind.Complex );
        }
        break;
      }
    }
    return null;
  }

  private Expr parseSimpleExpr()
  {
    if( isSimpleExprDisabled() )
    {
      return null;
    }

    int length = _stringValue.length();
    int index = _index + 1;
    int offset = index;
    StringBuilder expr = new StringBuilder();
    for( ; index < length; index++ )
    {
      char c = _stringValue.charAt( index );
      if( expr.length() == 0 )
      {
        if( c != '$' && Character.isJavaIdentifierStart( c ) )
        {
          expr.append( c );
        }
        else
        {
          return null;
        }
      }
      else if( c != '$' && Character.isJavaIdentifierPart( c ) )
      {
        expr.append( c );
      }
      else
      {
        break;
      }
      _index = index;
    }
    return expr.length() > 0 ? new Expr( expr.toString(), offset, ExprKind.Identifier ) : null;
  }

  public class Expr
  {
    private String _expr;
    private ExprKind _kind;
    private int _offset;

    Expr( String expr, int offset, ExprKind kind )
    {
      _expr = expr;
      _offset = offset;
      _kind = kind;
    }

    @SuppressWarnings("WeakerAccess")
    public String getExpr()
    {
      return _expr;
    }

    public int getOffset()
    {
      return _offset;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isVerbatim()
    {
      return _kind == ExprKind.Verbatim;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isIdentifier()
    {
      return _kind == ExprKind.Identifier;
    }
  }


  public enum ExprKind
  {
    Verbatim,
    Identifier,
    Complex
  }
}
