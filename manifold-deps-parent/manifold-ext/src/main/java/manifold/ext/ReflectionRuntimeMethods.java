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

import manifold.util.ReflectUtil;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ReflectionRuntimeMethods
{
  public static Object invoke_Object( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return ReflectUtil.method( receiver, name, paramTypes ).invoke( args );
  }

  public static boolean invoke_boolean( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (boolean)invoke_Object( receiver, name, paramTypes, args );
  }

  public static byte invoke_byte( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (byte)invoke_Object( receiver, name, paramTypes, args );
  }

  public static char invoke_char( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (char)invoke_Object( receiver, name, paramTypes, args );
  }

  public static int invoke_int( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (int)invoke_Object( receiver, name, paramTypes, args );
  }

  public static long invoke_long( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (long)invoke_Object( receiver, name, paramTypes, args );
  }

  public static float invoke_float( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (float)invoke_Object( receiver, name, paramTypes, args );
  }

  public static double invoke_double( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    return (double)invoke_Object( receiver, name, paramTypes, args );
  }

  public static void invoke_void( Object receiver, String name, Class[] paramTypes, Object[] args )
  {
    invoke_Object( receiver, name, paramTypes, args );
  }

  public static Object invokeStatic_Object( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    //noinspection ConstantConditions
    return ReflectUtil.method( cls, name, paramTypes ).invokeStatic( args );
  }

  public static boolean invokeStatic_boolean( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (boolean)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static byte invokeStatic_byte( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (byte)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static char invokeStatic_char( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (char)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static int invokeStatic_int( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (int)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static long invokeStatic_long( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (long)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static float invokeStatic_float( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (float)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static double invokeStatic_double( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    return (double)invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static void invokeStatic_void( Class cls, String name, Class[] paramTypes, Object[] args )
  {
    invokeStatic_Object( cls, name, paramTypes, args );
  }

  public static Object getField_Object( Object receiver, String name )
  {
    return ReflectUtil.field( receiver, name ).get();
  }

  public static boolean getField_boolean( Object receiver, String name )
  {
    return (boolean)getField_Object( receiver, name );
  }

  public static byte getField_byte( Object receiver, String name )
  {
    return (byte)getField_Object( receiver, name );
  }

  public static char getField_char( Object receiver, String name )
  {
    return (char)getField_Object( receiver, name );
  }

  public static short getField_short( Object receiver, String name )
  {
    return (short)getField_Object( receiver, name );
  }

  public static int getField_int( Object receiver, String name )
  {
    return (int)getField_Object( receiver, name );
  }

  public static long getField_long( Object receiver, String name )
  {
    return (long)getField_Object( receiver, name );
  }

  public static float getField_float( Object receiver, String name )
  {
    return (float)getField_Object( receiver, name );
  }

  public static double getField_double( Object receiver, String name )
  {
    return (double)getField_Object( receiver, name );
  }

  public static Object getFieldStatic_Object( Class receiver, String name )
  {
    //noinspection ConstantConditions
    return ReflectUtil.field( receiver, name ).getStatic();
  }

  public static boolean getFieldStatic_boolean( Class receiver, String name )
  {
    return (boolean)getFieldStatic_Object( receiver, name );
  }

  public static byte getFieldStatic_byte( Class receiver, String name )
  {
    return (byte)getFieldStatic_Object( receiver, name );
  }

  public static char getFieldStatic_char( Class receiver, String name )
  {
    return (char)getFieldStatic_Object( receiver, name );
  }

  public static short getFieldStatic_short( Class receiver, String name )
  {
    return (short)getFieldStatic_Object( receiver, name );
  }

  public static int getFieldStatic_int( Class receiver, String name )
  {
    return (int)getFieldStatic_Object( receiver, name );
  }

  public static long getFieldStatic_long( Class receiver, String name )
  {
    return (long)getFieldStatic_Object( receiver, name );
  }

  public static float getFieldStatic_float( Class receiver, String name )
  {
    return (float)getFieldStatic_Object( receiver, name );
  }

  public static double getFieldStatic_double( Class receiver, String name )
  {
    return (double)getFieldStatic_Object( receiver, name );
  }

  @SuppressWarnings("UnusedReturnValue")
  public static Object setField_Object( Object receiver, String name, Object value )
  {
    ReflectUtil.field( receiver, name ).set( value );
    return value;
  }

  public static boolean setField_boolean( Object receiver, String name, boolean value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static byte setField_byte( Object receiver, String name, byte value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static char setField_char( Object receiver, String name, char value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static short setField_short( Object receiver, String name, short value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static int setField_int( Object receiver, String name, int value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static long setField_long( Object receiver, String name, long value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static float setField_float( Object receiver, String name, float value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  public static double setField_double( Object receiver, String name, double value )
  {
    setField_Object( receiver, name, value );
    return value;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static Object setFieldStatic_Object( Class receiver, String name, Object value )
  {
    //noinspection ConstantConditions
    ReflectUtil.field( receiver, name ).setStatic( value );
    return value;
  }

  public static boolean setFieldStatic_boolean( Class receiver, String name, boolean value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static byte setFieldStatic_byte( Class receiver, String name, byte value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static char setFieldStatic_char( Class receiver, String name, char value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static short setFieldStatic_short( Class receiver, String name, short value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static int setFieldStatic_int( Class receiver, String name, int value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static long setFieldStatic_long( Class receiver, String name, long value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static float setFieldStatic_float( Class receiver, String name, float value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static double setFieldStatic_double( Class receiver, String name, double value )
  {
    setFieldStatic_Object( receiver, name, value );
    return value;
  }

  public static Object construct( Class type, Class[] paramTypes, Object[] args )
  {
    //noinspection ConstantConditions
    return ReflectUtil.constructor( type, paramTypes ).newInstance( args );
  }
}
