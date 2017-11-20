package manifold.api.json;

/**
 */
public class DynamicType implements IJsonType
{
  private static final DynamicType INSTANCE = new DynamicType();

  public static DynamicType instance()
  {
    return INSTANCE;
  }

  private DynamicType()
  {
  }

  @Override
  public String getName()
  {
    return "Dynamic";
  }

  @Override
  public String getIdentifier()
  {
    return "Object";
  }

  @Override
  public IJsonParentType getParent()
  {
    return null;
  }
}
