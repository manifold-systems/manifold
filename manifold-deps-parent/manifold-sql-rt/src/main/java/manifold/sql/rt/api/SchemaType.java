/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.rt.api;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.util.ReflectUtil;

/**
 * Common base type for db schema generated types.
 */
public interface SchemaType extends IBindingsBacked
{
  //## todo: revisit
  default Object makeDefaultValue( String stringValue, Class<?> type )
  {
    if( stringValue == null )
    {
      return null;
    }
    if( stringValue.isEmpty() && type == String.class )
    {
      return "";
    }

    if( stringValue.length() > 1 &&
      (stringValue.charAt( 0 ) == '\'' || stringValue.charAt( 0 ) == '"') )
    {
      return stringValue.substring( 1, stringValue.length() - 1 );
    }

    ReflectUtil.MethodRef valueOf = ReflectUtil.method( type, "valueOf", String.class );
    if( valueOf != null )
    {
      return valueOf.invokeStatic( stringValue );
    }
    ReflectUtil.ConstructorRef constructor = ReflectUtil.constructor( type, String.class );
    if( constructor != null )
    {
      return constructor.newInstance( stringValue );
    }
    return null;
//    throw new RuntimeException( "Failed to parse default value: \"" + stringValue + "\" for type: " + type.getTypeName() );

//    try {
//      switch (_typeCode) {
//        case Types.TINYINT:
//        case Types.SMALLINT:
//          return new Short(stringValue);
//        case Types.INTEGER:
//          return new Integer(stringValue);
//        case Types.BIGINT:
//          return new Long(stringValue);
//        case Types.DECIMAL:
//        case Types.NUMERIC:
//          return new BigDecimal(stringValue);
//        case Types.REAL:
//          return new Float(stringValue);
//        case Types.DOUBLE:
//        case Types.FLOAT:
//          return new Double(stringValue);
//        case Types.DATE:
//          return Date.valueOf(stringValue);
//        case Types.TIME:
//          return Time.valueOf(stringValue);
//        case Types.TIMESTAMP:
//          return Timestamp.valueOf(stringValue);
//        case Types.BIT:
//        case Types.BOOLEAN:
//          return ConvertUtils.convert(stringValue, Boolean.class);
//      }
//    } catch (NumberFormatException ex) {
//      return null;
//    } catch (IllegalArgumentException ex) {
//      return null;
//    }
//    return stringValue;
  }

}
