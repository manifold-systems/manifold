package manifold.api.gen;

/**
 */
public class SrcSwitchCase extends SrcStatement<SrcSwitchCase>
{
  private SrcExpression _expr;
  private SrcStatement _stmt;

  public SrcSwitchCase( Class type, Object valueExpr )
  {
    _expr = new SrcRawExpression( type, valueExpr );
  }

  public SrcSwitchCase statement( SrcStatement stmt )
  {
    _stmt = stmt;
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( "case " ).append( _expr ).append( ":\n" );
    if( _stmt != null )
    {
      if( _stmt instanceof SrcStatementBlock )
      {
        ((SrcStatementBlock)_stmt).render( sb, indent, false );
      }
      else
      {
        _stmt.render( sb, indent + INDENT );
      }
    }
    return sb;
  }
}
