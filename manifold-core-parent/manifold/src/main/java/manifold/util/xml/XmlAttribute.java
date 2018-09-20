package manifold.util.xml;

import manifold.util.ManStringUtil;
import manifold.util.xml.gen.XMLParser;

public class XmlAttribute extends XmlNamedPart
{
  private XmlTerminal _value;

  XmlAttribute( XMLParser.AttributeContext ctx, XmlElement parent )
  {
    super( parent, ctx.start.getStartIndex(), ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1, ctx.start.getLine() );
    setName( new XmlTerminal( ctx.Name().getSymbol(), this ) );
  }

  public String getValue()
  {
    XmlTerminal rawValue = getRawValue();
    return rawValue == null ? null : ManStringUtil.unquote( rawValue.getRawText() );
  }

  public XmlTerminal getRawValue()
  {
    return _value;
  }
  void setRawValue( XmlTerminal value )
  {
    _value = value;
    if( value.getParent() != this )
    {
      throw new IllegalStateException( "Parent mismatch" );
    }
  }
}
