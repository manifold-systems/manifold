package manifold;

import manifold.ext.api.Structural;

/**
 * test interface for MyMapExt
 */
@Structural
public interface IMyStruct
{
  int getAge();
  void setAge( int age );

  char charAt( int i );
}
