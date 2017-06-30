package manifold.api.gen;

import java.lang.reflect.Modifier;

/**
 */
public class SrcConstructor extends SrcStatement<SrcConstructor>
{
  private SrcStatementBlock _body;

  public SrcConstructor( SrcClass owner )
  {
    super( owner );
  }

  public SrcConstructor()
  {
  }

  public SrcConstructor body( SrcStatementBlock body )
  {
    _body = body;
    return this;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    renderModifiers( sb, false, Modifier.PUBLIC );
    sb.append( getOwner().getSimpleName() ).append( renderParameters( sb ) );
    _body.render( sb, indent );
    return sb;
  }
}
