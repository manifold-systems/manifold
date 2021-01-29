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

public class PrimitiveUtil
{
  public static int getPriorityOf( Class to, Class from )
  {
    if( to == from )
    {
      return 0;                       // score = 0
    }
//    if( JavaTypes.OBJECT().equals( to ) ) {
//      return MethodScorer.BOXED_COERCION_SCORE + 1;   // score = 11 (must be consistent with MethodScorer)
//    }

    int infoLoss = losesInformation( from, to );
    boolean sameFamily = isInSameFamily( from, to );

    int iScore = 1;
    if( infoLoss > 1 )
    {
      // Errant incompatible primitive types are treated identical to errant explicit coercible types because, same thing
      iScore += MethodScorer.PRIMITIVE_COERCION_SCORE; // score = 25
      if( !sameFamily )
      {
        iScore += 1;                                   // score = 26
      }
    }
    else if( sameFamily )
    {
      if( from == char.class || from == Character.class )
      {
        from = short.class; // char same distance to int as short
      }
      iScore += distance( from, to ); // score = (2..4)
    }
    else
    {
      iScore += infoLoss;             // score = (1..2)
      iScore += 4;                    // score = (5..6)
    }
    return iScore;
  }

  private static int distance( Class from, Class to )
  {
    int iDistance = getIndex( to ) - getIndex( from );
    return iDistance >= 0 ? iDistance : 5;
  }

  public static int losesInformation( Class from, Class to )
  {
    int[][] tab =
      {                                        //TO
        //FROM       boolean  char    byte    short   int     long    float   double
        /*boolean*/  {0,      0,      0,      0,      0,      0,      0,      0},
        /*char   */  {2,      0,      2,      2,      0,      0,      0,      1},
        /*byte   */  {2,      0,      0,      0,      0,      0,      0,      1},
        /*short  */  {2,      2,      2,      0,      0,      0,      0,      1},
        /*int    */  {2,      2,      2,      2,      0,      0,      0,      1},
        /*long   */  {2,      2,      2,      2,      2,      0,      0,      1},
        /*float  */  {2,      2,      2,      2,      2,      2,      0,      0},
        /*double */  {2,      2,      2,      2,      2,      2,      1,      0},
      };
    final int i = getIndex( from );
    final int j = getIndex( to );
    if( i == -1 || j == -1 )
    {
      return 0;
    }
    return tab[i][j];
  }

  private static int getIndex( Class type )
  {
    if( type == boolean.class || type == Boolean.class )
    {
      return 0;
    }
    else if( type == char.class || type == Character.class )
    {
      return 1;
    }
    else if( type == byte.class || type == Byte.class )
    {
      return 2;
    }
    else if( type == short.class || type == Short.class )
    {
      return 3;
    }
    else if( type == int.class || type == Integer.class )
    {
      return 4;
    }
    else if( type == long.class || type == Long.class )
    {
      return 5;
    }
    else if( type == float.class || type == Float.class )
    {
      return 6;
    }
    else if( type == double.class || type == Double.class )
    {
      return 7;
    }
    return -1;
  }

  private static boolean isInSameFamily( Class t1, Class t2 )
  {
    int indexT1 = getIndex( t1 );
    int indexT2 = getIndex( t2 );
    return indexT1 == indexT2 ||
      indexT1 > 0 && indexT1 < 6 && indexT2 > 0 && indexT2 < 6 ||
      indexT1 > 5 && indexT2 > 5;
  }

  public static boolean isBoxed( Class type )
  {
    return type == Boolean.class
      || type == Byte.class
      || type == Character.class
      || type == Double.class
      || type == Float.class
      || type == Integer.class
      || type == Long.class
      || type == Short.class
      || type == Void.class;
  }

  public static boolean isBoxedTypeFor( Class primitiveType, Class boxedType )
  {
    if( primitiveType != null && primitiveType.isPrimitive() )
    {
      if( primitiveType == boolean.class && boxedType == Boolean.class )
      {
        return true;
      }
      else if( primitiveType == char.class && boxedType == Character.class )
      {
        return true;
      }
      else if( primitiveType == byte.class && boxedType == Byte.class )
      {
        return true;
      }
      else if( primitiveType == short.class && boxedType == Short.class )
      {
        return true;
      }
      else if( primitiveType == int.class && boxedType == Integer.class )
      {
        return true;
      }
      else if( primitiveType == long.class && boxedType == Long.class )
      {
        return true;
      }
      else if( primitiveType == float.class && boxedType == Float.class )
      {
        return true;
      }
      else if( primitiveType == double.class && boxedType == Double.class )
      {
        return true;
      }
    }
    return false;
  }

  public static Class getPrimitiveType( Class boxedType )
  {
    if( boxedType == Boolean.class )
    {
      return boolean.class;
    }
    else if( boxedType == Character.class )
    {
      return char.class;
    }
    else if( boxedType == Byte.class )
    {
      return byte.class;
    }
    else if( boxedType == Short.class )
    {
      return short.class;
    }
    else if( boxedType == Integer.class )
    {
      return int.class;
    }
    else if( boxedType == Long.class )
    {
      return long.class;
    }
    else if( boxedType == Float.class )
    {
      return float.class;
    }
    else if( boxedType == Double.class )
    {
      return double.class;
    }
    return null;
  }

  public static Class getBoxedType( Class primitiveType )
  {
    if( primitiveType == boolean.class )
    {
      return Boolean.class;
    }
    else if( primitiveType == char.class )
    {
      return Character.class;
    }
    else if( primitiveType == byte.class )
    {
      return Byte.class;
    }
    else if( primitiveType == short.class )
    {
      return Short.class;
    }
    else if( primitiveType == int.class )
    {
      return Integer.class;
    }
    else if( primitiveType == long.class )
    {
      return Long.class;
    }
    else if( primitiveType == float.class )
    {
      return Float.class;
    }
    else if( primitiveType == double.class )
    {
      return Double.class;
    }
    return null;
  }

  public static boolean arePrimitiveTypesAssignable( Class toType, Class fromType )
  {
    if( toType == null || fromType == null || !toType.isPrimitive() || !fromType.isPrimitive() )
    {
      return false;
    }
    if( toType == fromType )
    {
      return true;
    }

    if( toType == double.class )
    {
      return fromType == float.class ||
        fromType == int.class ||
        fromType == char.class ||
        fromType == short.class ||
        fromType == byte.class;
    }
    if( toType == float.class )
    {
      return fromType == char.class ||
        fromType == short.class ||
        fromType == byte.class;
    }
    if( toType == long.class )
    {
      return fromType == int.class ||
        fromType == char.class ||
        fromType == short.class ||
        fromType == byte.class;
    }
    if( toType == int.class )
    {
      return fromType == short.class ||
        fromType == char.class ||
        fromType == byte.class;
    }
    if( toType == short.class )
    {
      return fromType == byte.class;
    }

    return false;
  }
}