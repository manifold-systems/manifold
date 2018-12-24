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

package manifold.ext;

import com.sun.tools.javac.tree.TreeTranslator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.host.RefreshRequest;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.api.type.JavaTypeManifold;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.ext.api.Extension;
import manifold.internal.javac.IssueReporter;
import manifold.internal.javac.TypeProcessor;
import manifold.util.StreamUtil;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class ExtensionManifold extends JavaTypeManifold<Model> implements ITypeProcessor
{
  @SuppressWarnings("WeakerAccess")
  public static final String EXTENSIONS_PACKAGE = "extensions";
  private static final Set<String> FILE_EXTENSIONS = new HashSet<>( Arrays.asList( "java", "class" ) );

  public void init( IModule module )
  {
    init( module, ( fqn, files ) -> new Model( fqn, files, this ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public ContributorKind getContributorKind()
  {
    return ContributorKind.Supplemental;
  }

  @Override
  protected CacheClearer createCacheClearer()
  {
    return new ExtensionCacheHandler();
  }

  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    if( fqn.length() > EXTENSIONS_PACKAGE.length() + 2 )
    {
      int iExt = fqn.indexOf( EXTENSIONS_PACKAGE + '.' );

      if( iExt >= 0 )
      {
        String extendedType = fqn.substring( iExt + EXTENSIONS_PACKAGE.length() + 1 );

        int iDot = extendedType.lastIndexOf( '.' );
        if( iDot > 0 )
        {
          return extendedType.substring( 0, iDot );
        }
      }
    }
    return null;
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    Set<String> fqns = getModule().getPathCache().getFqnForFile( file );
    if( fqns == null )
    {
      return false;
    }

    for( String fqn : fqns )
    {
      if( fqn.length() > EXTENSIONS_PACKAGE.length() + 2 )
      {
        int iExt = fqn.indexOf( EXTENSIONS_PACKAGE + '.' );
        if( iExt >= 0 )
        {
          String extendedType = fqn.substring( iExt + EXTENSIONS_PACKAGE.length() + 1 );

          int iDot = extendedType.lastIndexOf( '.' );
          if( iDot > 0 )
          {
            try
            {
              //## note: this is pretty sloppy science here, but we don't want to parse java or use asm at this point

              if( file.getExtension().equalsIgnoreCase( "java" ) )
              {
                String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
                return content.contains( "@Extension" ) && content.contains( Extension.class.getPackage().getName() );
              }
              else // .class file
              {
                String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
                return content.contains( Extension.class.getName().replace( '.', '/' ) );
              }
            }
            catch( IOException e )
            {
              // eat
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    // Include types extended by dynamically provided extension classes from IExtensionClassProducers

    Map<String, LocklessLazyVar<Model>> map = new HashMap<>();
    for( ITypeManifold tm : getModule().getTypeManifolds() )
    {
      if( tm instanceof IExtensionClassProducer )
      {
        for( String extended : ((IExtensionClassProducer)tm).getExtendedTypes() )
        {
          map.put( extended, LocklessLazyVar.make( () -> new Model( extended, Collections.emptySet(), this ) ) );
        }
      }
    }
    return map;
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return isType( topLevel ) &&
           (isInnerToPrimaryManifold( topLevel, relativeInner ) ||
            isInnerToJavaClass( topLevel, relativeInner ));
  }

  private boolean isInnerToPrimaryManifold( String topLevel, String relativeInner )
  {
    Set<ITypeManifold> tms = getModule().findTypeManifoldsFor( topLevel );
    if( tms != null )
    {
      for( ITypeManifold tm : tms )
      {
        if( tm.getContributorKind() == ContributorKind.Primary && tm instanceof ResourceFileTypeManifold )
        {
          return ((ResourceFileTypeManifold)tm).isInnerType( topLevel, relativeInner );
        }
      }
    }
    return false;
  }

  //## todo: This applies only to precompiled Java class files.
  //## todo: Need to move this method to IManifoldHost for different use-cases (class files, javac symbols, and IJ psi)
  private boolean isInnerToJavaClass( String topLevel, String relativeInner )
  {
    try
    {
      Class<?> cls = Class.forName( topLevel, false, getModule().getHost().getActualClassLoader() );
      for( Class<?> inner : cls.getDeclaredClasses() )
      {
        if( isInnerClass( inner, relativeInner ) )
        {
          return true;
        }
      }
    }
    catch( ClassNotFoundException ignore )
    {
    }
    return false;
  }

  private boolean isInnerClass( Class<?> cls, String relativeInner )
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
    if( cls.getSimpleName().equals( name ) )
    {
      if( remainder != null )
      {
        for( Class<?> m: cls.getDeclaredClasses() )
        {
          if( isInnerClass( m, remainder ) )
          {
            return true;
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

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    return new ExtCodeGen( location, model, topLevelFqn, existing ).make( errorHandler );
  }

  @Override
  public void process( TypeElement typeElement, TypeProcessor typeProcessor, IssueReporter<JavaFileObject> issueReporter )
  {
    if( typeElement.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE )
    {
      TreeTranslator visitor = new ExtensionTransformer( this, typeProcessor );
      typeProcessor.getTree().accept( visitor );
    }
  }

  private class ExtensionCacheHandler extends CacheClearer
  {
    @Override
    public void refreshedTypes( RefreshRequest request )
    {
      super.refreshedTypes( request );
      if( request.file == null )
      {
        return;
      }

      for( ITypeManifold tm: ExtensionManifold.this.getModule().findTypeManifoldsFor( request.file ) )
      {
        if( tm instanceof IExtensionClassProducer )
        {
          for( String extended: ((IExtensionClassProducer)tm).getExtendedTypesForFile( request.file ) )
          {
            refreshedType( extended, request );
          }
        }
      }
    }

    private void refreshedType( String extended, RefreshRequest request )
    {
      switch( request.kind )
      {
        case CREATION:
          createdType( Collections.emptySet(), extended );
          break;
        case MODIFICATION:
          modifiedType( Collections.emptySet(), extended );
          break;
        case DELETION:
          deletedType( Collections.emptySet(), extended );
          break;
      }
    }
  }

//  @Override
//  public boolean filterError( TypeProcessor typeProcessor, Diagnostic diagnostic )
//  {
//    if( diagnostic.getKind() == Diagnostic.Kind.ERROR )
//    {
//      Object[] args = ((JCDiagnostic)diagnostic).getArgs();
//      if( args != null )
//      {
//        for( Object arg: args )
//        {
//          if( arg instanceof JCDiagnostic )
//          {
//            JCDiagnostic jcArg = (JCDiagnostic)arg;
//            if( jcArg.getCode().equals( "compiler.misc.inconvertible.types" ) )
//            {
//              Object[] argArgs = jcArg.getArgs();
//              if( argArgs != null && argArgs.length == 2 )
//              {
//                Type.ClassType type = (Type.ClassType)argArgs[1];
//                if( type.tsym.hasAnnotations() )
//                {
//                  for( Attribute.Compound anno: type.tsym.getAnnotationMirrors() )
//                  {
//                    if( ((Type.ClassType)anno.getAnnotationType()).tsym.getQualifiedName().toString().equals( Structural.class.getName() ) )
//                    {
//                      //((JCDiagnostic)diagnostic).getDiagnosticPosition().getTree().type = type;
//                      return true;
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//    return false;
//  }
}