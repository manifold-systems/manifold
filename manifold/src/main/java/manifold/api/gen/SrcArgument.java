package manifold.api.gen;

/**
 */
public class SrcArgument extends SrcAnnotated<SrcArgument>
{
  private SrcExpression _value;

  public SrcArgument( SrcExpression value )
  {
    _value = value;
    _value.setOwner( this );
  }

  public SrcArgument( Class type, Object value )
  {
    _value = new SrcRawExpression( type, value );
    _value.setOwner( this );
  }
  public SrcArgument( SrcType type, Object value )
  {
    _value = new SrcRawExpression( type, value );
    _value.setOwner( this );
  }

  public SrcArgument copy()
  {
    return new SrcArgument( _value.copy() );
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, 0, true );
    if( getSimpleName() != null )
    {
      sb.append( getSimpleName() ).append( " = " );
    }
    _value.render( sb, 0 );
    return sb;
  }
}
