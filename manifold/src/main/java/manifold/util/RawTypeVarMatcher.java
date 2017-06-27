package manifold.util;

import java.lang.reflect.TypeVariable;

/**
 */
public class RawTypeVarMatcher implements TypeVarToTypeMap.ITypeVarMatcher<TypeVariable>
{
  private static final RawTypeVarMatcher INSTANCE = new RawTypeVarMatcher();

  public static RawTypeVarMatcher instance() {
    return INSTANCE;
  }

  private RawTypeVarMatcher() {
  }

  @Override
  public boolean matches( TypeVariable thisOne, TypeVariable thatOne )
  {
    return thisOne.equals( thatOne );
  }
}
