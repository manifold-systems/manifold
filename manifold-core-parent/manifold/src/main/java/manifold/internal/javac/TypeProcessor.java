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

import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import manifold.api.host.IManifoldHost;
import manifold.api.type.ICompilerComponent;
import manifold.api.type.ICompilerComponent.InitOrder;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.ConcurrentHashSet;

import static manifold.api.type.ICompilerComponent.InitOrder.After;
import static manifold.api.type.ICompilerComponent.InitOrder.Before;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  private final Set<Object> _drivers;
  private LinkedHashSet<ICompilerComponent> _compilerComponents;

  TypeProcessor( IManifoldHost host, BasicJavacTask javacTask )
  {
    super( host, javacTask );
    _drivers = new ConcurrentHashSet<>();
    loadCompilerComponents( javacTask );
  }

  private void loadCompilerComponents( BasicJavacTask javacTask )
  {
    _compilerComponents = new LinkedHashSet<>();
    ServiceUtil.loadRegisteredServices( _compilerComponents, ICompilerComponent.class, getClass().getClassLoader() );
    _compilerComponents = new LinkedHashSet<>( order( new ArrayList<>( _compilerComponents ) ) );
    _compilerComponents.forEach( cc -> cc.init( javacTask, this ) );
  }

  /**
   * Allow the components to control the order of their init() call with respect to other components.
   */
  private List<ICompilerComponent> order( List<ICompilerComponent> compilerComponents )
  {
    if( compilerComponents.size() <= 0 )
    {
      return compilerComponents;
    }

    List<ICompilerComponent> copy = new ArrayList<>( compilerComponents );
    for( int i = 0; i < copy.size(); i++ )
    {
      ICompilerComponent c = copy.get( i );
      int oldIndex = compilerComponents.indexOf( c );
      compilerComponents.remove( oldIndex );
      int newIndex = -1;
      for( int j = compilerComponents.size() - 1; j >= 0; j-- )
      {
        ICompilerComponent cc = compilerComponents.get( j );
        InitOrder initOrder = c.initOrder( cc );
        if( initOrder == Before )
        {
          newIndex = j;
        }
      }
      for( int j = 0; j < compilerComponents.size(); j++ )
      {
        ICompilerComponent cc = compilerComponents.get( j );
        InitOrder initOrder = c.initOrder( cc );
        if( initOrder == After )
        {
          newIndex = j + 1;
        }
      }
      compilerComponents.add( newIndex >= 0 ? newIndex : oldIndex, c );
    }
    return compilerComponents;
  }

  public Collection<ICompilerComponent> getCompilerComponents()
  {
    return _compilerComponents;
  }

  @Override
  public void process( TypeElement element, IssueReporter<JavaFileObject> issueReporter )
  {
    if( IDynamicJdk.isInitializing() )
    {
      // avoid re-entry of dynamic jdk construction
      return;
    }

    for( ITypeManifold sp: getHost().getSingleModule().getTypeManifolds() )
    {
      if( sp instanceof ITypeProcessor )
      {
        //JavacProcessingEnvironment.instance( getContext() ).getMessager().printMessage( Diagnostic.Kind.NOTE, "Processing: " + element.getQualifiedName() );

        try  
        {
          ((ITypeProcessor)sp).process( element, this, issueReporter );
        }
        catch( Throwable e )
        {
          StringWriter stackTrace = new StringWriter();
          e.printStackTrace( new PrintWriter( stackTrace ) );
          issueReporter.reportError( "Fatal error processing with Manifold type processor: " + sp.getClass().getName() +
                                     "\non type: " + element.getQualifiedName() +
                                     "\nPlease report the error with the accompanying stack trace.\n" + stackTrace );
          throw e;
        }
      }
    }
  }

  public void addDrivers( Set<Object> drivers )
  {
    _drivers.addAll( drivers );
  }
  public Set<Object> getDrivers()
  {
    return _drivers;
  }

  // adds listener *before* TypeProcessor so that ExtensionTransformer processes whatever changes are made from listener
  public void addTaskListener( TaskListener listener )
  {
    getJavacTask().removeTaskListener( this );
    getJavacTask().addTaskListener( listener );
    getJavacTask().addTaskListener( this );
  }
}
