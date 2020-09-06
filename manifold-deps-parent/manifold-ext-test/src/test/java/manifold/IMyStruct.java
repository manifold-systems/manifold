package manifold;

import manifold.rt.api.ActualName;
import manifold.ext.rt.api.Structural;

/**
 * test interface for MyMapExt
 */
@Structural
public interface IMyStruct
{
  int getAge();
  void setAge( int age );

  @ActualName( "name" )
  String getName();
  @ActualName( "name" )
  void setName( String name );

  char charAt( int i );
}
