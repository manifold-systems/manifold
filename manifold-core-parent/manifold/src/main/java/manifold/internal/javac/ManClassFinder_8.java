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
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import manifold.util.ReflectUtil;


public class ManClassFinder_8 extends ClassReader
{
  public static ManClassFinder_8 instance(Context ctx )
  {
    ReflectUtil.LiveFieldRef fieldRef = ReflectUtil.field( Resolve.instance( ctx ) , "reader" );
    Object classReader = fieldRef.get();
    if( !(classReader instanceof ManClassFinder_8) )
    {
      classReader = new ManClassFinder_8( ctx, (ClassReader)classReader );
      fieldRef.set(classReader);
    }

    return (ManClassFinder_8)classReader;
  }

  private final ClassReader _reader;

  private ManClassFinder_8(Context ctx, ClassReader reader )
  {
    super( ctx, false );
    _reader = reader;
  }

  @Override
  public Symbol.PackageSymbol enterPackage(Name name) {
    return _reader.enterPackage(name);
  }

  @Override
  public Symbol.ClassSymbol loadClass(Name name) throws Symbol.CompletionFailure
  {
    Symbol.ClassSymbol symbol = _reader.loadClass( name );
    if( TypeAliasTranslator.IMPORTER != null )
    {
      TypeAliasTranslator.IMPORTER.accept( name, symbol );
    }
    return symbol;
  }
}