package manifold.util;

import java.util.AbstractList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpMatch extends AbstractList<String> implements List<String>
{
  private Matcher _matcher;

  public RegExpMatch( Matcher matcher )
  {
    _matcher = matcher;
  }

  public String get( int index )
  {
    return _matcher.group( index + 1 );
  }

  public int size()
  {
    return _matcher.groupCount();
  }

  public Matcher getMatcher()
  {
    return _matcher;
  }

  public Pattern getPattern()
  {
    return _matcher.pattern();
  }

  /**
   * @deprecated RegExpMatch now implements List&lt;String>, so it is
   * no longer necessary to call getGroups()
   */
  public List<String> getGroups()
  {
    return this;
  }

  @Override
  public String toString()
  {
    return "[RegExpMatch: " + _matcher + "]";
  }
}
