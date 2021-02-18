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

package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.middle.FromClassFile;
import manifold.ext.props.middle.IFromClassFile;
import manifold.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings( "JavaReflectionMemberAccess" )
public class FromClassFileTest extends TestCase
{
  public void testStaticReadWriteBacking() throws Throwable
  {
    assertEquals( "staticReadwriteBackingProp", FromClassFile.staticReadwriteBackingProp );
    FromClassFile.staticReadwriteBackingProp = "b";
    assertEquals( "b", FromClassFile.staticReadwriteBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "staticReadwriteBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testStaticReadonlyBacking() throws Throwable
  {
    assertEquals( "staticReadonlyBackingProp", FromClassFile.staticReadonlyBackingProp );
    FromClassFile.updateStaticReadonlyBackingProp();
    assertEquals( "updated", FromClassFile.staticReadonlyBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "staticReadonlyBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromClassFile.class.getMethod( "staticReadonlyBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testStaticFinalBacking() throws Throwable
  {
    assertEquals( "staticFinalBackingProp", FromClassFile.staticFinalBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "staticFinalBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromClassFile.class.getMethod( "setStaticFinalBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testReadWriteBacking() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    assertEquals( "readwriteBackingProp", f.readwriteBackingProp );
    f.readwriteBackingProp = "b";
    assertEquals( "b", f.readwriteBackingProp );

    String r = f.readwriteBackingProp = "c";
    assertEquals( "c", r );

    Field field = FromClassFile.class.getDeclaredField( "readwriteBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testReadWriteBacking2() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    assertEquals( "readwriteBackingProp2", f.readwriteBackingProp2 );
    f.readwriteBackingProp2 = "b";
    assertEquals( "b", f.readwriteBackingProp2 );

    Field field = FromClassFile.class.getDeclaredField( "readwriteBackingProp2" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testReadonlyBackingProp() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    assertEquals( "readonlyBackingProp", f.readonlyBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "readonlyBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromClassFile.class.getMethod( "setReadonlyBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testWriteonlyBackingProp() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    f.writeonlyBackingProp = "c";

    ReflectUtil.LiveFieldRef ref = ReflectUtil.field( f, "writeonlyBackingProp" );
    assertNotNull( ref );
    assertTrue( Modifier.isPrivate( ref.getField().getModifiers() ) );
    assertEquals( "c", ref.get() );

    try
    {
      FromClassFile.class.getMethod( "getWriteonlyBackingProp" );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testFinalBacking() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    assertEquals( "finalBackingProp", f.finalBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "finalBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromClassFile.class.getMethod( "setFinalBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testStaticNonBacking() throws Throwable
  {
    // field should not exist
    ReflectUtil.FieldRef staticnNonbackingProp = ReflectUtil.field( FromClassFile.class, "staticNonbackingProp" );
    assertNull( staticnNonbackingProp );

    // nonbacking field works
    assertEquals( 8, FromClassFile.staticNonbackingProp );
    FromClassFile.staticNonbackingProp = 9;
    assertEquals( 9, FromClassFile.staticNonbackingProp );
  }

  public void testStaticNonBackingIface() throws Throwable
  {
    // field should not exist
    ReflectUtil.FieldRef staticNonbackingProp = ReflectUtil.field( IFromClassFile.class, "staticNonbackingProp" );
    assertNull( staticNonbackingProp );

    // nonbacking field works
    assertEquals( 5, IFromClassFile.staticNonbackingProp );
    try
    {
      IFromClassFile.staticNonbackingProp = 9;
      fail();
    }
    catch( UnsupportedOperationException value )
    {
      // the setter throws this exception to show it was called
      assertEquals( 9, Integer.valueOf( value.getMessage() ).intValue() );
    }

    assertEquals( 5, IFromClassFile.staticNonbackingProp );
  }

}
