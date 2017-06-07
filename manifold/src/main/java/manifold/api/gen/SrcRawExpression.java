package manifold.api.gen;

/**
 */
public class SrcRawExpression extends SrcExpression<SrcRawExpression>
{
  private String _text;

  public SrcRawExpression( String text )
  {
    _text = text;
  }

  public SrcRawExpression( Class type, Object value )
  {
    _text = makeCompileTimeConstantValue( new SrcType( type ), value );
  }
  public SrcRawExpression( SrcType type, Object value )
  {
    _text = makeCompileTimeConstantValue( type, value );
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    indent( sb, indent );
    sb.append( _text );
    return sb;
  }
}
