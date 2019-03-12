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

package manifold.internal.javac;

import com.sun.tools.javac.comp.LambdaToMethod;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import manifold.util.ReflectUtil;

public class ManTransTypes extends TransTypes
{
  private static final String TRANS_TYPES_FIELD = "transTypes";

  private int _translateCount;

  public static TransTypes instance( Context ctx )
  {
    TransTypes transTypes = ctx.get( transTypesKey );
    if( !(transTypes instanceof ManTransTypes) )
    {
      ctx.put( transTypesKey, (TransTypes)null );
      transTypes = new ManTransTypes( ctx );
    }

    return transTypes;
  }

  private ManTransTypes( Context ctx )
  {
    super( ctx );
    ReflectUtil.field( JavaCompiler.instance( ctx ), TRANS_TYPES_FIELD ).set( this );
    ReflectUtil.field( LambdaToMethod.instance( ctx ), TRANS_TYPES_FIELD ).set( this );
  }

  /**
   * Override to keep track of when/if translate() is in scope, if ManTypes#memberType() should not try to substitute
   * the qualifier type for @Self because the qualifier is not really a call site, rather it is the declaring class
   * of the method being checked for bridge method possibilities etc.  Thus we need to let the normal signature flow
   * through.
   */
  @Override
  public JCTree translateTopLevelClass( JCTree cdef, TreeMaker make )
  {
    _translateCount++;
    try
    {
      return super.translateTopLevelClass( cdef, make );
    }
    finally
    {
      _translateCount--;
    }
  }
  public boolean isTranslating()
  {
    return _translateCount > 0;
  }
}
