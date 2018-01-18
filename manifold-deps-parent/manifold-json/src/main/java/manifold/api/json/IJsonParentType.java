package manifold.api.json;

import java.util.List;

/**
 */
public interface IJsonParentType extends IJsonType
{
  void addChild( String name, IJsonParentType child );

  IJsonType findChild( String name );

  List<JsonIssue> getIssues();
  void addIssue( JsonIssue issue );

  void render( StringBuilder sb, int indent, boolean mutable );
}
