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
import manifold.ext.props.example.FromSourceFile;
import manifold.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings( "JavaReflectionMemberAccess" )
public class FromSourceFileTest extends TestCase
{
  public void testStaticReadWriteBacking() throws Throwable
  {
    assertEquals( "staticReadwriteBackingProp", FromSourceFile.staticReadwriteBackingProp );
    FromSourceFile.staticReadwriteBackingProp = "b";
    assertEquals( "b", FromSourceFile.staticReadwriteBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "staticReadwriteBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testStaticReadonlyBacking() throws Throwable
  {
    assertEquals( "staticReadonlyBackingProp", FromSourceFile.staticReadonlyBackingProp );
    FromSourceFile.updateStaticReadonlyBackingProp();
    assertEquals( "updated", FromSourceFile.staticReadonlyBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "staticReadonlyBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromSourceFile.class.getMethod( "staticReadonlyBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testStaticFinalBacking() throws Throwable
  {
    assertEquals( "staticFinalBackingProp", FromSourceFile.staticFinalBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "staticFinalBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromSourceFile.class.getMethod( "setStaticFinalBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testReadWriteBacking() throws Throwable
  {
    FromSourceFile f = new FromSourceFile();
    assertEquals( "readwriteBackingProp", f.readwriteBackingProp );
    f.readwriteBackingProp = "b";
    assertEquals( "b", f.readwriteBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "readwriteBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testReadWriteBacking2() throws Throwable
  {
    FromSourceFile f = new FromSourceFile();
    assertEquals( "readwriteBackingProp2", f.readwriteBackingProp2 );
    f.readwriteBackingProp2 = "b";
    assertEquals( "b", f.readwriteBackingProp2 );

    Field field = FromSourceFile.class.getDeclaredField( "readwriteBackingProp2" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testReadonlyBackingProp() throws Throwable
  {
    FromSourceFile f = new FromSourceFile();
    assertEquals( "readonlyBackingProp", f.readonlyBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "readonlyBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromSourceFile.class.getMethod( "setReadonlyBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testWriteonlyBackingProp() throws Throwable
  {
    FromSourceFile f = new FromSourceFile();
    f.writeonlyBackingProp = "c";

    ReflectUtil.LiveFieldRef ref = ReflectUtil.field( f, "writeonlyBackingProp" );
    assertNotNull( ref );
    assertTrue( Modifier.isPrivate( ref.getField().getModifiers() ) );
    assertEquals( "c", ref.get() );

    try
    {
      FromSourceFile.class.getMethod( "getWriteonlyBackingProp" );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testFinalBacking() throws Throwable
  {
    FromSourceFile f = new FromSourceFile();
    assertEquals( "finalBackingProp", f.finalBackingProp );

    Field field = FromSourceFile.class.getDeclaredField( "finalBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );

    try
    {
      FromSourceFile.class.getMethod( "setFinalBackingProp", String.class );
      fail( "method should not exist" );
    }
    catch( NoSuchMethodException ignore )
    {
    }
  }

  public void testStaticNonBacking() throws Throwable
  {
    // field should not exist
    ReflectUtil.FieldRef staticnNonbackingProp = ReflectUtil.field( FromSourceFile.class, "staticNonbackingProp" );
    assertNull( staticnNonbackingProp );

    // nonbacking field works
    assertEquals( 8, FromSourceFile.staticNonbackingProp );
    FromSourceFile.staticNonbackingProp = 9;
    assertEquals( 9, FromSourceFile.staticNonbackingProp );
  }
}
