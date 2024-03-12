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

package manifold.ext;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import manifold.rt.api.util.TypesUtil;

import java.util.ArrayList;
import javax.lang.model.type.NoType;

/**
 */
class StructuralTypeEraser extends Types.UnaryVisitor<Type>
{
  private ExtensionTransformer _extensionTransformer;
  Types _types;

  public StructuralTypeEraser( ExtensionTransformer extensionTransformer )
  {
    _extensionTransformer = extensionTransformer;
    _types = Types.instance( _extensionTransformer.getTypeProcessor().getContext() );
  }

  @Override
  public Type visitClassType( Type.ClassType t, Void s )
  {
    boolean erased = false;
    Type erasure = _types.erasure( t );
    Type base = visitType( erasure, s );
    if( base != erasure )
    {
      erased = true;
    }
    ArrayList<Type> params = new ArrayList<>();
    for( Type arg : t.allparams() )
    {
      Type param = visit( arg );
      params.add( param );
      if( param != arg )
      {
        erased = true;
      }
    }
    if( erased )
    {
      return new Type.ClassType( t.getEnclosingType(), List.from( params ), base.tsym );
    }
    return t;
  }

  @Override
  public Type visitArrayType( Type.ArrayType t, Void aVoid )
  {
    Type compType = visit( t.getComponentType() );
    if( compType == t.getComponentType() )
    {
      return t;
    }
    return new Type.ArrayType( compType, t.tsym );
  }

  @Override
  public Type visitCapturedType( Type.CapturedType t, Void s )
  {
    Type w_bound = t.wildcard.type;
    w_bound = eraseBound( t, w_bound );
    if( w_bound == t.wildcard.type )
    {
      return t;
    }
    return new Type.CapturedType( t.tsym.name, t.tsym.owner, t.getUpperBound(), t.lower, t.wildcard );
  }

  @Override
  public Type visitTypeVar( Type.TypeVar t, Void s )
  {
    Type bound = eraseBound( t, t.getUpperBound() );
    Type lower = eraseBound( t, t.lower );
    if( bound == t.getUpperBound() && lower == t.lower )
    {
      return t;
    }
    return new Type.TypeVar( t.tsym, bound, lower );
  }

  @Override
  public Type visitWildcardType( Type.WildcardType t, Void s )
  {
    Type bound = eraseBound( t, t.type );
    if( bound == t.type )
    {
      return t;
    }
    return new Type.WildcardType( bound, t.kind, t.tsym );
  }

  @Override
  public Type visitType( Type t, Void o )
  {
    if( TypesUtil.isStructuralInterface( _extensionTransformer.getTypeProcessor().getTypes(), t.tsym ) )
    {
      return _extensionTransformer.getObjectClass().asType();
    }
    return t;
  }

//  @Override
//  public Type visitMethodType( Type.MethodType mt, Void aVoid )
//  {
//    boolean erased = false;
//    ArrayList<Type> paramTypes = new ArrayList<>();
//    for( Type paramType: mt.getParameterTypes() )
//    {
//      Type param = visit( paramType );
//      if( param != paramType )
//      {
//        erased = true;
//      }
//      paramTypes.add( param );
//    }
//    Type returnType = visit( mt.getReturnType() );
//
//    if( returnType != mt.getReturnType() )
//    {
//      erased = true;
//    }
//    if( erased )
//    {
//      List<Type> pt = List.from( paramTypes );
//      mt = new Type.MethodType( pt, returnType, mt.getThrownTypes(), mt.asElement() );
//    }
//    return mt;
//  }

  private Type eraseBound( Type t, Type bound )
  {
    if( bound == null || bound instanceof NoType )
    {
      return bound;
    }

    Type erasedBound;
    if( bound.contains( t ) )
    {
      // short-circuit recursive type
      Type erasure = _types.erasure( bound );

      erasedBound = visit( erasure );

      if( _types.isSameType( erasure, erasedBound ) )
      {
        // erased type wasn't structural, change back to original recursive type
        erasedBound = bound;
      }
    }
    else
    {
      erasedBound = visit( bound );
    }
    return erasedBound;
  }
}
