package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SrcRawStatement extends SrcStatement<SrcRawStatement>
{
  private List<String> _text = new ArrayList<>();

  public SrcRawStatement()
  {
    super();
  }

  public SrcRawStatement( SrcStatementBlock owner )
  {
    super( owner );
  }

  public SrcRawStatement rawText( String text )
  {
    _text.add( text );
    return this;
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean sameLine )
  {
    for( String text : _text )
    {
      indent( sb, indent );
      sb.append( text ).append( "\n" );
    }
    return sb;
  }
}
