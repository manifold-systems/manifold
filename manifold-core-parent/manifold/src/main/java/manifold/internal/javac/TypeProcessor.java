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

import com.sun.tools.javac.api.BasicJavacTask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import manifold.api.host.IManifoldHost;
import manifold.api.type.ICompilerComponent;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.ConcurrentHashSet;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  private final Set<Object> _drivers;
  private SortedSet<ICompilerComponent> _compilerComponents;

  TypeProcessor( IManifoldHost host, BasicJavacTask javacTask )
  {
    super( host, javacTask );
    _drivers = new ConcurrentHashSet<>();
    loadCompilerComponents( javacTask );
  }

  private void loadCompilerComponents( BasicJavacTask javacTask )
  {
    _compilerComponents = new TreeSet<>( Comparator.comparing( c -> c.getClass().getTypeName() ) );
    ServiceUtil.loadRegisteredServices( _compilerComponents, ICompilerComponent.class, getClass().getClassLoader() );
    _compilerComponents.forEach( cc -> cc.init( javacTask, this ) );
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
}
