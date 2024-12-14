/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;

import javax.lang.model.type.NoType;
import java.util.ArrayList;

public class RecursiveTypeVarEraser extends Types.UnaryVisitor<Type>
{
  private final Types _types;

  public static Type eraseTypeVars( Types types, Type type )
  {
    if( type.isPrimitive() )
    {
      return type;
    }
    return new RecursiveTypeVarEraser( types ).visit( type );
  }

  private RecursiveTypeVarEraser( Types types )
  {
    _types = types;
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
      Type.TypeVar typeVar = null;
      if( arg instanceof Type.TypeVar )
      {
        typeVar = (Type.TypeVar)arg;
      }
      Type param = visit( arg );
      if( typeVar != null )
      {
        // TypeVar args must be replaced with Wildcards:
        //
        // public class Foo<T extends CharSequence> {
        // ...
        //   Foo<T> foo = blah;
        //   auto tuple = (foo, bar);
        // }
        //
        // tuple type for foo must be Foo<? extends CharSequence>, not Foo<CharSequence>
        //
        param = makeWildCard( param, typeVar );
      }

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

  private Type makeWildCard( Type t, Type.TypeVar typeVar )
  {
    return typeVar.getUpperBound() == null
      ? new Type.WildcardType( t, BoundKind.SUPER, t.tsym )
      : new Type.WildcardType( t, BoundKind.EXTENDS, t.tsym );
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
    return new Type.CapturedType( t.tsym.name, t.tsym.owner, w_bound, t.lower, t.wildcard );
  }

  @Override
  public Type visitTypeVar( Type.TypeVar t, Void s )
  {
    Type bound = eraseBound( t, t.getUpperBound() );
    Type lower = eraseBound( t, t.lower );
    return bound == null ? lower : bound;
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
    return t;
  }

  private Type eraseBound( Type t, Type bound )
  {
    if( bound == null || bound instanceof NoType )
    {
      return bound;
    }

    Type erasedBound;
    if( bound.contains( t ) )
    {
      erasedBound = visit( _types.erasure( bound ) );
    }
    else
    {
      erasedBound = visit( bound );
    }
    return erasedBound;
  }
}