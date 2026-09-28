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

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.jvm.Code;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.OutputStream;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.util.List;
import manifold.api.type.ISelfCompiledFile;
import manifold.util.ReflectUtil;

public class ManClassWriter extends ClassWriter
{
  private static final String GENERATED = "parts.rt.internal.Generated";

  public static ManClassWriter instance( Context ctx )
  {
    ClassWriter classWriter = ctx.get( classWriterKey );
    if( !(classWriter instanceof ManClassWriter) )
    {
      ctx.put( classWriterKey, (ClassWriter)null );
      classWriter = new ManClassWriter( ctx );
    }

    return (ManClassWriter)classWriter;
  }

  private ManClassWriter( Context ctx )
  {
    super( ctx );
    ReflectUtil.field( JavaCompiler.instance( ctx ), "writer" ).set( this );
  }

  @Override
  public void writeClassFile( OutputStream out, Symbol.ClassSymbol c ) throws StringOverflow, IOException, PoolOverflow
  {
//    removeLineInfoForGeneratedMethods( c );

    JavaFileObject sourceFile = c.sourcefile;
    if( sourceFile instanceof ISelfCompiledFile && ((ISelfCompiledFile)sourceFile).isSelfCompile( c.getQualifiedName().toString() ) )
    {
      out.write( ((ISelfCompiledFile)sourceFile).compile( c.getQualifiedName().toString() ) );
    }
    else
    {
      super.writeClassFile( out, c );
    }
  }

  // remove the line info for generated methods so that debuggers will not try to land on invisible methods
  private static void removeLineInfoForGeneratedMethods( Symbol.ClassSymbol c )
  {
    for (Symbol s : IDynamicJdk.instance().getMembers( c ) )
    {
      if( s instanceof Symbol.MethodSymbol )
      {
        Symbol.MethodSymbol m = (Symbol.MethodSymbol)s;
        if( m.code != null && isGenerated( m ) )
        {
          // clear line numbers so debugger will not step into invisible generated methods
          ReflectUtil.field( Code.class, "lineInfo" ).set( m.code, List.nil() );
        }
      }
    }
  }

  private static boolean isGenerated( Symbol.MethodSymbol m )
  {
    for( Attribute.Compound a : m.getRawAttributes() )
    {
      if( a.type.tsym.flatName().toString().contains( GENERATED ) )
      {
        return true;
      }
    }
    return false;
  }
}