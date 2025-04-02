/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.api.util;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import manifold.internal.javac.JavacPlugin;
import manifold.util.JreUtil;

import javax.lang.model.SourceVersion;

public class JavacUtil
{
  public static SourceVersion getSourceVersion()
  {
    Context ctx = JavacPlugin.instance().getContext();
    JavacProcessingEnvironment jpe = JavacProcessingEnvironment.instance( ctx );
    return jpe.getSourceVersion();
  }

  public static int getSourceNumber()
  {
    return getSourceVersion().ordinal();
  }

  public static int getReleaseNumber()
  {
    String release = Options.instance( JavacPlugin.instance().getContext() ).get( "--release" );
    try
    {
      return Integer.parseInt( release );
    }
    catch( NumberFormatException e )
    {
      return JreUtil.JAVA_VERSION;
    }
  }
}
