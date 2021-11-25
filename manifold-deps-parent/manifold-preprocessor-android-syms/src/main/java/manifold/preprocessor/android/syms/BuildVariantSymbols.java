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

package manifold.preprocessor.android.syms;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import manifold.api.fs.IFile;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.api.SymbolProvider;
import manifold.preprocessor.definitions.Definitions;
import manifold.rt.api.util.StreamUtil;
import manifold.util.JreUtil;
import manifold.util.ManExceptionUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

public class BuildVariantSymbols implements SymbolProvider
{
  private final LocklessLazyVar<Map<String, String>> _buildConfigSyms =
    LocklessLazyVar.make( () -> loadBuildConfigSymbols() );

  @Override
  public boolean isDefined( Definitions rootDefinitions, IFile sourceFile, String def )
  {
    return _buildConfigSyms.get().containsKey( def );
  }

  @Override
  public String getValue( Definitions rootDefinitions, IFile sourceFile, String def )
  {
    return _buildConfigSyms.get().get( def );
  }

  private Map<String, String> loadBuildConfigSymbols()
  {
    String generatedClassesDir = getBuildConfigSourcePath();
    if( generatedClassesDir == null )
    {
      return Collections.emptyMap();
    }

    File dir = new File( generatedClassesDir );
    File buildConfig = findBuildConfig( dir );
    if( buildConfig != null )
    {
      return extractBuildConfigSymbols( buildConfig );
    }
    return Collections.emptyMap();
  }

  private Map<String, String> extractBuildConfigSymbols( File buildConfig )
  {
    Map<String, String> map = new HashMap<>();
    try
    {
      FileReader fileReader = new FileReader( buildConfig );
      ArrayList<CompilationUnitTree> trees = new ArrayList<>();
      JavacPlugin.instance().getHost().getJavaParser()
        .parseText( StreamUtil.getContent( fileReader ), trees, null, null, null );
      if( !trees.isEmpty() )
      {
        CompilationUnitTree tree = trees.get( 0 );
        List<? extends Tree> typeDecls = tree.getTypeDecls();
        if( typeDecls != null && !typeDecls.isEmpty() )
        {
          Tree cls = typeDecls.get( 0 );
          if( cls instanceof JCTree.JCClassDecl )
          {
            com.sun.tools.javac.util.List<JCTree> defs = ((JCTree.JCClassDecl)cls).defs;
            if( !defs.isEmpty() )
            {
              for( JCTree def: defs )
              {
                if( def instanceof JCTree.JCVariableDecl )
                {
                  processConstant( map, (JCTree.JCVariableDecl)def );
                }
              }
            }
          }
        }
      }
    }
    catch( IOException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
    return map;
  }

  private void processConstant( Map<String, String> map, JCTree.JCVariableDecl def )
  {
    JCTree.JCModifiers modifiers = def.getModifiers();
    long mods = modifiers == null ? 0 : modifiers.flags;
    int psf = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
    if( (mods & psf) == psf )
    {
      JCTree.JCExpression initializer = def.getInitializer();
      if( initializer != null )
      {
        String value = null;
        String init = initializer.toString();
        if( init.startsWith( "\"" ) )
        {
          value = init.substring( 1, init.length()-1 );
        }
        else
        {
          try
          {
            long l = Long.parseLong( init );
            value = init;
          }
          catch( Exception e )
          {
            try
            {
              double d = Double.parseDouble( init );
              value = init;
            }
            catch( Exception e2 )
            {
              // hack to handle DEBUG init, which can be like: Boolean.parseBooean("true")
              if( init.contains( "true" ) )
              {
                // preprocessor definition will be just defined, a "false" value will not be defined
                value = "";
              }
            }
          }
        }
        if( value != null )
        {
          map.put( def.getName().toString(), value );
        }
      }
    }
  }

  private String getBuildConfigSourcePath()
  {
    Set<String> sourcePath = JavacPlugin.instance().deriveJavaSourcePath();
    String generatedClassesDir = null;
    for( String path: sourcePath )
    {
      int index = path.lastIndexOf( "/app/src/".replace( '/', File.separatorChar ) );
      if( index > 0 )
      {
        generatedClassesDir = path.substring( 0, index ) + "/app/build/generated/source/buildConfig".replace( '/', File.separatorChar );
        String variantPart = getVariantPart();
        if( variantPart != null )
        {
          generatedClassesDir += File.separatorChar + variantPart;
        }
        break;
      }
    }
    return generatedClassesDir;
  }

  private String getVariantPart()
  {
    try
    {
      String variantPart = null;
      if( JreUtil.isJava8() )
      {
        String[] args = (String[])ReflectUtil.field( JavacPlugin.instance().getJavacTask(), "args" ).get();
        boolean found = false;
        for( String arg : args )
        {
          // derive build variant from generated source output path, which has the variant directory
          if( arg != null && arg.equalsIgnoreCase( "-s" ) )
          {
            found = true;
          }
          else if( found )
          {
            variantPart = arg;
            break;
          }
        }
      }
      else // Java 9+
      {
        // javacTask.args.options.get( "-s" )
        Object args = ReflectUtil.field( JavacPlugin.instance().getJavacTask(), "args" ).get();
        Object options = ReflectUtil.field( args, "options" ).get();
        variantPart = (String)ReflectUtil.method( options, "get", String.class ).invoke( "-s" );
      }

      if( variantPart == null )
      {
        return null;
      }

      String marker = File.separatorChar + "ap_generated_sources" + File.separatorChar;
      int index = variantPart.lastIndexOf( marker );
      if( index > 0 )
      {
        // C:\Users\scott\AndroidStudioProjects\MyBasicActivityApplication\app\build\generated\ap_generated_sources\release\out
        variantPart = variantPart.substring( index + marker.length() );
        int outIndex = variantPart.lastIndexOf( File.separatorChar );
        variantPart = variantPart.substring( 0, outIndex );
        return variantPart;
      }
      return null;
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private File findBuildConfig( File file )
  {
    if( file.isFile() )
    {
      if( file.getName().equals( "BuildConfig.java" ) )
      {
        return file;
      }
      return null;
    }
    else
    {
      File[] listing = file.listFiles();
      if( listing != null )
      {
        for( File f : listing )
        {
          File buildConfig = findBuildConfig( f );
          if( buildConfig != null )
          {
            return buildConfig;
          }
        }
      }
    }
    return null;
  }
}
