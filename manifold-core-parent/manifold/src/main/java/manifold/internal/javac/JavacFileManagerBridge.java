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

import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.*;

import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

/**
 * The purpose of this class is to make our ManifoldJavaFileManager a JavacFileManager, which is necessary for
 * straight usage of javac.exe on the command line; other javac usage such as via Maven, Gradle, and more generally
 * via the Java Compiler API do not require our file manager to extend JavacFileManager.  Otherwise, we'd extend
 * ForwardingJavaFileManager.
 */
public class JavacFileManagerBridge<M extends JavaFileManager> extends JavacFileManager implements PreJava17JavacFileManagerMethod
{
  /**
   * The file manager which all methods are delegated to.
   */
  private final M fileManager;

  /**
   * Create a JavacFileManager using a given context, optionally registering
   * it as the JavaFileManager for that context.
   */
  JavacFileManagerBridge( M fileManager, Context context )
  {
    super( context, false, null );
    this.fileManager = fileManager;
  }

  /**
   */
  public ClassLoader getClassLoader( Location location )
  {
    return fileManager.getClassLoader( location );
  }

  /**
   */
  public Iterable<JavaFileObject> list( Location location,
                                        String packageName,
                                        Set<JavaFileObject.Kind> kinds,
                                        boolean recurse )
    throws IOException
  {
    return fileManager.list( location, packageName, kinds, recurse );
  }

  /**
   */
  public String inferBinaryName( Location location, JavaFileObject file )
  {
    return fileManager.inferBinaryName( location, file );
  }

  /**
   */
  public boolean isSameFile( FileObject a, FileObject b )
  {
    return fileManager.isSameFile( a, b );
  }

  /**
   */
  public boolean handleOption( String current, Iterator<String> remaining )
  {
    return fileManager.handleOption( current, remaining );
  }

  public boolean hasLocation( Location location )
  {
    boolean hasLocation = fileManager.hasLocation( location );

    if( JreUtil.isJava8() )
    {
      return hasLocation;
    }

    // Java 9 introduces StandardJavaFileManager#getLocationAsPaths() for validation, but there is a bug in their code
    // where it does not check for empty iterable, which is what we do here
    if( hasLocation )
    {
      ReflectUtil.LiveMethodRef getLocationAsPaths = findStandardJavaFileManagerMethod(
        fileManager, "getLocationAsPaths", Location.class );
      if( getLocationAsPaths != null )
      {
        Iterable iter = getLocationAsPaths( location );
        if( iter == null || !iter.iterator().hasNext() )
        {
          hasLocation = false;
        }
      }
    }
    return hasLocation;
  }

  @Override
  public Iterable<? extends File> getLocation( Location location )
  {
    ReflectUtil.LiveMethodRef getLocation = findStandardJavaFileManagerMethod(
      fileManager, "getLocation", Location.class );

    //noinspection unchecked
    return (Iterable)getLocation.invoke( location );
  }

  // exclusive to Java 9, also note the PreJava17JavacFileManagerMethod impl
  public Collection<? extends Path> getLocationAsPaths( Location location )
  {
    ReflectUtil.LiveMethodRef getLocationAsPaths = findStandardJavaFileManagerMethod(
      fileManager, "getLocationAsPaths", Location.class );

    //noinspection unchecked
    return (Collection)getLocationAsPaths.invoke( location );
  }

  public static ReflectUtil.LiveMethodRef findStandardJavaFileManagerMethod( JavaFileManager fm, String name, Class... params )
  {
    ReflectUtil.LiveMethodRef methodRef = ReflectUtil.WithNull.method( fm, name, params );

    // Some build systems (Gradle 6.x) may use ClientCodeWrapper variant, WrappedJavaFileManager, as opposed to the
    // expected WrappedStandardJavaFileManager, thus we must find the wrapped StandardJavaFileManager and delegate the
    // call.

    while( methodRef == null && fm.getClass().getTypeName().equals( "com.sun.tools.javac.main.DelegatingJavaFileManager" ) )
    {
      fm = (JavaFileManager)ReflectUtil.field( fm, "baseFM" ).get();
      methodRef = ReflectUtil.WithNull.method( fm, name, params );
    }

    while( methodRef == null && fm.getClass().getEnclosingClass() == ClientCodeWrapper.class )
    {
      fm = (JavaFileManager)ReflectUtil.field( fm, "clientJavaFileManager" ).get();
      methodRef = ReflectUtil.WithNull.method( fm, name, params );
    }

    while( methodRef == null && fm instanceof ForwardingJavaFileManager )
    {
      fm = (JavaFileManager)ReflectUtil.field( fm, "fileManager" ).get();
      methodRef = ReflectUtil.WithNull.method( fm, name, params );
    }

    return methodRef;
  }

  public int isSupportedOption( String option )
  {
    return fileManager.isSupportedOption( option );
  }

  /**
   */
  public JavaFileObject getJavaFileForInput( Location location,
                                             String className,
                                             JavaFileObject.Kind kind )
    throws IOException
  {
    return fileManager.getJavaFileForInput( location, className, kind );
  }

  /**
   */
  public JavaFileObject getJavaFileForOutput( Location location,
                                              String className,
                                              JavaFileObject.Kind kind,
                                              FileObject sibling )
    throws IOException
  {
    return fileManager.getJavaFileForOutput( location, className, kind, sibling );
  }

  /**
   */
  public FileObject getFileForInput( Location location,
                                     String packageName,
                                     String relativeName )
    throws IOException
  {
    return fileManager.getFileForInput( location, packageName, relativeName );
  }

  /**
   */
  public FileObject getFileForOutput( Location location,
                                      String packageName,
                                      String relativeName,
                                      FileObject sibling )
    throws IOException
  {
    return fileManager.getFileForOutput( location, packageName, relativeName, sibling );
  }

  public void flush()
  {
    try
    {
      fileManager.flush();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public void close()
  {
    try
    {
      fileManager.close();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Location getLocationForModule( Location location, String moduleName ) throws IOException
  {
    //return fileManager.getLocationForModule(location, moduleName);
    try
    {
      ReflectUtil.LiveMethodRef getLocationForModule = ReflectUtil.method( fileManager, "getLocationForModule", Location.class, String.class );
      return (Location)getLocationForModule.invoke( location, moduleName );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Location getLocationForModule( Location location, JavaFileObject fo ) throws IOException
  {
    //return fileManager.getLocationForModule(location, fo);
    try
    {
      ReflectUtil.LiveMethodRef getLocationForModule = ReflectUtil.method( fileManager, "getLocationForModule", Location.class, JavaFileObject.class );
      return (Location)getLocationForModule.invoke( location, fo );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public <S> ServiceLoader<S> getServiceLoader( Location location, Class<S> service ) throws IOException
  {
    //return fileManager.getServiceLoader(location, service);
    try
    {
      ReflectUtil.LiveMethodRef getServiceLoader = ReflectUtil.method( fileManager, "getServiceLoader", Location.class, Class.class );
      //noinspection unchecked
      return (ServiceLoader)getServiceLoader.invoke( location, service );
    }
    catch( Exception e )
    {
      throw new IOException( e );
    }
  }

  /**
   * @since 9
   */
  public String inferModuleName( Location location )
  {
    //return fileManager.inferModuleName( location );
    try
    {
      ReflectUtil.LiveMethodRef inferModuleName = ReflectUtil.method( fileManager, "inferModuleName", Location.class );
      return (String)inferModuleName.invoke( location );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Iterable<Set<Location>> listLocationsForModules( Location location ) throws IOException
  {
    //return fileManager.listLocationsForModules( location );
    try
    {
      ReflectUtil.LiveMethodRef listLocationsForModules = ReflectUtil.method( fileManager, "listLocationsForModules", Location.class );
      //noinspection unchecked
      return (Iterable)listLocationsForModules.invoke( location );
    }
    catch( Exception e )
    {
      throw new IOException( e );
    }
  }

  /**
   * @since 9
   */
  public boolean contains( Location location, FileObject fo ) throws IOException
  {
    //return fileManager.contains( location, fo );
    try
    {
      ReflectUtil.LiveMethodRef contains = ReflectUtil.method( fileManager, "contains", Location.class, FileObject.class );
      return (boolean)contains.invoke( location, fo );
    }
    catch( Exception e )
    {
      if( fo instanceof GeneratedJavaStubFileObject )
      {
        //## todo: ...
        return true;
      }
      throw new IOException( e );
    }
  }
}
