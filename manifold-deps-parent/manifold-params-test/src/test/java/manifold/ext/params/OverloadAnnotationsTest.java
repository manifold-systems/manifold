package manifold.ext.params;

import junit.framework.TestCase;
import manifold.ext.params.rt.bool;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Verifies that annotations on the original method/constructor are preserved on the overloads generated for
 * optional parameters (see issue #780).
 */
public class OverloadAnnotationsTest extends TestCase
{
  @Retention( RetentionPolicy.RUNTIME ) @Target( ElementType.TYPE_USE )
  public @interface Nullable {}

  @Retention( RetentionPolicy.RUNTIME ) @Target( ElementType.PARAMETER )
  public @interface P1 {}

  @Retention( RetentionPolicy.RUNTIME ) @Target( ElementType.PARAMETER )
  public @interface P2 {}

  @Retention( RetentionPolicy.RUNTIME ) @Target( ElementType.PARAMETER )
  public @interface P3 {}

  public static class MyObject<T> {}

  public static class Sample
  {
    public <T extends @Nullable Object> @Nullable MyObject<T> test( @P1 MyObject<T> foo,
      @P2 @Nullable MyObject<T> bar = null,
      @P3 @Nullable MyObject<T> fooBar = null )
    {
      return null;
    }

    public Sample( @P1 String a, @P2 String b = "b", @P3 String c = "c" ) {}
  }

  private static final Class<?>[] PARAM_ANNOS = {P1.class, P2.class, P3.class};

  public void testMethodOverloads()
  {
    int checked = 0;
    for( Method m : Sample.class.getDeclaredMethods() )
    {
      if( !m.getName().equals( "test" ) && !m.getName().startsWith( "$test_" ) )
      {
        continue;
      }
      checked++;

      // type-use annotation on the type parameter bound: <T extends @Nullable Object>
      assertTrue( m + ": missing @Nullable on type parameter bound",
        m.getTypeParameters()[0].getAnnotatedBounds()[0].isAnnotationPresent( Nullable.class ) );

      // declaration annotations on parameters. All overloads keep the original parameter order, so the
      // parameter at index i always corresponds to PARAM_ANNOS[i]
      Class<?>[] types = m.getParameterTypes();
      Annotation[][] paramAnnos = m.getParameterAnnotations();
      assertEquals( m.toString(), types.length, paramAnnos.length );
      for( int i = 0; i < types.length; i++ )
      {
        assertAnnotated( m.toString(), paramAnnos[i], PARAM_ANNOS[i] );
      }

      // Executable#getAnnotatedParameterTypes() does not report type-use annotations reliably on JDK 8,
      // even though the class file is correct, so only check on newer JDKs.
      if( !isJava8() )
      {
        for( int i = 1; i < types.length; i++ )
        {
          assertTrue( m + ": missing @Nullable on parameter " + i,
            m.getAnnotatedParameterTypes()[i].isAnnotationPresent( Nullable.class ) );
        }
      }
    }
    // test(foo), test(foo, bar), test(foo, bar, fooBar), $test_bar, $test_fooBar
    assertEquals( 5, checked );
  }

  public void testReturnTypeAnnotation()
  {
    if( isJava8() )
    {
      // Method#getAnnotatedReturnType() doesn't report type annotations reliably on JDK 8,
      // even though the class file is correct, so only check on newer JDKs.
      return;
    }

    int checked = 0;
    for( Method m : Sample.class.getDeclaredMethods() )
    {
      // '$test_*' default value methods return the type of the optional parameter, so they are excluded
      if( !m.getName().equals( "test" ) )
      {
        continue;
      }
      checked++;

      assertTrue( m + ": missing @Nullable on return type",
        m.getAnnotatedReturnType().isAnnotationPresent( Nullable.class ) );
    }
    // test(foo), test(foo, bar), test(foo, bar, fooBar)
    assertEquals( 3, checked );
  }

  public void testConstructorOverloads()
  {
    int checked = 0;
    for( Constructor<?> c : Sample.class.getDeclaredConstructors() )
    {
      checked++;

      // Overloads having '$isXxx' flag parameters (type bool) are included. The flag parameters carry no
      // annotations, while the remaining parameters, in order, carry @P1, @P2, @P3.
      Class<?>[] types = c.getParameterTypes();
      Annotation[][] paramAnnos = c.getParameterAnnotations();
      assertEquals( c.toString(), types.length, paramAnnos.length );

      int original = 0;
      for( int i = 0; i < types.length; i++ )
      {
        if( types[i] == bool.class )
        {
          assertEquals( c + ": flag parameter " + i + " should not be annotated", 0, paramAnnos[i].length );
        }
        else
        {
          assertAnnotated( c.toString(), paramAnnos[i], PARAM_ANNOS[original++] );
        }
      }
    }
    // (a), (a, b), (a, b, c), (a, $isB, b), (a, $isB, b, $isC, c)
    assertEquals( 5, checked );
  }

  private static boolean isJava8()
  {
    return System.getProperty( "java.specification.version" ).equals( "1.8" );
  }

  private static void assertAnnotated( String where, Annotation[] annos, Class<?> expected )
  {
    if( annos.stream().noneMatch( a -> a.annotationType() == expected ) )
    {
      fail( where + ": missing @" + expected.getSimpleName() );
    }
  }
}