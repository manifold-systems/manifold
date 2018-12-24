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

package manifold.internal.javac.templ;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Names;

class NameReplacer extends TreeTranslator
{
  private final BasicJavacTask _javacTask;
  private final int _offset;

  NameReplacer( BasicJavacTask javacTask, int offset )
  {
    _offset = offset;
    _javacTask = javacTask;
  }

  @Override
  public void visitIdent( JCTree.JCIdent jcIdent )
  {
    super.visitIdent( jcIdent );
    Names names = Names.instance( _javacTask.getContext() );
    jcIdent.name = names.fromString( jcIdent.name.toString() );
    jcIdent.pos = _offset;
  }

  @Override
  public void visitSelect( JCTree.JCFieldAccess jcFieldAccess )
  {
    super.visitSelect( jcFieldAccess );
    Names names = Names.instance( _javacTask.getContext() );
    jcFieldAccess.name = names.fromString( jcFieldAccess.name.toString() );
    jcFieldAccess.pos = _offset;
  }
}
