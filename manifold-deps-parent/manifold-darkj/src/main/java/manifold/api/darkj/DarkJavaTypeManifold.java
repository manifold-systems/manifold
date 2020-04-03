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

package manifold.api.darkj;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.JCTree;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;
import manifold.api.host.IRuntimeManifoldHost;
import manifold.api.type.JavaTypeManifold;
import manifold.api.util.StreamUtil;


import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Dark Java is dark because it's Java, yet it can't be "seen" at compile-time.
 * It satisfies the use-case where you need to write Java software against
 * a system that materializes only at runtime.  For instance, say your code
 * must target different versions of the same API. You can use Dark Java
 * to write an alternative version of the parts of your software that support
 * an older (or newer) API.
 */
public class DarkJavaTypeManifold extends JavaTypeManifold<Model>
{
  @SuppressWarnings("WeakerAccess")
  public static final Set<String> FILE_EXTENSIONS = Collections.singleton( "darkj" );

  @Override
  public boolean accept( IModule module )
  {
    // Dark Java files are intended to be compiled dynamically at runtime, they
    // should never be compiled statically, otherwise just use normal Java.
    return module.getHost() instanceof IRuntimeManifoldHost;
  }

  @Override
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new Model( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    if( isAnonymous( relativeInner ) )
    {
      return true;
    }

    Model model = getModel( topLevel );
    if( model == null )
    {
      return false;
    }

    JCTree.JCClassDecl classDecl = getClassDecl( model );
    if( classDecl == null )
    {
      return false;
    }

    for( JCTree m: classDecl.getMembers() )
    {
      if( m instanceof JCTree.JCClassDecl )
      {
        return isInnerClass( (JCTree.JCClassDecl)m, relativeInner );
      }
    }

    return false;
  }

  private JCTree.JCClassDecl getClassDecl( Model model )
  {
    JCTree.JCClassDecl classDecl = model.getClassDecl();
    if( classDecl != null )
    {
      return classDecl;
    }

    List<CompilationUnitTree> trees = new ArrayList<>();
    getModule().getHost().getJavaParser().parseText( getSource( model ), trees, null, null, null );
    if( trees.isEmpty() )
    {
      return null;
    }
    classDecl = (JCTree.JCClassDecl)trees.get( 0 ).getTypeDecls().get( 0 );
    model.setClassDecl( classDecl );
    return classDecl;
  }

  private boolean isAnonymous( String relativeInner )
  {
    String first = relativeInner;
    int iDot = relativeInner.indexOf( '.' );
    if( iDot > 0 )
    {
      first = relativeInner.substring( 0, iDot );
    }
    try
    {
      int result = Integer.parseInt( first );
      return result >= 0;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isInnerClass( JCTree.JCClassDecl cls, String relativeInner )
  {
    String name;
    String remainder;
    int iDot = relativeInner.indexOf( '.' );
    if( iDot > 0 )
    {
      name = relativeInner.substring( 0, iDot );
      remainder = relativeInner.substring( iDot+1 );
    }
    else
    {
      name = relativeInner;
      remainder = null;
    }
    if( cls.getSimpleName().toString().equals( name ) )
    {
      if( remainder != null )
      {
        for( JCTree m: cls.getMembers() )
        {
          if( m instanceof JCTree.JCClassDecl )
          {
            if( isInnerClass( (JCTree.JCClassDecl)m, remainder ) )
            {
              return true;
            }
          }
        }
      }
      else
      {
        return true;
      }
    }
    return false;
  }

  private String getSource( Model model )
  {
    try
    {
      return StreamUtil.getContent( new InputStreamReader( model.getFile().openInputStream(), UTF_8 ) );
    }
    catch( IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return getSource( model );
  }
}