package manifold.util.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import manifold.util.xml.gen.XMLParser;

public class XmlElement extends XmlNamedPart
{
  private List<XmlElement> _children;
  private Map<String, XmlAttribute> _attributes;
  private XmlTerminal _content;

  XmlElement( XMLParser.ElementContext ctx, XmlElement parent )
  {
    super( parent, ctx.start.getStartIndex(), ctx.stop.getStopIndex() - ctx.start.getStartIndex() + 1, ctx.start.getLine() );
    setName( new XmlTerminal( ctx.Name( 0 ).getSymbol(), this ) );
    _children = Collections.emptyList();
    _attributes = Collections.emptyMap();
  }

  public List<XmlElement> getChildren()
  {
    return _children;
  }
  void addChild( XmlElement child )
  {
    if( _children.isEmpty() )
    {
      _children = new ArrayList<>();
    }
    _children.add( child );
  }

  /**
   * @return attributes ordered by appearance in the parent element
   */
  public Map<String, XmlAttribute> getAttributes()
  {
    return _attributes;
  }
  void addAttribute( XmlAttribute attr )
  {
    if( _attributes.isEmpty() )
    {
      _attributes = new LinkedHashMap<>();
    }
    _attributes.put( attr.getName().getText(), attr );
  }

  public XmlTerminal getContent()
  {
    return _content;
  }
  void setContent( XmlTerminal content )
  {
    _content = content;
    if( content.getParent() != this )
    {
      throw new IllegalStateException( "Parent mismatch" );
    }
  }
}
