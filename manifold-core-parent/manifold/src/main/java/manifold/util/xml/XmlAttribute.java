package manifold.util.xml;

import manifold.util.xml.gen.XMLParser;

public class XmlAttribute extends XmlNamedPart
{
  private XmlTerminal _value;

  XmlAttribute( XMLParser.AttributeContext ctx, XmlElement parent )
  {
    super( parent, ctx.start.getStartIndex(), ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1, ctx.start.getLine() );
    setName( new XmlTerminal( ctx.Name().getSymbol(), this ) );
  }

  public XmlTerminal getValue()
  {
    return _value;
  }
  void setValue( XmlTerminal value )
  {
    _value = value;
    if( value.getParent() != this )
    {
      throw new IllegalStateException( "Parent mismatch" );
    }
  }
}
