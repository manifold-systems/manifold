/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.tuple;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import manifold.api.fs.IFile;
import manifold.api.gen.*;
import manifold.api.host.IModule;
import manifold.api.host.RefreshKind;
import manifold.api.service.BaseService;
import manifold.api.type.ClassType;
import manifold.api.type.ContributorKind;
import manifold.api.type.ISourceKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.javac.ITupleTypeProvider;
import manifold.internal.javac.JavacPlugin;
import manifold.rt.api.util.ManClassUtil;
import manifold.tuple.rt.internal.GeneratedTuple;
import manifold.util.ReflectUtil;

import static manifold.tuple.TupleTypeProvider.BASE_NAME;

/**
 * Tuples
 */
public class TupleTypeManifold extends BaseService implements ITypeManifold
{
  private IModule _module;
  private final Map<String, Set<File>> _fqnToEnclosingSourceFile;

  public TupleTypeManifold()
  {
    _fqnToEnclosingSourceFile = new ConcurrentHashMap<>();
  }

  @Override
  public void init( IModule module )
  {
    _module = module;
  }

  @Override
  public IModule getModule()
  {
    return _module;
  }

   @Override
  public ISourceKind getSourceKind()
  {
    return ISourceKind.Java;
  }

  @Override
  public ContributorKind getContributorKind()
  {
    return ContributorKind.Primary;
  }

  @Override
  public boolean isTopLevelType( String fqn )
  {
    return isType( fqn );
  }

  @Override
  public ClassType getClassType( String fqn )
  {
    return ClassType.JavaClass;
  }

  @Override
  public List<IFile> findFilesForType( String fqn )
  {
    return Collections.emptyList();
//## todo: this doesn't really work, if there are more than one classes defining/referencing a tuple and one is deleted, the tuple .class file is deleted and not rebuilt for an incremental build.
//   Maybe we can force any remaining enclosing classes to rebuild by always adding them to the list of resource files that need compiling??
//    Set<File> enclosingFiles = _fqnToEnclosingSourceFile.get( fqn );
//    return enclosingFiles != null
//      ? enclosingFiles.stream()
//        .map( f -> JavacPlugin.instance().getHost().getFileSystem().getIFile( f ) )
//        .collect( Collectors.toList() )
//      : Collections.emptyList();
  }

  @SuppressWarnings( "unused" )
  public void addEnclosingSourceFile( String fqn, URI sourceFile )
  {
    Set<File> files = _fqnToEnclosingSourceFile.computeIfAbsent( fqn, k -> new HashSet<>() );
    files.add( new File( sourceFile ) );
  }

  @Override
  public void clear()
  {
  }

  @Override
  public boolean isType( String fqn )
  {
    return fqn.contains( '.' + BASE_NAME );
  }

  @Override
  public boolean isPackage( String pkg )
  {
    return !getTypeNames( pkg ).isEmpty();
  }

  @Override
  public String getPackage( String fqn )
  {
    return isType( fqn ) ? ManClassUtil.getPackage( fqn ) : null;
  }

  @Override
  public String contribute( JavaFileManager.Location location, String fqn, boolean genStubs, String existing, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new SrcClass( fqn, AbstractSrcClass.Kind.Class )
      .imports( List.class, ArrayList.class )
      .modifiers( Modifier.PUBLIC )  // non-final to support structural interface casts (until structural assignability is impled)
      .superClass( GeneratedTuple.class )
      .addField( new SrcField( "_orderedLabels",new  SrcType( List.class ).addTypeParam( String.class ) )
        .modifiers( Modifier.PRIVATE ) )
      .addMethod( new SrcMethod()
        .modifiers( Modifier.PUBLIC )
        .addAnnotation( new SrcAnnotationExpression( Override.class ) )
        .name( "orderedLabels" )
        .returns( new SrcType( List.class ).addTypeParam( String.class ) )
        .body( "return _orderedLabels;" ) );
    SrcConstructor srcConstructor = new SrcConstructor( srcClass )
      .modifiers( Modifier.PUBLIC );
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader newLoader = getClass().getClassLoader();
    if( newLoader != null )
    {
      ReflectUtil.setContextClassLoader( newLoader );
    }
    Map<String, String> fieldsMap;
    try
    {
      fieldsMap = ITupleTypeProvider.INSTANCE.get().getFields( fqn );
    }
    finally
    {
      ReflectUtil.setContextClassLoader( prevLoader );
    }
    if( fieldsMap == null )
    {
      throw new IllegalStateException( "Missing field mapping for tuple: " + fqn );
    }
    SrcStatementBlock body = new SrcStatementBlock()
      .addStatement( "List<String> orderedLabels = new ArrayList<>();" );
    for( Map.Entry<String, String> entry: fieldsMap.entrySet() )
    {
      String name = entry.getKey();
      String type = entry.getValue();
      SrcField field = new SrcField( name, type )
        .modifiers( Modifier.PUBLIC );
      srcClass.addField( field );
      srcConstructor.addParam( new SrcParameter( name, type ).modifiers( Modifier.FINAL ) );
      body
        .addStatement( "this." + name + " = " + name + ";" )
        .addStatement( "orderedLabels.add( \"" + name + "\" );" );
    }
    body.addStatement( "_orderedLabels = orderedLabels;" );
    srcConstructor.body( body );
    srcClass.addConstructor( srcConstructor );

    //todo: generate equals, hashcode, toString instead of the reflection based stuff in the base class
    return srcClass.render().toString();
  }

  @Override
  public Collection<String> getAllTypeNames()
  {
    return Collections.emptyList();
  }

  @Override
  public Collection<TypeName> getTypeNames( String namespace )
  {
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader newLoader = getClass().getClassLoader();
    if( newLoader != null )
    {
      ReflectUtil.setContextClassLoader( newLoader );
    }
    try
    {
      return ITupleTypeProvider.INSTANCE.get().getTypes().stream()
        .filter( fqn -> ManClassUtil.getPackage( fqn ).equals( namespace ) )
        .map( fqn -> new TypeName( fqn, _module, TypeName.Kind.TYPE, TypeName.Visibility.PUBLIC ) )
        .collect( Collectors.toSet() );
    }
    finally
    {
      ReflectUtil.setContextClassLoader( prevLoader );
    }
  }

  //
  // IFileConnected (not file connected...)
  //

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return false;
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    return false;
  }

  @Override
  public String[] getTypesForFile( IFile file )
  {
    return new String[0];
  }

  @Override
  public RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind )
  {
    return null;
  }

}