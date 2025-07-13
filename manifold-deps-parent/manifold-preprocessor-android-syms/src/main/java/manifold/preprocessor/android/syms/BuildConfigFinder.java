package manifold.preprocessor.android.syms;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import manifold.internal.javac.JavacPlugin;
import manifold.rt.api.util.StreamUtil;
import manifold.util.JreUtil;
import manifold.util.ManExceptionUtil;
import manifold.util.ReflectUtil;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

class BuildConfigFinder
{
  private static BuildConfigFinder INSTANCE = null;

  static BuildConfigFinder instance()
  {
    return INSTANCE == null ? INSTANCE = new BuildConfigFinder() : INSTANCE;
  }

  Map<String, String> loadBuildConfigSymbols()
  {
    if( JavacPlugin.instance() == null )
    {
      // probably in an IDE, which will have an IDE-specific SymbolProvider service impl for android build variants
      return Collections.emptyMap();
    }

    // load from BuildConfig.java file (if it is in the compiler's input list)
    Map<String, String> symbols = loadBuildConfigSymbols_Source();
    if( symbols != null && !symbols.isEmpty() )
    {
      return symbols;
    }

    // load from BuildConfig.class file
    return BuildConfigFinder.instance().loadBuildConfigSymbols_Class();
  }

  private Map<String, String> loadBuildConfigSymbols_Source()
  {
    Collection<JavaFileObject> fileObjects;
    if( JreUtil.isJava8() )
    {
      try
      {
        //noinspection unchecked
        fileObjects = (Collection<JavaFileObject>)ReflectUtil.field( JavacPlugin.instance().getJavacTask(), "fileObjects" ).get();
      }
      catch( Throwable t )
      {
        // can happen if getJavacTask() return something that is not JavaTaskImpl, or more generally, doesn't define fileObjects
        return Collections.emptyMap();
      }
    }
    else
    {
      Object arguments = ReflectUtil.method( "com.sun.tools.javac.main.Arguments", "instance", Context.class ).invokeStatic( JavacPlugin.instance().getContext() );
      //noinspection unchecked
      fileObjects = (Collection<JavaFileObject>)ReflectUtil.method( arguments, "getFileObjects" ).invoke();
    }

    for( JavaFileObject srcFile : fileObjects )
    {
      File file = new File( srcFile.getName() );
      if( file.getName().equals( "BuildConfig.java") )
      {
        return extractBuildConfigSymbols( file );
      }
    }
    return null;
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
              // hack to handle DEBUG init, which can be like: Boolean.parseBoolean("true")
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

  private Map<String, String> loadBuildConfigSymbols_Class()
  {
    Class<?> buildConfigClass = loadBuildConfigClass();
    if( buildConfigClass == null )
    {
      return Collections.emptyMap();
    }

    Map<String, String> symbols = new HashMap<>();
    for( Field field : buildConfigClass.getDeclaredFields() )
    {
      int mods = field.getModifiers();
      if( Modifier.isPublic( mods ) && Modifier.isStatic( mods ) && Modifier.isFinal( mods ) )
      {
        try
        {
          Object value = field.get( null );
          if( value != null )
          {
            symbols.put( field.getName(), String.valueOf( value ) );
          }
        }
        catch( IllegalAccessException e )
        {
          // Shouldn’t happen since field is public, but catch just in case
          throw new RuntimeException( "Failed to access field: " + field.getName(), e );
        }
      }
    }

    return symbols;
  }

  private Class<?> loadBuildConfigClass()
  {
    Path classesRoot = ((StandardJavaFileManager)JavacPlugin.instance().getManifoldFileManager())
      .getLocation( CLASS_OUTPUT ).iterator().next().toPath();

    try
    {
      Path classFile = Files.walk( classesRoot )
        .filter( p -> p.getFileName().toString().equals( "BuildConfig.class" ) )
        .findFirst()
        .orElse( null );

      if( classFile == null )
      {
        // should never happen, but don't want to tank the build for this
        return null;
      }

      return new MyClassLoader().load( classesRoot, classFile );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static class MyClassLoader extends ClassLoader
  {
    MyClassLoader()
    {
      super( BuildConfigFinder.class.getClassLoader() );
    }

    Class<?> load( Path classesRoot, Path classFile ) throws IOException
    {
      String fqn = classesRoot.relativize( classFile )
        .toString()
        .replace( File.separatorChar, '.' )
        .replaceAll( "\\.class$", "" );

      byte[] bytes = Files.readAllBytes( classFile );
      return defineClass( fqn, bytes, 0, bytes.length );
    }
  }
}