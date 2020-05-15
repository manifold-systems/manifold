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

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Assert;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IModule;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.internal.host.JavacManifoldHost;
import manifold.util.ReflectUtil;

import java.io.File;
import java.util.*;

import static manifold.api.type.ContributorKind.Supplemental;

class StaticCompiler
{
  private static final StaticCompiler INSTANCE = new StaticCompiler();
  private boolean _enterGuard;

  private StaticCompiler()
  {
  }

  static StaticCompiler instance()
  {
    return INSTANCE;
  }

  void compileRemainingTypes_ByFile()
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
          // place resource types in JavaCompiler's todo list
          ClassSymbols.instance( module ).getClassSymbol( JavacPlugin.instance().getJavacTask(), fqn );
        }
      }
    }

    JavaCompiler javaCompiler = JavaCompiler.instance( JavacPlugin.instance().getContext() );
    if( !javaCompiler.todo.isEmpty() )
    {
      // compile resource types we just loaded
      compileTodo( javaCompiler );
    }

    _enterGuard = false;
  }

  void compileRemainingTypes_ByTypeNameRegexes()
  {
    if( _enterGuard )
    {
      return;
    }
    _enterGuard = true;

    Map<String, String> others = JavacPlugin.instance().getOtherSourceMappings();
    Map<ITypeManifold, Set<String>> classToRegex = new HashMap<>();
    for( Map.Entry<String, String> entry: others.entrySet() )
    {
      mapTypeManifoldToTypeNameRegexes( classToRegex, entry.getKey(), entry.getValue() );
    }

    IModule module = JavacPlugin.instance().getHost().getSingleModule();
    for( Map.Entry<ITypeManifold, Set<String>> mapping: classToRegex.entrySet() )
    {
      ITypeManifold tm = mapping.getKey();
      Collection<String> types = computeNamesToPrecompile( tm.getAllTypeNames(), mapping.getValue() );
      if( types.isEmpty() )
      {
        //todo: add compile error
        continue;
      }

      // signal the type manifold for post Java compilation
      tm.enterPostJavaCompilation();

      // Cause the types to compile
      for( String fqn: types )
      {
        // place gosu class in JavaCompiler's `todo` list
        ClassSymbols.instance( module ).getClassSymbol( JavacPlugin.instance().getJavacTask(), fqn );
      }
    }

    JavaCompiler javaCompiler = JavaCompiler.instance( JavacPlugin.instance().getContext() );
    if( !javaCompiler.todo.isEmpty() )
    {
      // compile resource types we just loaded
      compileTodo( javaCompiler );
    }

    _enterGuard = false;
  }

  private void compileTodo( JavaCompiler javac )
  {
    Todo todo = javac.todo;
    String compilePolicy = ((Enum<?>) ReflectUtil.field( javac, "compilePolicy" ).get()).name();
    switch( compilePolicy )
    {
      case "ATTR_ONLY":
        javac.attribute( todo );
        break;

      case "CHECK_ONLY":
        javac.flow( javac.attribute( todo ) );
        break;

      case "SIMPLE":
        javac.generate( javac.desugar( javac.flow( javac.attribute( todo ) ) ) );
        break;

      case "BY_FILE":
      {
        Queue<Queue<Env<AttrContext>>> q = todo.groupByFile();
        while( !q.isEmpty() &&
          !(boolean)ReflectUtil.method( javac, "shouldStop", CompileStates.CompileState.class ).invoke( CompileStates.CompileState.ATTR ) )
        {
          javac.generate( javac.desugar( javac.flow( javac.attribute( q.remove() ) ) ) );
        }
      }
      break;

      case "BY_TODO":
        while( !todo.isEmpty() )
          javac.generate( javac.desugar( javac.flow( javac.attribute( todo.remove() ) ) ) );
        break;

      default:
        Assert.error( "unknown compile policy" );
    }
  }

  private Collection<String> computeNamesToPrecompile( Collection<String> allTypeNames, Set<String> regexes )
  {
    Set<String> matchingTypes = new HashSet<>();
    for( String fqn: allTypeNames )
    {
      if( regexes.stream().anyMatch( fqn::matches ) )
      {
        matchingTypes.add( fqn );
      }
    }
    return matchingTypes;
  }

  public void mapTypeManifoldToTypeNameRegexes( Map<ITypeManifold, Set<String>> typeNames, String fqnOrExt, String regex )
  {
    int iClass = fqnOrExt.indexOf( "class:" );
    Set<ITypeManifold> typeManifolds = JavacPlugin.instance().getHost().getSingleModule().getTypeManifolds();
    if( iClass > 0 )
    {
      String typeManifoldClassName = fqnOrExt.substring( "class:".length() );
      ITypeManifold typeManifold = typeManifolds.stream().filter( tm -> tm.getClass().getTypeName().equals( typeManifoldClassName ) )
        .findFirst()
        .orElseThrow( () -> new RuntimeException( "Expecting type manifold class: " + typeManifoldClassName ) );
      Set<String> regexes = typeNames.computeIfAbsent( typeManifold, tm -> new HashSet<>() );
      regexes.add( regex );
    }
    else
    {
      //noinspection UnnecessaryLocalVariable
      String ext = fqnOrExt;
      boolean all = "*".equals( ext );
      typeManifolds.stream()
        .filter( tm -> tm.getContributorKind() != ContributorKind.Supplemental )
        .forEach( tm -> {
          if( all || tm.handlesFileExtension( ext ) )
          {
            Set<String> regexes = typeNames.computeIfAbsent( tm, e -> new HashSet<>() );
            regexes.add( regex );
          }
        } );
    }
  }
}
