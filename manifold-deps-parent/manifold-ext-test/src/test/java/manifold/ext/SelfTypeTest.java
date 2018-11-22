package manifold.ext;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import manifold.ext.api.Self;

public class SelfTypeTest extends TestCase
{
//  List<@Self Foo> foo;
//  @Self Foo foos;

  public void testSelfType()
  {
    barQualifier();
    barQualifierRaw();
    new Foo<String>().fooQualifier();
    new Foo<String>().fooQualifier();
    new Bar<String>().fooQualifier();
    new Bar<String>().noQualifier();
  }

  static class Foo<T>
  {
    @Self Foo<T> getMe()
    {
      return this;
    }

    List<@Self Foo<T>> getListFoo()
    {
      return Collections.singletonList( this );
    }

    List<? extends @Self Foo<T>> getListFoo2()
    {
      return Collections.singletonList( this );
    }

    List<? extends Map<String, @Self Foo<T>>> getListMapFoo()
    {
      return Collections.singletonList( Collections.singletonMap( "hi", this ) );
    }

    List<? extends Map<@Self Foo<T>, String>> getListMapFoo2()
    {
      return Collections.singletonList( Collections.singletonMap( this, "hi" ) );
    }

    List<? extends Map<String, ? extends @Self Foo<T>>> getListMapFoo3()
    {
      return Collections.singletonList( Collections.singletonMap( "hi", this ) );
    }

    List<? extends Map<? extends @Self Foo<T>, String>> getListMapFoo4()
    {
      return Collections.singletonList( Collections.singletonMap( this, "hi" ) );
    }

    Map<String, @Self Foo<T>> getMapFoo()
    {
      return Collections.singletonMap( "hi", this );
    }

    Map<@Self Foo<T>, String> getMapFoo2()
    {
      return Collections.singletonMap( this, "hi" );
    }

    @Self Foo[] getArrayFoo()
    {
      Object array = Array.newInstance( getClass(), 1 );
      Array.set( array, 0, this );
      return (Foo[])array;
    }

    Foo @Self [] getArrayFoo2()
    {
      return getArrayFoo();
    }
    
    
    void fooQualifier()
    {
      Foo<T> foo = this.getMe();
      List<Foo<T>> list = this.getListFoo();
      List<? extends Foo<T>> list2 = this.getListFoo2();
      List<? extends Map<String, Foo<T>>> list3 = this.getListMapFoo();
      List<? extends Map<Foo<T>, String>> list4 = this.getListMapFoo2();
      List<? extends Map<String, ? extends Foo<T>>> list5 = this.getListMapFoo3();
      List<? extends Map<? extends Foo<T>, String>> list6 = this.getListMapFoo4();
      Map<String, Foo<T>> map = this.getMapFoo();
      Map<Foo<T>, String> maps = this.getMapFoo2();
      Foo<T>[] foos = this.getArrayFoo();
      Foo<T>[] foos2 = this.getArrayFoo2();
    }

    void noQualifier()
    {
      List<Foo<T>> list = getListFoo();
      List<? extends Foo<T>> list2 = getListFoo2();
      List<? extends Map<String, Foo<T>>> list3 = getListMapFoo();
      List<? extends Map<Foo<T>, String>> list4 = getListMapFoo2();
      List<? extends Map<String, ? extends Foo<T>>> list5 = getListMapFoo3();
      List<? extends Map<? extends Foo<T>, String>> list6 = getListMapFoo4();
      Map<String, Foo<T>> map = getMapFoo();
      Map<Foo<T>, String> maps = getMapFoo2();
      Foo<T>[] foos = getArrayFoo();
      Foo<T>[] foos2 = getArrayFoo2();
    }
  }

  static class Bar<T> extends Foo<T>
  {
    void noQualifier()
    {
      Bar<T> bar = getMe();
      List<Bar<T>> list = getListFoo();
      List<? extends Bar> list2 = getListFoo2();
      List<? extends Map<String, Bar<T>>> list3 = getListMapFoo();
      List<? extends Map<Bar<T>, String>> list4 = getListMapFoo2();
      List<? extends Map<String, ? extends Bar>> list5 = getListMapFoo3();
      List<? extends Map<? extends Bar, String>> list6 = getListMapFoo4();
      Map<String, Bar<T>> map = getMapFoo();
      Map<Bar<T>, String> maps = getMapFoo2();
      Bar[] bars = getArrayFoo();
      Bar[] bars2 = getArrayFoo2();
    }
  }

  private void barQualifier()
  {
    Bar<String> zeeBar = new Bar<>();
    Bar<String> bar = zeeBar.getMe();
    List<Bar<String>> list = zeeBar.getListFoo();
    List<? extends Bar<String>> list2 = zeeBar.getListFoo2();
    List<? extends Map<String, Bar<String>>> list3 = zeeBar.getListMapFoo();
    List<? extends Map<Bar<String>, String>> list4 = zeeBar.getListMapFoo2();
    List<? extends Map<String, ? extends Bar<String>>> list5 = zeeBar.getListMapFoo3();
    List<? extends Map<? extends Bar<String>, String>> list6 = zeeBar.getListMapFoo4();
    Map<String, Bar<String>> map = zeeBar.getMapFoo();
    Map<Bar<String>, String> maps = zeeBar.getMapFoo2();
    Bar[] bars = zeeBar.getArrayFoo();
    //Bar[] bars2  = zeeBar.getArrayFoo2();
  }

  private void barQualifierRaw()
  {
    Bar zeeBar = new Bar();
    Bar bar = zeeBar.getMe();
    List<Bar> list = zeeBar.getListFoo();
    List<? extends Bar> list2 = zeeBar.getListFoo2();
    List<? extends Map<String, Bar>> list3 = zeeBar.getListMapFoo();
    List<? extends Map<Bar, String>> list4 = zeeBar.getListMapFoo2();
    List<? extends Map<String, ? extends Bar>> list5 = zeeBar.getListMapFoo3();
    List<? extends Map<? extends Bar, String>> list6 = zeeBar.getListMapFoo4();
    Map<String, Bar> map = zeeBar.getMapFoo();
    Map<Bar, String> maps = zeeBar.getMapFoo2();
    Bar[] bars = zeeBar.getArrayFoo();
    //Bar[] bars2  = zeeBar.getArrayFoo2();
  }

}