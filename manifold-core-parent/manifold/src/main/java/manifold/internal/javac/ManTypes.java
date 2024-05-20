package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import manifold.rt.api.util.TypesUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Flags.PUBLIC;

public interface ManTypes
{
  ThreadLocal<Map<Pair<Type, Type>, Boolean>> CACHED_PAIRS = ThreadLocal.withInitial( HashMap::new );

  Types types();

  /**
   * Is t structurally assignable to structural interface s?
   * <p/>
   * Note, this tests the _erased_ types of t and s. Handling generics here is a bridge too for, for now.
   */
  default boolean isAssignableToStructuralType( Type t, Type s )
  {
    try
    {
      return _isAssignableToStructuralType( t, s );
    }
    catch( Throwable e )
    {
      // todo: diagnose this
      //  as a diagnostic, prevent this check from causing an exception to quietly test if it is causing compilation failure
      return false;
    }
  }
  default boolean _isAssignableToStructuralType( Type t, Type s )
  {
    t = types().erasure( t );
    s = types().erasure( s );

    if( !(t instanceof Type.ClassType) )
    {
      return false;
    }

    if( !TypesUtil.isStructuralInterface( types(), s.tsym ) )
    {
      return false;
    }

    Map<Pair<Type, Type>, Boolean> cache = CACHED_PAIRS.get();
    Pair<Type, Type> pair = new Pair<>( t, s );
    if( cache.containsKey( pair ) )
    {
      // short-circuit null, actual result comes from initial frame
      Boolean result = cache.get( pair );
      return result == null || result;
    }
    cache.put( pair, null );
    try
    {
      // check if all non-default public instance methods in (erased) structural interface `s` are provided in type `t`

      Set<MethodSymbol> sMethods = new HashSet<>();
      getAllMethods( s, m -> !m.isStatic() && (m.flags() & DEFAULT) == 0 && (m.flags() & PUBLIC) != 0 && (m.flags() & SYNTHETIC) == 0, sMethods );

      Set<MethodSymbol> tMethods = new HashSet<>();
      getAllMethods( t, m -> !m.isStatic() && (m.flags() & PUBLIC) != 0, tMethods );

      boolean result = sMethods.stream()
        .allMatch( m -> tMethods.stream()
          .anyMatch( tm -> m.flatName().equals( tm.flatName() ) &&
            types().isAssignable( types().erasure( tm.getReturnType() ), types().erasure( m.getReturnType() ) ) &&
            hasStructurallyEquivalentArgs( tm, m ) ) );
      cache.put( pair, result );
      return result;
    }
    finally
    {
      cache.remove( pair );
    }
  }

  static void getAllMethods( Type t, Predicate<MethodSymbol> filter, Set<MethodSymbol> tMethods )
  {
    if( !(t instanceof Type.ClassType) )
    {
      return;
    }

    ClassSymbol tsym = (ClassSymbol)t.tsym;
    for( Symbol sym : IDynamicJdk.instance().getMembers( tsym, m -> m instanceof MethodSymbol && filter.test( (MethodSymbol)m ) ) )
    {
      tMethods.add( (MethodSymbol)sym );
    }

    getAllMethods( tsym.getSuperclass(), filter, tMethods );
    for( Type iface : tsym.getInterfaces() )
    {
      getAllMethods( iface, filter, tMethods );
    }
  }

  default boolean hasStructurallyEquivalentArgs( MethodSymbol t, MethodSymbol s )
  {
    List<Symbol.VarSymbol> tParams = t.getParameters();
    List<Symbol.VarSymbol> sParams = s.getParameters();
    if( tParams.size() != sParams.size() )
    {
      return false;
    }
    for( int i = 0; i < sParams.size(); i++ )
    {
      Symbol.VarSymbol sParam = sParams.get( i );
      Symbol.VarSymbol tParam = tParams.get( i );
      if( !types().isAssignable( sParam.type, tParam.type ) )
      {
        return false;
      }
    }
    return true;
  }
}
