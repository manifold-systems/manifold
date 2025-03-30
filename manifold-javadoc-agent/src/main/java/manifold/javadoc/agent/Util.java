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

package manifold.javadoc.agent;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.ServiceLoader;

public class Util
{
  public static void initJavacPlugin( Object context, ClassLoader cl )
  {
    // Load the JavacPlugin
    bypassJava9Security( cl );
    ServiceLoader<Plugin> sl = ServiceLoader.load( Plugin.class, cl );
    for( Plugin plugin : sl )
    {
      if( plugin.getName().equals( "Manifold" ) )
      {
        try
        {
          // note, using reflection here for the case where we build manifold with java 11 when we have to compile ManXxx_11 classes
          // otherwise, direct refs to com.sun.tools.javac.xxx classes fail to resolve cuz modules
          Class<?> jpeClass = Class.forName( "com.sun.tools.javac.processing.JavacProcessingEnvironment" );
          Class<?> ctxClass = Class.forName( "com.sun.tools.javac.util.Context" );
          Object jpe = jpeClass.getMethod( "instance", ctxClass ).invoke( null, context );
          plugin.init( JavacTask.instance( (ProcessingEnvironment)jpe ) );
          break;
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }

  private static void bypassJava9Security( ClassLoader cl )
  {
    try
    {
      Class<?> Neu = Class.forName( "manifold.util.JdkAccessUtil", true, cl );
      Neu.getMethod( "bypassJava9Security" ).invoke( null );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
