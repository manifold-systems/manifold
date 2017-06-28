package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcMethodCallExpression extends SrcExpression<SrcMethodCallExpression>
{
  private List<SrcArgument> _arguments = new ArrayList<>();

  public SrcMethodCallExpression( String name )
  {
    name( name );
  }

  @Override
  public SrcMethodCallExpression copy()
  {
    SrcMethodCallExpression expr = new SrcMethodCallExpression( getSimpleName() );
    for( SrcArgument arg: _arguments )
    {
      expr.addArgument( arg.copy() );
    }
    return expr;
  }

  public List<SrcArgument> getArguments()
  {
    return _arguments;
  }

  public SrcMethodCallExpression addArgument( SrcArgument arg )
  {
    _arguments.add( arg );
    arg.setOwner( this );
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
