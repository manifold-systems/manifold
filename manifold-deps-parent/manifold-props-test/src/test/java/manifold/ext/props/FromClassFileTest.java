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

  public void testInt_ReadWriteBacking() throws Throwable
  {
    FromClassFile f = new FromClassFile();
    assertEquals( 1, f.int_readwriteBackingProp );
    f.int_readwriteBackingProp = 2;
    assertEquals( 2, f.int_readwriteBackingProp );

    Field field = FromClassFile.class.getDeclaredField( "int_readwriteBackingProp" );
    assertNotNull( field );
    assertTrue( Modifier.isPrivate( field.getModifiers() ) );
  }

  public void testInt_ReadWriteBacking_AssignExpr()
  {
    FromClassFile f = new FromClassFile();

    int res = f.int_readwriteBackingProp = 3;
    assertEquals( 3, res );
    assertEquals( 3, f.int_readwriteBackingProp );

    res = 2 + (f.int_readwriteBackingProp = 4);
    assertEquals( 6, res );
    assertEquals( 4, f.int_readwriteBackingProp );

    f.int_readwriteBackingProp += 4;
    assertEquals( 8, f.int_readwriteBackingProp );

    res = 2 + (f.int_readwriteBackingProp += 4);
    assertEquals( 14, res );
    assertEquals( 12, f.int_readwriteBackingProp );
  }

  public void testInt_ReadWriteBacking_IncDec()
  {
    FromClassFile f = new FromClassFile();

    // ++
    //
    int res = ++f.int_readwriteBackingProp;
    assertEquals( 2, res );
    assertEquals( 2, f.int_readwriteBackingProp );

    res = f.int_readwriteBackingProp++;
    assertEquals( 2, res );
    assertEquals( 3, f.int_readwriteBackingProp );

    f.int_readwriteBackingProp++;
    assertEquals( 4, f.int_readwriteBackingProp );
    ++f.int_readwriteBackingProp;
    assertEquals( 5, f.int_readwriteBackingProp );

    // --
    //
    f.int_readwriteBackingProp = 10;

    res = --f.int_readwriteBackingProp;
    assertEquals( 9, res );
    assertEquals( 9, f.int_readwriteBackingProp );

    res = f.int_readwriteBackingProp--;
    assertEquals( 9, res );
    assertEquals( 8, f.int_readwriteBackingProp );

    f.int_readwriteBackingProp--;
    assertEquals( 7, f.int_readwriteBackingProp );
    --f.int_readwriteBackingProp;
    assertEquals( 6, f.int_readwriteBackingProp );
  }

  public void testInt_ReadWriteBacking_UnaryPlusMinus()
  {
    FromClassFile f = new FromClassFile();

    int res = -f.int_readwriteBackingProp;
    assertEquals( -1, res );

    res = +f.int_readwriteBackingProp;
    assertEquals( 1, res );
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

  public void testWriteonlyBackingProp()
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

  public void testStaticNonBacking()
  {
    // field should not exist
    ReflectUtil.FieldRef staticnNonbackingProp = ReflectUtil.field( FromClassFile.class, "staticNonbackingProp" );
    assertNull( staticnNonbackingProp );

    // nonbacking field works
    assertEquals( 8, FromClassFile.staticNonbackingProp );
    FromClassFile.staticNonbackingProp = 9;
    assertEquals( 9, FromClassFile.staticNonbackingProp );
  }

  public void testStaticNonBackingIface()
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

  public void testSub_Int_ReadWriteBacking() throws Throwable
  {
    new SubFromClassFile().testIdent_Int_ReadWriteBacking();
  }

  public void testSub_Int_ReadWriteBacking_AssignExpr()
  {
    new SubFromClassFile().testIdent_Int_ReadWriteBacking_AssignExpr();
  }

  public void testSub_Int_ReadWriteBacking_IncDec()
  {
    new SubFromClassFile().testIdent_Int_ReadWriteBacking_IncDec();
  }

  public void testSub_Int_ReadWriteBacking_UnaryPlusMinus()
  {
    new SubFromClassFile().testIdent_Int_ReadWriteBacking_UnaryPlusMinus();
  }

  static class SubFromClassFile extends FromClassFile
  {
    public void testIdent_ReadWriteBacking() throws Throwable
    {
      assertEquals( "readwriteBackingProp", readwriteBackingProp );
      readwriteBackingProp = "b";
      assertEquals( "b", readwriteBackingProp );

      String r = readwriteBackingProp = "c";
      assertEquals( "c", r );

      Field field = FromClassFile.class.getDeclaredField( "readwriteBackingProp" );
      assertNotNull( field );
      assertTrue( Modifier.isPrivate( field.getModifiers() ) );
    }

    public void testIdent_Int_ReadWriteBacking() throws Throwable
    {
      assertEquals( 1, int_readwriteBackingProp );
      int_readwriteBackingProp = 2;
      assertEquals( 2, int_readwriteBackingProp );

      Field field = FromClassFile.class.getDeclaredField( "int_readwriteBackingProp" );
      assertNotNull( field );
      assertTrue( Modifier.isPrivate( field.getModifiers() ) );
    }

    public void testIdent_Int_ReadWriteBacking_AssignExpr()
    {
      int res = int_readwriteBackingProp = 3;
      assertEquals( 3, res );
      assertEquals( 3, int_readwriteBackingProp );

      res = 2 + (int_readwriteBackingProp = 4);
      assertEquals( 6, res );
      assertEquals( 4, int_readwriteBackingProp );

      int_readwriteBackingProp += 4;
      assertEquals( 8, int_readwriteBackingProp );

      res = 2 + (int_readwriteBackingProp += 4);
      assertEquals( 14, res );
      assertEquals( 12, int_readwriteBackingProp );
    }

    public void testIdent_Int_ReadWriteBacking_IncDec()
    {
      // ++
      //
      int res = ++int_readwriteBackingProp;
      assertEquals( 2, res );
      assertEquals( 2, int_readwriteBackingProp );

      res = int_readwriteBackingProp++;
      assertEquals( 2, res );
      assertEquals( 3, int_readwriteBackingProp );

      int_readwriteBackingProp++;
      assertEquals( 4, int_readwriteBackingProp );
      ++int_readwriteBackingProp;
      assertEquals( 5, int_readwriteBackingProp );

      // --
      //
      int_readwriteBackingProp = 10;

      res = --int_readwriteBackingProp;
      assertEquals( 9, res );
      assertEquals( 9, int_readwriteBackingProp );

      res = int_readwriteBackingProp--;
      assertEquals( 9, res );
      assertEquals( 8, int_readwriteBackingProp );

      int_readwriteBackingProp--;
      assertEquals( 7, int_readwriteBackingProp );
      --int_readwriteBackingProp;
      assertEquals( 6, int_readwriteBackingProp );
    }

    public void testIdent_Int_ReadWriteBacking_UnaryPlusMinus()
    {
      int res = -int_readwriteBackingProp;
      assertEquals( -1, res );

      res = +int_readwriteBackingProp;
      assertEquals( 1, res );
    }
  }
}
