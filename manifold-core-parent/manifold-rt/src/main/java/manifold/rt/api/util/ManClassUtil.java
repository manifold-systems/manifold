/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.rt.api.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is in part derived from org.apache.commons.lang.ClassUtils and is intended
 * to break a dependency on that project.
 */
public class ManClassUtil
{
  public static String getNameNoPackage( String className )
  {
    if( className == null )
    {
      return null;
    }
    int packageIndex = className.lastIndexOf( '.' ) + 1;
    int innerClassIndex = className.lastIndexOf( '$' ) + 1;
    int index = Math.max( packageIndex, innerClassIndex );
    return className.substring( index );
  }

  /**
   */
  public static String getPackage( String className )
  {
    if( className == null || !className.contains( "." ) )
    {
      return "";
    }
    int packageIndex = className.lastIndexOf( '.' );
    if( packageIndex < 0 )
    {
      return "";
    }
    return className.substring( 0, packageIndex );
  }

  public static String getFileExtension( String name )
  {
    int iIndex = name.lastIndexOf( '.' );
    if( iIndex >= 0 )
    {
      return name.substring( iIndex );
    }
    return null;
  }

  public static String getFileExtension( File file )
  {
    String name = file.getName();
    return getFileExtension( name );
  }

  /**
   * <p>Gets the class name minus the package name from a <code>Class</code>.</p>
   *
   * @param cls the class to get the short name for.
   *
   * @return the class name without the package name or an empty string
   */
  public static String getShortClassName( Class cls )
  {
    if( cls == null )
    {
      return "";
    }
    return getShortClassName( cls.getName() );
  }

  /**
   * <p>Gets the class name minus the package name from a String.</p>
   * <p/>
   * <p>The string passed in is assumed to be a class name - it is not checked.</p>
   *
   * @param className the className to get the short name for
   *
   * @return the class name of the class without the package name or an empty string
   */
  public static String getShortClassName( String className )
  {
    if( className == null )
    {
      return "";
    }
    if( className.length() == 0 )
    {
      return "";
    }

    int lastDotIdx = className.lastIndexOf( "." );
    int innerIdx = className.indexOf(
      "$", lastDotIdx == -1 ? 0 : lastDotIdx + 1 );
    String out = className.substring( lastDotIdx + 1 );
    if( innerIdx != -1 )
    {
      out = out.replace( "$", "." );
    }
    return out;
  }

  public static Set<Class> getAllInterfaces( Class c )
  {
    return getAllInterfacesImpl( c, new HashSet<Class>() );
  }

  private static Set<Class> getAllInterfacesImpl( Class c, HashSet<Class> hashSet )
  {
    if( c == null || hashSet.contains( c ) )
    {
      return hashSet;
    }
    else
    {
      if( c.isInterface() )
      {
        hashSet.add( c );
      }
      getAllInterfacesImpl( c.getSuperclass(), hashSet );
      for( Class iface : c.getInterfaces() )
      {
        getAllInterfacesImpl( iface, hashSet );
      }
    }
    return hashSet;
  }

  public static boolean isJavaIdentifier( String part )
  {
    if( part.length() == 0 )
    {
      return false;
    }
    else if( !Character.isJavaIdentifierStart( part.charAt( 0 ) ) )
    {
      return false;
    }
    else
    {
      for( int i = 1; i < part.length(); ++i )
      {
        if( !Character.isJavaIdentifierPart( part.charAt( i ) ) )
        {
          return false;
        }
      }

      return true;
    }
  }

  public static boolean isValidClassName( String name )
  {
    while( name.length() != 0 )
    {
      int iDot = name.indexOf( '.' );
      if( iDot < 0 )
      {
        iDot = name.length();
      }
      if( !isJavaIdentifier( name.substring( 0, iDot ) ) )
      {
        return false;
      }
      name = name.substring( iDot );
      if( name.length() != 0 )
      {
        name = name.substring( 1 );
      }
    }
    return true;
  }

  public static Class<?> box( Class<?> type )
  {
    if( type == boolean.class )
    {
      return Boolean.class;
    }
    if( type == byte.class )
    {
      return Byte.class;
    }
    if( type == char.class )
    {
      return Character.class;
    }
    if( type == short.class )
    {
      return Short.class;
    }
    if( type == int.class )
    {
      return Integer.class;
    }
    if( type == long.class )
    {
      return Long.class;
    }
    if( type == float.class )
    {
      return Float.class;
    }
    if( type == double.class )
    {
      return Double.class;
    }
    throw new IllegalStateException();
  }
}
