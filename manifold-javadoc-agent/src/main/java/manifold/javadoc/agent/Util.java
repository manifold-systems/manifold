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
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import java.util.ServiceLoader;

public class Util
{
  public static void initJavacPlugin( Context context, ClassLoader cl )
  {
    // Load the JavacPlugin
    bypassJava9Security( cl );
    ServiceLoader<Plugin> sl = ServiceLoader.load( Plugin.class, cl );
    for( Plugin plugin : sl )
    {
      if( plugin.getName().equals( "Manifold" ) )
      {
        JavacProcessingEnvironment pEnv = JavacProcessingEnvironment.instance( context );
        plugin.init( JavacTask.instance( pEnv ) );
        break;
      }
    }
  }

  private static void bypassJava9Security( ClassLoader cl )
  {
    try
    {
      Class<?> Neu = Class.forName( "manifold.util.NecessaryEvilUtil", true, cl );
      Neu.getMethod( "bypassJava9Security" ).invoke( null );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
