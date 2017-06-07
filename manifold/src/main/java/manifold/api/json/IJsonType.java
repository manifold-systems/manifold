package manifold.api.json;

import java.util.Collections;
import java.util.List;

/**
*/
public interface IJsonType
{
  String getName();
  IJsonParentType getParent();

  default List<IJsonType> getDefinitions()
  {
    return Collections.emptyList();
  }
  default void setDefinitions( List<IJsonType> definitions )
  {
  }
}
