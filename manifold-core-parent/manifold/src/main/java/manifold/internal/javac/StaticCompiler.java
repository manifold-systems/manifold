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

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IModule;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.internal.host.JavacManifoldHost;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import java.io.File;
import java.util.*;

import static manifold.api.type.ContributorKind.Supplemental;

/**
 * This class compiles resource types optionally specified with the command line arguments: <br>
 * <code>-Aother.source.files=[file-list]</code><br>
 * or<br>
 * <code>-Amanifold.source.[ext]=[type-name-regex]</code><br><br>
 * Note, the "other.source.files" argument may not be used with the "manifold.source.*" argument.<br>
 * <p/>
 * The specified resource types are compiled after javac finishes compiling the .java source list. Note, if a resource
 * type is referenced by any of the <i>.java</i> files, the resource type sill compiled along with the .java files
 * during javac's normal round of compilation. Thus, this class compiles only the specified resource types that remain
 * uncompiled after <i>.java</i> source files.
 * <p/>
 * See <a href="https://github.com/manifold-systems/manifold/tree/master/manifold-core-parent/manifold#explicit-resource-compilation">Explicit Resource Compilation</a>
 */
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
    Context ctx = JavacPlugin.instance().getContext();
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

        // Cause the types to compile by entering ClassSymbols into javac's "todos"
        if( !enterClassSymbols( module, ctx, Arrays.asList( types ) ) )
        {
          return;
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
    Context ctx = JavacPlugin.instance().getContext();
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

      // Cause the types to compile by entering ClassSymbols into javac's "todos"
      if( !enterClassSymbols( module, ctx, types ) )
      {
        return;
      }
    }

    JavaCompiler javaCompiler = JavaCompiler.instance( ctx );
    if( !javaCompiler.todo.isEmpty() )
    {
      // compile resource types we just loaded
      compileTodo( javaCompiler );
    }

    _enterGuard = false;
  }

  private boolean enterClassSymbols( IModule module, Context ctx, Collection<String> types )
  {
    //
    // Push the module symbol (for Java 9+)
    //

    /*Symbol.ModuleSymbol*/ Object moduleSym = null;
    if( JreUtil.isJava9orLater() )
    {
      moduleSym = pushModuleSymbol( ctx );
      if( moduleSym == null )
      {
        return false;
      }
    }

    //
    // Make ClassSymbols (to enter the types into javac's todos, which cause the types to compile)
    //

    Object top = null;
    try
    {
      // Cause the types to compile
      for( String fqn : types )
      {
        // place gosu class in JavaCompiler's "todos" list
        ClassSymbols.instance( module ).getClassSymbol( JavacPlugin.instance().getJavacTask(), fqn );
      }
    }
    finally
    {
      //
      // Pop the ModuleSymbol
      //

      if( moduleSym != null )
      {
        top = ctx.get( ManifoldJavaFileManager.MODULE_CTX ).pop();
      }
    }
    if( top != moduleSym )
    {
      throw new IllegalStateException( "unbalanced stack" );
    }

    return true;
  }

  private Object pushModuleSymbol( Context ctx )
  {
    /*Modules*/ Object modules = ReflectUtil.method( "com.sun.tools.javac.comp.Modules", "instance", Context.class )
    .invokeStatic( ctx );
    Set<?>/*<Symbol.ModuleSymbol>*/ rootModules = (Set<?>)ReflectUtil.method( modules, "getRootModules" ).invoke();
    /*Symbol.ModuleSymbol*/ Object moduleSym = null;
    if( rootModules.size() == 1 )
    {
      moduleSym = rootModules.iterator().next();
    }
    else
    {
      if( rootModules.size() > 1 )
      {
        // todo: compile warning/error (are multiple roots possible in a single javac invocation?)
      }

      moduleSym = ReflectUtil.field( Symtab.instance( ctx ), "unnamedModule" ).get();
    }
    ctx.get( ManifoldJavaFileManager.MODULE_CTX ).push( moduleSym );

    return moduleSym;
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
