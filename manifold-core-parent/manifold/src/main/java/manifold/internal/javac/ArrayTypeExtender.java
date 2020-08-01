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

package manifold.internal.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import manifold.rt.api.Array;
import manifold.util.ReflectUtil;

public class ArrayTypeExtender
{
  /**
   * Array types in Java all share the same symbol, {@link Symtab#arrayClass}. For example, {@code int[]}, {@code String[]},
   * {@code Foo[]}, and {@code long[][]} all share the same symbol instance. Also, Java has no base type for the array
   * class; there's no "java.lang.Array" to add extension methods to. Therefore, Manifold provides a substitute type for
   * the sole purpose of adding extension methods, namely {@code manifold.rt.api.Array}. Extension classes extending
   * this class effectively extend Java's array class.
   */
  public static void extend( Context context, CompilationUnitTree compilingClass )
  {
    Symbol.ClassSymbol ourArrayClass = IDynamicJdk.instance().getTypeElement( context, compilingClass, Array.class.getTypeName() );
    if( ourArrayClass == null )
    {
      return;
    }

    if( (int)ReflectUtil.field( ReflectUtil.field( ourArrayClass, "members_field" ).get(), "nelems" ).get() > 1 )
    {
      Symbol.ClassSymbol arrayClass = Symtab.instance( context ).arrayClass;
      for( Symbol sym : IDynamicJdk.instance().getMembers( ourArrayClass ) )
      {
        // Add extensions directly to array class
        if( sym instanceof Symbol.MethodSymbol && sym.hasAnnotations() )
        {
          Symbol clone = sym.clone( arrayClass );
          // copy annotations (they are not cloned for some reason)
          ReflectUtil.field( clone, "metadata" ).set( ReflectUtil.field( sym, "metadata" ).get() );
          // now enter the method symbol into the array type directly
          ReflectUtil.method( ReflectUtil.field( arrayClass, "members_field" ).get(), "enter", Symbol.class )
            .invoke( clone );
        }
      }
    }
  }
}
