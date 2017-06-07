package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcMethodCallExpression extends SrcExpression
{
  private List<SrcArgument> _arguments = new ArrayList<>();

  public SrcMethodCallExpression( String name )
  {
    name( name );
  }

  public SrcMethodCallExpression addArgument( SrcArgument arg )
  {
    _arguments.add( arg );
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( getSimpleName() );
    renderArgumenets( sb, _arguments, indent, true );
    return sb;
  }
}
