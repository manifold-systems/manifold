/*
 * Copyright (c) 2023 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext.delegation.rt;

import manifold.ext.delegation.rt.api.link;
import manifold.util.ReflectUtil;

import java.util.*;

import static manifold.rt.api.util.ManClassUtil.getAllInterfaces;

public class RuntimeMethods
{
  public static final String SELF_FIELD = "$theSelf";
  public static final String COVERED_FIELD = "$interfacesFullyCovered";

  /**
   * This method is called from generated code.
   */
  @SuppressWarnings( "unused" )
  public static Object linkPart( Object delegatingClass, String fieldName, Object part )
  {
    if( part == null )
    {
      throw new IllegalStateException( "Part class instance is null for field `" + fieldName + "'" );
    }

    if( delegatingClass == null )
    {
      throw new IllegalStateException(
        "Delegating class instance is null when assigned to field '" + fieldName + "' of part class '" + part.getClass().getTypeName() );
    }

    // link the part to self
    linkPartToSelf( delegatingClass, part );
    return part;
  }

  private static void linkPartToSelf( Object delegatingClass, Object part )
  {
    ReflectUtil.LiveFieldRef self = ReflectUtil.WithNull.field( part, SELF_FIELD );
    if( self == null )
    {
      return;
    }

    if( areAllPartInterfacesLinked( delegatingClass, part ) )
    {
      ReflectUtil.LiveFieldRef covered = ReflectUtil.WithNull.field( part, COVERED_FIELD );
      if( covered == null )
      {
        return;
      }
      covered.set( true );
    }

    self.set( delegatingClass );

    for( Class<?> superclass = part.getClass().getSuperclass(); superclass != null; superclass = superclass.getSuperclass() )
    {
      if( superclass == Object.class )
      {
        break;
      }
      ReflectUtil.field( superclass, SELF_FIELD ).set( part, delegatingClass );
    }

    ReflectUtil.fields( part, f -> !f.isStatic() && f.getField().getAnnotation( link.class ) != null )
      .forEach( delegateField -> {
        Object partDelegate = delegateField.get();
        linkPartToSelf( delegatingClass, partDelegate );
      } );
  }

  private static boolean areAllPartInterfacesLinked( Object delegatingClass, Object part )
  {
    Set<Class> partInterfaces = getAllInterfaces( part.getClass() );
    int partInterfaceCount = partInterfaces.size();
    partInterfaces.retainAll( getAllInterfaces( delegatingClass.getClass() ) );
    return partInterfaces.size() == partInterfaceCount;
  }

  /**
   * This method is called from generated code.
   */
  @SuppressWarnings( "unused" )
  public static Object invokeDefault( Object receiver, Class iface, String name, Object paramsArray, Object argsArray )
  {
    return ReflectUtil.invokeDefault( receiver, (Class<?>)iface, name, (Class<?>[])paramsArray, (Object[])argsArray );
  }

  /**
   * This method is called from generated code.
   */
  @SuppressWarnings( "unused" )
  public static Object replaceThis( Class<?> iface, Object self, Object this_ )
  {
    if( iface == null )
    {
      throw new IllegalArgumentException( "Interface class is null" );
    }

    if( !iface.isInterface() )
    {
      throw new IllegalStateException( "Expecting an interface type" );
    }

    if( self == null )
    {
      return this_;
    }

    return linksInterfaceTo( iface, self, this_ ) ? self : this_;
  }

  /**
   * This method is called from generated code.
   */
  @SuppressWarnings( "unused" )
  public static boolean linksInterfaceTo( Class<?> iface, Object from, Object to )
  {
    if( (boolean)ReflectUtil.field( to, COVERED_FIELD ).get() )
    {
      return true;
    }
    return _linksInterfaceTo( iface, from, to );
  }
  private static boolean _linksInterfaceTo( Class<?> iface, Object from, Object to )
  {
    if( from == null )
    {
      return false;
    }

    for( ReflectUtil.LiveFieldRef f : ReflectUtil.fields( from, f -> !f.isStatic() ) )
    {
      link linkAnno = f.getField().getAnnotation( link.class );
      if( linkAnno == null )
      {
        continue;
      }

      Object value = f.get();
      if( !iface.isInstance( value ) )
      {
        continue;
      }

      Class<?>[] annoTypes = linkAnno.value();
      if( annoTypes != null && annoTypes.length > 0 )
      {
        if( Arrays.stream( annoTypes ).anyMatch( t -> iface.isAssignableFrom( t ) ) )
        {
          if( value == to || linksInterfaceTo( iface, value, to ) )
          {
            return true;
          }
        }
      }
      else
      {
        Class<?> type = f.getField().getType();
        if( iface.isAssignableFrom( type ) )
        {
          if( value == to || linksInterfaceTo( iface, value, to ) )
          {
            return true;
          }
        }
      }
    }
    return false;
  }
}
