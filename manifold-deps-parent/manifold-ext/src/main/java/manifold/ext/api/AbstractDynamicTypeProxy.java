package manifold.ext.api;

/**
 */
public class AbstractDynamicTypeProxy
{
  private Object _root;

  public AbstractDynamicTypeProxy( Object root )
  {
    _root = root;
  }

  public Object getRoot()
  {
    return _root;
  }
}
