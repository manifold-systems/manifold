package manifold.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 */
public class TypeUtil
{
//  public static Type getActualType( Type type, TypeVarToTypeMap actualParamByVarName )
//  {
//    return getActualType( type, actualParamByVarName, false );
//  }
//
//  public static Type getActualType( Type type, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars )
//  {
//    return getActualType( type, actualParamByVarName, bKeepTypeVars, new LinkedHashSet<Type>() );
//  }
//  public static Type getActualType( Type type, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars, LinkedHashSet<Type> recursiveTypes )
//  {
//    Type retType;
//    if( type instanceof Class )
//    {
//      retType = type;
//    }
//    else if( type instanceof TypeVariable )
//    {
//      retType = actualParamByVarName.getByMatcher( (TypeVariable)type, RawTypeVarMatcher.instance() );
//      if( retType == null )
//      {
//        // the type must come from the map, otherwise it comes from a context where there is no argument for the type var, hence the error type
//        return null;
//      }
//      else if( !bKeepTypeVars )
//      {
//        retType = getDefaultParameterizedTypeWithTypeVars( retType );
//      }
//    }
//    else if( type instanceof WildcardType )
//    {
//      Type bound = ((WildcardType)type).getUpperBounds()[0];
//      Type lowerBound = maybeGetLowerBound( (WildcardType)type, actualParamByVarName, bKeepTypeVars, recursiveTypes );
//      if( lowerBound != null )
//      {
//        bound = lowerBound;
//      }
//      retType = getActualType( bound, actualParamByVarName, bKeepTypeVars, recursiveTypes );
//      if( retType instanceof TypeVariable )
//      {
//        TypeVariableDefinition tvd = ((TypeVariable)retType).getTypeVarDef().clone();
//        retType = new TypeVariable( tvd, ((TypeVariable)retType).isFunctionStatement() );
//        ((TypeVariable)retType).getTypeVarDef().setVariance( ((WildcardType)type).getLowerBounds().length == 0 ? Variance.WILD_COVARIANT : Variance.WILD_CONTRAVARIANT );
//      }
//    }
//    else if( type instanceof ParameterizedType )
//    {
//      recursiveTypes.add( type );
//      try
//      {
//        Type genType = getActualType( ((ParameterizedType)type).getRawType(), actualParamByVarName, bKeepTypeVars, recursiveTypes );
//        Type[] typeArgs = ((ParameterizedType)type).getActualTypeArguments();
//        if( typeArgs == null || typeArgs.length == 0 )
//        {
//          retType = genType;
//        }
//        else
//        {
//          Type[] types = new Type[typeArgs.length];
//          for( int i = 0; i < types.length; i++ )
//          {
//            Type typeArg = typeArgs[i];
//            if( !bKeepTypeVars && typeArg instanceof TypeVariable )
//            {
//              Type bound = ((TypeVariable)typeArg).getBounds()[0];
//              if( !recursiveTypes.contains( bound ) )
//              {
//                types[i] = getActualType( bound, actualParamByVarName, bKeepTypeVars, recursiveTypes );
//              }
//              else if( bound instanceof ParameterizedType )
//              {
//                types[i] = getActualType( ((ParameterizedType)bound).getRawType(), actualParamByVarName, bKeepTypeVars, recursiveTypes );
//              }
//              else
//              {
//                throw new IllegalStateException( "Expecting bound to be a ParameterizedType here" );
//              }
//            }
//            else
//            {
//              if( typeArg instanceof WildcardType && (((WildcardType)typeArg).getUpperBounds()[0].equals( Object.class ) ||
//                                                      ((WildcardType)typeArg).getLowerBounds().length > 0) )
//              {
//                Type lowerBound = maybeGetLowerBound( (WildcardType)typeArg, actualParamByVarName, bKeepTypeVars, recursiveTypes );
//                if( lowerBound == null )
//                {
//                  // Object is the default type for the naked <?> wildcard, so we have to get the actual bound, if different, from the corresponding type var
//                  Type[] boundingTypes = ((Class)((ParameterizedType)type).getRawType()).getTypeParameters()[i].getBounds();
//                  Type boundingType = boundingTypes.length == 0 ? null : boundingTypes[0];
//                  if( boundingType != null )
//                  {
//                    typeArg = boundingType;
//                  }
//                }
//              }
//
//              types[i] = getActualType( typeArg, actualParamByVarName, bKeepTypeVars, recursiveTypes );
//            }
//          }
//          retType = genType.getParameterizedType( types );
//        }
//      }
//      finally
//      {
//        recursiveTypes.remove( type );
//      }
//    }
//    else if( type instanceof GenericArrayType )
//    {
//      retType = getActualType( ((GenericArrayType)type).getGenericComponentType(), actualParamByVarName, bKeepTypeVars, recursiveTypes ).getArrayType();
//    }
//    else
//    {
//      //retType = parseType( normalizeJavaTypeName( type ), actualParamByVarName, bKeepTypeVars, null );
//      throw new IllegalStateException();
//    }
//    return retType;
//  }
//
//  private static Type maybeGetLowerBound( WildcardType type, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars, LinkedHashSet<Type> recursiveTypes )
//  {
//    Type[] lowers = type.getLowerBounds();
//    if( lowers != null && lowers.length > 0 && recursiveTypes.size() > 0 )
//    {
//      // This is a "super" (contravariant) wildcard
//
//      LinkedList<Type> list = new LinkedList<>( recursiveTypes );
//      Type enclType = list.getLast();
//      if( enclType instanceof ParameterizedType )
//      {
//        Type genType = getActualType( ((ParameterizedType)enclType).getRawType(), actualParamByVarName, bKeepTypeVars, recursiveTypes );
//        if( genType instanceof IJavaType )
//        {
//          if( FunctionToInterfaceCoercer.getSingleMethodFromJavaInterface( (IJavaType)genType ) != null )
//          {
//            // For functional interfaces we keep the lower bound as an upper bound so that blocks maintain contravariance wrt the single method's parameters
//            return lowers[0];
//          }
//        }
//      }
//    }
//    return null;
//  }
}
