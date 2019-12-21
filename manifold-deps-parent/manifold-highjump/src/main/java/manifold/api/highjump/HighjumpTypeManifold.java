/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.highjump;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.AbstractSrcClass;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.host.IModule;
import manifold.api.host.RefreshKind;
import manifold.api.service.BaseService;
import manifold.api.type.ClassType;
import manifold.api.type.ContributorKind;
import manifold.api.type.ISourceKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.api.util.fingerprint.Fingerprint;


public class HighjumpTypeManifold extends BaseService implements ITypeManifold
{
  private static final String PKG = "manifold.highjump.pkg";
  private static final String PREFIX = "HjClass_";
  static final String FQN_PREFIX = PKG + '.' + PREFIX;
  private static final String SYMBOL = "_Symbol_";
  private static final String FQN_SYMBOL_PREFIX = FQN_PREFIX + SYMBOL;

  private IModule _module;

  private final Map<ClassLoader, Map<Fingerprint, ExpressionClass>> _fpToExprClass;

  private final Map<String, ExpressionClass> _fqnToExprClass;
  private final Map<String, Symbol> _fqnToSymbol;

  public HighjumpTypeManifold()
  {
    _fpToExprClass = new ConcurrentHashMap<>();
    _fqnToExprClass = new ConcurrentHashMap<>();
    _fqnToSymbol = new ConcurrentHashMap<>();
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
  }

  @Override
  public void clear()
  {

  }

  //
  // todo
  //

  public Object evaluate( Options options )
  {
    Map<Fingerprint, ExpressionClass> loaderToExprCache = _fpToExprClass.computeIfAbsent(
      options.contextLoader == null ? getClass().getClassLoader() : options.contextLoader, key -> new ConcurrentHashMap<>() );
    ExpressionClass exprClass = loaderToExprCache.computeIfAbsent( new Fingerprint( options.expr ),
      key -> new ExpressionClass( options, _fqnToExprClass ) );
    return exprClass.evaluate();
  }

  @Override
  public boolean isType( String fqn )
  {
    return fqn.startsWith( FQN_PREFIX );
  }

  @Override
  public boolean isPackage( String pkg )
  {
    return pkg.equals( PKG );
  }

  @Override
  public String getPackage( String fqn )
  {
    return isType( fqn ) ? PKG : null;
  }

  @Override
  public String contribute( JavaFileManager.Location location, String fqn, String existing, DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( fqn.startsWith( FQN_SYMBOL_PREFIX ) )
    {
      return makeSymbolClass( fqn, errorHandler );
    }

    ExpressionClass exprClass = _fqnToExprClass.get( fqn );
    SrcClass srcClass = new SrcClass( fqn, AbstractSrcClass.Kind.Class );
    for( String imprt: exprClass.getOptions().imports )
    {
      srcClass.addImport( imprt );
    }
    for( String staticImport: exprClass.getOptions().staticImports )
    {
      srcClass.addStaticImport( staticImport );
    }

    // todo: make a unique class for each Symbol *instance* where the class has a static field with the Symbol's name.
    // Then generate code to statically import the field.
    for( Map.Entry<String, Symbol> entry: exprClass.getOptions().symbols.entrySet() )
    {
      Symbol symbol = entry.getValue();
      String symClassName = symbol.getUid() + '$' + symbol.getName();
      String symFqn = FQN_SYMBOL_PREFIX + symClassName;
      _fqnToSymbol.put( symFqn, symbol );
      srcClass.addStaticImport( symFqn + '.' + symbol.getName() );
    }
    
    srcClass.addMethod( new SrcMethod()
      .name( "evaluate" )
      .modifiers( Modifier.PUBLIC )
      .returns( Object.class )
      .body( "return " + exprClass.getOptions().expr + ';' ) );

    return srcClass.toString();
  }

  private String makeSymbolClass( String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    Symbol symbol = _fqnToSymbol.get( fqn );

    SrcClass srcClass = new SrcClass( fqn, AbstractSrcClass.Kind.Class );
    srcClass.modifiers( Modifier.PUBLIC | Modifier.FINAL );
    SrcField field = new SrcField( symbol.getName(), symbol.getType() )
      .modifiers( Modifier.PUBLIC | Modifier.STATIC );
    if( symbol.getInitialValue() != null )
    {
      //todo: add call to "Highjump.getInitialValue(\"" + symbol.getUid() + '$' + symbol.getName() + "\")" which will
      // get the initialValue from the _fqnToSymbol map.
    }
    srcClass.addField( field );

    return null;
  }


  @Override
  public Collection<String> getAllTypeNames()
  {
    return Collections.emptyList();
  }

  @Override
  public Collection<TypeName> getTypeNames( String namespace )
  {
    return Collections.emptyList();
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