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

package manifold.util;

import manifold.util.concurrent.Cache;

import java.lang.reflect.Method;
import java.util.*;

public class MethodScorer
{
  public static final int BOXED_COERCION_SCORE = 10;
  public static final int PRIMITIVE_COERCION_SCORE = 24;
  private static volatile MethodScorer INSTANCE = null;

  private final Cache<Pair<Class, Class>, Integer> _typeScoreCache =
    Cache.make( "Type Score Cache", 10000,
      key -> _addToScoreForTypes( key.fst, key.snd ) );


  public static MethodScorer instance()
  {
    if( INSTANCE == null )
    {
      synchronized( MethodScorer.class )
      {
        if( INSTANCE == null )
        {
          INSTANCE = new MethodScorer();
        }
      }
    }
    return INSTANCE;
  }

  private MethodScorer()
  {
  }

  public List<MethodScore> scoreMethods( List<Method> funcTypes, List<Class> argTypes, Class returnType )
  {
    List<MethodScore> scores = new ArrayList<>();
    for( Method funcType : funcTypes )
    {
      scores.add( scoreMethod( funcType, argTypes, returnType ) );
    }
    Collections.sort( scores );
    return scores;
  }

  public MethodScore scoreMethod( Method funcType, List<Class> argTypes, Class returnType )
  {
    MethodScore score = new MethodScore( null );
    score.setValid( true );
    // Perform method scoring
    score.setMethod( funcType );
    score.incScore( scoreMethod( score, funcType, argTypes, returnType ) );

    return score;
  }

  public int scoreMethod( MethodScore score, Method funcType, List<Class> argTypes, Class returnType )
  {
    Class[] paramTypes = funcType.getParameterTypes();
    int iScore = 0;
    for( int i = 0; i < argTypes.size(); i++ )
    {
      if( paramTypes.length <= i )
      {
        // Extra argument  +Max+1
        iScore += Byte.MAX_VALUE + 1;
        score.setErrant( true );
        continue;
      }
      Class argType = argTypes.get( i );
      // Argument  +(0..Max)
      // function params are covariant wrt assignability from a call site
      int paramScore = addToScoreForTypes( paramTypes[i], argType );
      iScore += paramScore;
      if( paramScore >= Byte.MAX_VALUE - 1 )
      {
        score.setErrant( true );
      }
    }
    for( int i = argTypes.size(); i < paramTypes.length; i++ )
    {
      // Missing argument  +Max
      iScore += Byte.MAX_VALUE;
      score.setErrant( true );
    }

    if( funcType.isVarArgs() )
    {
      // Disambiguate Java varargs methods
      iScore += 1;
    }

    if( funcType.getReturnType() != void.class )
    {
      // Return type  +(0..Max)
      // function return type is contravariant wrt assignability from a call site
      int returnScore = addToScoreForTypes( returnType, funcType.getReturnType() );
      iScore += returnScore;
      if( returnScore >= Byte.MAX_VALUE - 1 )
      {
        score.setErrant( true );
      }
    }

    return iScore;
  }

  public int addToScoreForTypes( Class paramType, Class argType )
  {
    if( argType == paramType )
    {
      return 0;
    }
    return _typeScoreCache.get( new Pair<>( paramType, argType ) );
  }

  public int _addToScoreForTypes( Class<?> paramType, Class argType )
  {
    int iScore;
    Class primitiveArgType;
    Class primitiveParamType;
    if( paramType.equals( argType ) )
    {
      // Same types  +0
      iScore = 0;
    }
    else if( !paramType.isPrimitive() && argType == void.class )
    {
      // null  +1
      iScore = 1;
    }
    else if( arePrimitiveTypesCompatible( paramType, argType ) )
    {
      // Primitive coercion  +(2..9)
      iScore = PrimitiveUtil.getPriorityOf( paramType, argType );
    }
    else if( PrimitiveUtil.isBoxedTypeFor( paramType, argType ) ||
      PrimitiveUtil.isBoxedTypeFor( argType, paramType ) )
    {
      // Boxed coercion  +10
      iScore = BOXED_COERCION_SCORE;
    }
    else if( argType.isPrimitive() && PrimitiveUtil.isBoxed( paramType ) &&
      arePrimitiveTypesCompatible( primitiveParamType = PrimitiveUtil.getPrimitiveType( paramType ), argType ) )
    {
      // primitive -> Boxed coercion  +10 + Primitive coercion  +(12..19)
      iScore = BOXED_COERCION_SCORE + PrimitiveUtil.getPriorityOf( primitiveParamType, argType );
    }
    else if( PrimitiveUtil.isBoxed( argType ) && paramType.isPrimitive() &&
      arePrimitiveTypesCompatible( paramType, primitiveArgType = PrimitiveUtil.getPrimitiveType( argType ) ) )
    {
      // Boxed -> primitive coercion  +10 + Primitive coercion  +(12..19)
      iScore = BOXED_COERCION_SCORE + PrimitiveUtil.getPriorityOf( paramType, primitiveArgType );
    }
    else if( PrimitiveUtil.isBoxed( argType ) && PrimitiveUtil.isBoxed( paramType ) &&
      arePrimitiveTypesCompatible( primitiveParamType = PrimitiveUtil.getPrimitiveType( paramType ), primitiveArgType = PrimitiveUtil.getPrimitiveType( argType ) ) )
    {
      // Boxed -> Boxed coercion  +10 + 10 + Primitive coercion  +(22..29)
      iScore = BOXED_COERCION_SCORE + BOXED_COERCION_SCORE + PrimitiveUtil.getPriorityOf( primitiveParamType, primitiveArgType );
    }
    else
    {
      Class boxedArgType;
      if( argType.isPrimitive() && argType != void.class && !paramType.isPrimitive() &&
        paramType.isAssignableFrom( boxedArgType = PrimitiveUtil.getBoxedType( argType ) ) )
      {
        // Autobox type assignable  10 + Assignable degrees-of-separation
        iScore = BOXED_COERCION_SCORE + addDegreesOfSeparation( paramType, boxedArgType );
      }
      else if( paramType.isAssignableFrom( argType ) )
      {
        // Assignable types  0 + degrees-of-separation
        iScore = addDegreesOfSeparation( paramType, argType );
      }
      else
      {
      // Type not compatible  +Max-1
      iScore = Byte.MAX_VALUE - 1;
      }
    }
    return iScore;
  }

  private boolean arePrimitiveTypesCompatible( Class paramType, Class argType )
  {
    return PrimitiveUtil.arePrimitiveTypesAssignable( paramType, argType ) ||
      (paramType.isPrimitive() && argType.isPrimitive() &&
        (paramType != boolean.class) && (argType != boolean.class) &&
        (paramType != char.class) && (argType != char.class) &&
        (paramType != void.class) && (argType != void.class) &&
        PrimitiveUtil.losesInformation( argType, paramType ) <= 1);
  }

  public int addDegreesOfSeparation( Class parameterType, Class exprType )
  {
    return addDegreesOfSeparation( parameterType, TypeAncestry.instance().getTypesInAncestry( exprType ) );
  }

  public int addDegreesOfSeparation( Class<?> parameterType, Set<? extends Class> types )
  {
    int iScore = 0;

    for( Class type : types )
    {
      if( parameterType == type )
      {
        // don't include the same type in the hierarchy.  We are adding degrees because the arg type and param type are
        // different, but assignable, which means there must be at least one type in the hierarchy, if not the arg type
        // itself, that is not the parameter type.
        continue;
      }
      if( parameterType.isAssignableFrom( type ) )
      {
        iScore += 1;
      }
    }
    return iScore;
  }
  
  static class Pair<F,S>
  {
    private F fst;
    private S snd;

    public Pair( F fst, S snd )
    {
      this.fst = fst;
      this.snd = snd;
    }
  }
}
