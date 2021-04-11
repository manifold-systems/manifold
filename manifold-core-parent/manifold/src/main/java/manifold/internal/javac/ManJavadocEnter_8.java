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

package manifold.internal.javac;

import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javadoc.JavadocEnter;

// Override JavadocEnter so that JavacPlugin#initialize() is called before Enter starts
class ManJavadocEnter_8 extends JavadocEnter
{
  private final MultiTaskListener _taskListener;

  public static ManJavadocEnter_8 instance( Context context )
  {
    JavadocEnter enter = (JavadocEnter)context.get( enterKey );
    if( !(enter instanceof ManJavadocEnter_8) )
    {
      context.put( enterKey, (JavadocEnter)null );
      enter = new ManJavadocEnter_8( context );
    }

    return (ManJavadocEnter_8)enter;
  }

  protected ManJavadocEnter_8( Context context )
  {
    super( context );
    _taskListener = MultiTaskListener.instance( context );
  }

  @Override
  public void main( List<JCTree.JCCompilationUnit> trees )
  {
    if( !_taskListener.isEmpty() )
    {
      // we only need to call this once so that JavacPlugin#initialize() is called
      for( JCTree.JCCompilationUnit tree: trees )
      {
        TaskEvent e = new TaskEvent( TaskEvent.Kind.ENTER, tree );
        _taskListener.started( e );
      }
    }

    super.main( trees );
  }
}
