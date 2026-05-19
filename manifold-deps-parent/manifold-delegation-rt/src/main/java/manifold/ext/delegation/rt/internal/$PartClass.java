package manifold.ext.delegation.rt.internal;

import manifold.ext.delegation.rt.api.DelegationLinkageError;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.rt.api.Structural;
import manifold.util.ReflectUtil;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static manifold.util.DebugModeUtil.isJdwpEnabled;

/**
 * For internal use only!
 * <p/>
 * This is a multipurpose interface:<br>
 * - As a performance measure it is used as a marker interface to save from using slower annotation reflection to identify `@part` classes<br>
 * - The compiler adds this interface to `@part` class' `implements` list to enable generated code to cast and call `linkPart`<br>
 * - It hosts the `Internal` static methods generated code uses for implementation details.<br>
 */
public interface $PartClass
{
  /**
   * The compiler generates the implementation of this method per `@part` class.
   * <p>
   * A call to `Internal#linkPart()` is generated where a delegate is assigned to a `@link` field. In turn, `Internal#linkPart()`
   * calls this method to write `root` into the appropriate `$selves[]` slots and recursively propagates through nested
   * `@link` fields - this is how arbitrary DAG topologies work.
   *
   * @param root      A delegating class
   * @param linkScope The interfaces `root` links/delegates to this part
   */
  @internal
  void $linkPartToSelf( Object root, Class<?>[] linkScope );

  class Internal {
    // called from generated code.
    @SuppressWarnings( "unused" )
    public static Object linkPart( Object root, Class<?>[] linkScope, String fieldName, Object delegate )
    {
      if( delegate == null )
      {
        throw new IllegalStateException( "Delegate class instance is null for field `" + fieldName + "'" );
      }

      if( isJdwpEnabled() )
      {
        // this check is somewhat expensive, enabled only when VM is in debug mode
        checkInternalInterfaces( root, fieldName, linkScope, delegate );
      }

      if( delegate instanceof $PartClass )
      {
        (($PartClass)delegate).$linkPartToSelf( root, linkScope );
      }
      return delegate;
    }

    private static void checkInternalInterfaces( Object root, String fieldName, Class<?>[] linkScope, Object delegate )
    {
      List<Class<?>> exposed = getExposedInterfaces( delegate.getClass() );
      for( Class<?> linkIface : linkScope )
      {
        if( linkIface.isAnnotationPresent( Structural.class ) )
        {
          continue;
        }

        if( exposed.stream().noneMatch( cls -> linkIface.isAssignableFrom( cls ) ) )
        {
          throw new DelegationLinkageError(
            "Linked field '" + root.getClass().getTypeName() + "#" + fieldName + "'\n" +
            "delegates interface '" + linkIface.getTypeName() + "'\n" +
            "which is declared @internal to class '" + delegate.getClass().getTypeName() + "'" );
        }
      }
    }

    private static List<Class<?>> getExposedInterfaces( Class<?> delegateClass )
    {
      Class<?>[] delegateIfaces = delegateClass.getInterfaces();
      Set<Class<?>> exposed = new LinkedHashSet<>();
      AnnotatedType[] annotatedInterfaces = delegateClass.getAnnotatedInterfaces();
      for( int i = 0; i < annotatedInterfaces.length; i++ )
      {
        AnnotatedType annoIface = annotatedInterfaces[i];
        if( !annoIface.isAnnotationPresent( internal.class ) )
        {
          exposed.add( delegateIfaces[i] );
        }
      }
      Class<?> superclass = delegateClass.getSuperclass();
      if( superclass != null && superclass != Object.class )
      {
        exposed.addAll( getExposedInterfaces( superclass ) );
      }
      return new ArrayList<>( exposed );
    }

    // called from generated code
    @SuppressWarnings("unused")
    public static void reportCycle( $PartClass part, Object root, Class<?> iface )
    {
      throw new DelegationLinkageError( "Cycle detected in delegation graph.\n" +
                                        "Interface: " + iface.getTypeName() + "\n" +
                                        "delegated by root: '" + root.getClass().getTypeName() + "'\n" +
                                        "is already wired into part: `" + part.getClass().getTypeName() + "'" );
    }

    // called from generated code
    @SuppressWarnings("unused")
    public static Class<?>[] intersect( Class<?>[] a, Class<?>[] b )
    {
      List<Class<?>> result = new ArrayList<>();
      for( Class<?> ca : a )
      {
        for( Class<?> cb : b )
        {
          if( ca == cb )
          {
            result.add( ca );
            break;
          }
        }
      }
      return result.toArray( new Class<?>[0] );
    }

    // called from generated code
    @SuppressWarnings("unused")
    public static Object invokeDefault( Object receiver, Class<?> iface, String name, Object paramsArray, Object argsArray )
    {
      return ReflectUtil.invokeDefault( receiver, iface, name, (Class<?>[])paramsArray, (Object[])argsArray );
    }
  }
}
