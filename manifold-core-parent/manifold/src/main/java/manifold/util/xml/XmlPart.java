package manifold.util.xml;

public abstract class XmlPart
{
  private final XmlPart _parent;
  private final int _offset;
  private final int _length;
  private final int _line;

  XmlPart( XmlPart parent, int offset, int length, int line )
  {
    _parent = parent;
    _offset = offset;
    _length = length;
    _line = line;
  }

  public XmlPart getParent()
  {
    return _parent;
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getLength()
  {
    return _length;
  }

  public int getLine()
  {
    return _line;
  }
}
