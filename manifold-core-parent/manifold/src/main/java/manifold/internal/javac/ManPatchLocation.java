/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import javax.tools.JavaFileManager;
import manifold.util.ReflectUtil;

public class ManPatchLocation implements JavaFileManager.Location
{
  private final GeneratedJavaStubFileObject _fo;

  ManPatchLocation( GeneratedJavaStubFileObject fo )
  {
    _fo = fo;
  }

  @Override
  public String getName()
  {
    return _fo.getName();
  }

  @Override
  public boolean isOutputLocation()
  {
    return false;
  }

  public String inferModuleName( Context ctx )
  {
    Names names = Names.instance( ctx );
    GeneratedJavaStubFileObject fo = _fo;
    String packageName = getPackageName( fo );

    JavacElements elementUtils = JavacElements.instance( ctx );
    for( Object /*ModuleElement*/ ms : (Iterable)ReflectUtil.method( elementUtils, "getAllModuleElements" ).invoke() )
    {
      if( (boolean)ReflectUtil.method( ms, "isUnnamed" ).invoke() )
      {
        continue;
      }

      if( ms.getClass().getSimpleName().equals( "ModuleSymbol" ) )
      {
        //noinspection unchecked
        for( Symbol pkg : (Iterable<Symbol>)ReflectUtil.field( ms, "enclosedPackages" ).get() )
        {
          if( !(pkg instanceof Symbol.PackageSymbol) )
          {
            continue;
          }
          if( pkg.toString().equals( packageName ) )
          {
            //noinspection unchecked,RedundantCast
            Iterable<Symbol> symbolsByName = (Iterable<Symbol>)ReflectUtil.method( ReflectUtil.method( pkg, "members" ).invoke(), "getSymbolsByName", Name.class ).invoke( names.fromString( getClassName( fo ) ) );
            if( symbolsByName.iterator().hasNext() )
            {
              return ReflectUtil.method( ms, "getQualifiedName" ).invoke().toString();
            }
          }
        }
      }
    }
    return null;
  }

  private String getPackageName( GeneratedJavaStubFileObject fo )
  {
    String name = fo.getName();
    int iLast = name.lastIndexOf( '/' );
    if( iLast >= 0 )
    {
      name = name.substring( 0, iLast );
      return name.replace( '/', '.' );
    }
    return "";
  }

  private String getClassName( GeneratedJavaStubFileObject fo )
  {
    String name = fo.getName();
    name = name.substring( name.lastIndexOf( '/' ) + 1, name.lastIndexOf( '.' ) );
    return name;
  }
}
