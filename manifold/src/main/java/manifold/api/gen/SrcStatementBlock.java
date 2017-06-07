package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcStatementBlock extends SrcStatement<SrcStatementBlock>
{
  private List<SrcStatement> _statements = new ArrayList<>();

  public SrcStatementBlock addStatement( SrcStatement stmt )
  {
    _statements.add( stmt );
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, true );
  }
  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    if( sameLine )
    {
      sb.append( " {\n" );
    }
    else
    {
      sb.append( indent( sb, indent ) ).append( "{\n" );
    }
    for( SrcStatement stmt: _statements )
    {
      stmt.render( sb, indent+INDENT );
    }
    sb.append( indent( sb, indent ) ).append( "}\n" );
    return sb;
  }
}
