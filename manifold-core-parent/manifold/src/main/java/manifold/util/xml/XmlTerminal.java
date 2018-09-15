package manifold.util.xml;

import org.antlr.v4.runtime.Token;

public class XmlTerminal extends XmlPart
{
  private final String _text;

  XmlTerminal( Token t, XmlPart parent )
  {
    super( parent, t.getStartIndex(), t.getStopIndex() - t.getStartIndex() + 1, t.getLine() );
    _text = t.getText();
  }

  public String getText()
  {
    return _text;
  }
}
