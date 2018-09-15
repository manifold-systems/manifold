package manifold.util.xml;

public class XmlNamedPart extends XmlPart
{
  private XmlTerminal _name;

  XmlNamedPart( XmlPart parent, int offset, int length, int line )
  {
    super( parent, offset, length, line );
  }

  public XmlTerminal getName()
  {
    return _name;
  }
  void setName( XmlTerminal name )
  {
    _name = name;
  }
}
