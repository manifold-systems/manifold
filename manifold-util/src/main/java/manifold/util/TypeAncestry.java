/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.util;

import manifold.util.concurrent.ConcurrentWeakHashMap;

import java.lang.reflect.Array;
import java.util.*;

public class TypeAncestry
{
  private static TypeAncestry INSTANCE;

  public static TypeAncestry instance()
  {
    return INSTANCE == null ? INSTANCE = new TypeAncestry() : INSTANCE;
  }

  private final Map<Class, Set<Class>> _ancestry;


  private TypeAncestry()
  {
    _ancestry = new ConcurrentWeakHashMap<>();
  }

  public Set<Class> getTypesInAncestry( Class type )
  {
    return _ancestry.computeIfAbsent( type,
      t -> {
        if( t.isArray() )
        {
          Set<Class> types = getAllClassesInClassHierarchyAsIntrinsicTypes( t );
          types.addAll( new HashSet<>( getArrayVersionsOfEachType( getTypesInAncestry( t.getComponentType() ) ) ) );
          return Collections.unmodifiableSet( types );
        }
        else
        {
          Set<Class> types = getAllClassesInClassHierarchyAsIntrinsicTypes( t );
          return Collections.unmodifiableSet( types );
        }
      } );
  }

  public static Set<Class> getAllClassesInClassHierarchyAsIntrinsicTypes( Class type )
  {
    Set<Class> classSet = new HashSet<>();
    addAllClassesInClassHierarchy( type, classSet );
    classSet.add( Object.class );
    return classSet;
  }

  private static void addAllClassesInClassHierarchy( Class type, Set<Class> set )
  {
    if( !set.add( type ) )
    {
      return;
    }

    Class[] interfaces = type.getInterfaces();
    for( Class interfaceClass : interfaces )
    {
      addAllClassesInClassHierarchy( interfaceClass, set );
    }

    Class superClass = type.getSuperclass();
    if( superClass != null )
    {
      addAllClassesInClassHierarchy( superClass, set );
    }
  }

  public static Set<Class> getArrayVersionsOfEachType( Set<Class> componentTypes )
  {
    Set<Class> allTypes = new HashSet<>();
    allTypes.add( Object.class );
    for( Class type : componentTypes )
    {
      allTypes.add( Array.newInstance( type, 0 ).getClass() );
    }
    return allTypes;
  }
}
