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

import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.util.List;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IModule;
import manifold.api.type.ITypeManifold;
import manifold.internal.host.JavacManifoldHost;
import manifold.util.ReflectUtil;


import static manifold.api.type.ContributorKind.Supplemental;

public class ManCheck extends Check
{
  private static final String CHECK_FIELD = "chk";
  private boolean _enterGuard;

  public static Check instance( Context ctx )
  {
    Check check = ctx.get( checkKey );
    if( !(check instanceof ManCheck) )
    {
      ctx.put( checkKey, (Check)null );
      check = new ManCheck( ctx );
    }

    return check;
  }

  private ManCheck( Context ctx )
  {
    super( ctx );

    ReflectUtil.field( JavaCompiler.instance( ctx ), CHECK_FIELD ).set( this );
  }

  @Override
  public void reportDeferredDiagnostics()
  {
    compileRemainingTypes();
    super.reportDeferredDiagnostics();
  }

  private void compileRemainingTypes()
  {
    if( _enterGuard )
    {
      return;
    }
    _enterGuard = true;

    List<String> others = JavacPlugin.instance().getOtherInputFiles();
    JavacManifoldHost host = JavacPlugin.instance().getHost();
    IFileSystem fs = host.getFileSystem();
    for( String path: others )
    {
      IFile file = fs.getIFile( new File( path ) );
      if( file.exists() )
      {
        IModule module = host.getSingleModule();
        Set<ITypeManifold> tms = module.findTypeManifoldsFor( file,
          tm -> tm.getContributorKind() != Supplemental );
        if( tms.isEmpty() )
        {
          //todo: add compiler error
          continue;
        }
        ITypeManifold tm = tms.iterator().next();
        String[] types = tm.getTypesForFile( file );
        if( types == null || types.length == 0 )
        {
          //todo: add compile error
          continue;
        }

        tm.enterPostJavaCompilation();

        // Cause the types to compile
        for( String fqn: types )
        {
          // place gosu class in JavaCompiler's todo list
          ClassSymbols.instance( module ).getClassSymbol( JavacPlugin.instance().getJavacTask(), fqn );
        }
      }
    }

    JavaCompiler javaCompiler = JavaCompiler.instance( JavacPlugin.instance().getContext() );
    if( !javaCompiler.todo.isEmpty() )
    {
      // compile gosu classes we just loaded
      ReflectUtil.method( javaCompiler, "compile2" ).invoke();
    }

    _enterGuard = false;
  }
}
