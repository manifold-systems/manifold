package manifold.api.templ;

import java.util.ArrayList;
import java.util.List;

public class StringLiteralTemplateParser
{
  private String _stringValue;
  private int _index;

  public static List<Expr> parse( String stringValue )
  {
    return new StringLiteralTemplateParser( stringValue ).parse();
  }

  private StringLiteralTemplateParser( String stringValue )
  {
    _stringValue = stringValue;
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

    public String getExpr()
    {
      return _expr;
    }

    public int getOffset()
    {
      return _offset;
    }

    public boolean isVerbatim()
    {
      return _kind == ExprKind.Verbatim;
    }

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
