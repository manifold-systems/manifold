package manifold.api.json.schema;

import java.util.List;
import java.util.function.Supplier;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;

public class LazyRefJsonType implements IJsonType
{
  private final Supplier<IJsonType> _supplier;

  public LazyRefJsonType( Supplier<IJsonType> supplier )
  {
    _supplier = supplier;
  }

  public IJsonType resolve()
  {
    IJsonType type = _supplier.get();
    while( type instanceof LazyRefJsonType )
    {
      type = ((LazyRefJsonType)type).resolve();
    }
    return type;
  }

  @Override
  public String getName()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getIdentifier()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IJsonParentType getParent()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<IJsonType> getDefinitions()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDefinitions( List<IJsonType> definitions )
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equalsStructurally( IJsonType type2 )
  {
    throw new UnsupportedOperationException();
  }
}
