package manifold.api.json;

import java.util.Collections;
import java.util.List;

/**
 */
public interface IJsonType
{
  String getName();

  String getIdentifier();

  IJsonParentType getParent();

  default List<IJsonType> getDefinitions()
  {
    return Collections.emptyList();
  }

  default void setDefinitions( List<IJsonType> definitions )
  {
  }

  /**
   * JSon Schema types normally compare by identity, however for
   * some use-cases we still need to compare them structurally e.g.,
   * for merging types.
   */
  default boolean equalsStructurally( IJsonType type2 )
  {
    return equals( type2 );
  }
}
