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

package manifold.templates;

import java.util.HashMap;
import manifold.templates.runtime.ILayout;

public class ManifoldTemplates
{
  private static HashMap<String, ILayout> DEFAULT_LAYOUT_MAP;
  private static TraceCallback TRACER = (c, t) -> {}; // NO-OP tracer by default

  static
  {
    DEFAULT_LAYOUT_MAP = new HashMap<>();
    DEFAULT_LAYOUT_MAP.put( "", ILayout.EMPTY );
  }

  public static void resetDefaultLayout()
  {
    DEFAULT_LAYOUT_MAP = new HashMap<>();
    DEFAULT_LAYOUT_MAP.put( "", ILayout.EMPTY );
  }

  public static void setDefaultLayout( ILayout layout )
  {
    DEFAULT_LAYOUT_MAP.put( "", layout );
  }

  public static void setDefaultLayout( String somePackage, ILayout layout )
  {
    DEFAULT_LAYOUT_MAP.put( somePackage, layout );
  }

  public static void trace()
  {
    traceWith( (template, timeToRender) -> System.out.println( " - Template " + template.getName() + " rendered in " + timeToRender + "ms" ) );
  }

  public static void traceWith( TraceCallback tracer )
  {
    TRACER = tracer;
  }

  public static ILayout getDefaultLayout( String packageName )
  {
    if( DEFAULT_LAYOUT_MAP.containsKey( packageName ) )
    {
      return DEFAULT_LAYOUT_MAP.get( packageName );
    }
    else
    {
      return getDefaultLayout( packageName.substring( 0, Math.max( 0, packageName.lastIndexOf( '.' ) ) ) );
    }
  }

  public static TraceCallback getTracer()
  {
    return TRACER;
  }

  public interface TraceCallback
  {
    void trace( Class template, long timeToRender );
  }
}
