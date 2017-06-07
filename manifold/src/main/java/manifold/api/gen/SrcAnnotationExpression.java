package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcAnnotationExpression extends SrcExpression<SrcAnnotationExpression>
{
  private String _fqn;
  private List<SrcArgument> _arguments = new ArrayList<>();

  public SrcAnnotationExpression( String fqn )
  {
    _fqn = fqn;
  }
  public SrcAnnotationExpression( Class type )
  {
    _fqn = type.getName();
  }

  public SrcAnnotationExpression addArgument( SrcArgument arg )
  {
    _arguments.add( arg );
    return this;
  }
  public SrcAnnotationExpression addArgument( String paramName, Class type, Object value )
  {
    _arguments.add( new SrcArgument( type, value ).name( paramName ) );
    return this;
  }
  public SrcAnnotationExpression addArgument( String paramName, SrcType type, Object value )
  {
    _arguments.add( new SrcArgument( type, value ).name( paramName ) );
    return this;
  }

  public String getType()
  {
    return _fqn;
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    indent( sb, indent );
    sb.append( '@' ).append( _fqn );
    renderArgumenets( sb, _arguments, indent, sameLine );
    return sb;
  }
}
