package manifold.ext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import junit.framework.TestCase;
import manifold.ext.api.Self;
import manifold.ext.stuff.Car;
import manifold.ext.stuff.CarBuilder;


import static java.lang.System.out;

public class SelfTypeTest extends TestCase
{
  public void testSelfType()
  {
    barQualifier();
    barQualifierRaw();
    new Foo<String>().fooQualifier();
    new Foo<String>().fooQualifier();
    new Bar<String>().fooQualifier();
    new Bar<String>().noQualifier();
  }

  public void testMoreSelfType()
  {
    CarBuilder carBuilder = new CarBuilder();
    Car car = carBuilder
      .withName("Mach Five") // returns CarBuilder
      .withColor(255, 255, 255)
      .build();
    assertEquals("Mach Five", car.getName());
  }

  public void testExtensions()
  {
    "hi".something( "" );

    LinkedList<String> ll = new LinkedList<>();
    ll.add( "hi" );
    String result = ll.brapple( l -> l.getFirst() );
  }

  static class Foo<T>
  {
    @Self Foo<T> _parent;
    List<@Self Foo<T>> _children = new ArrayList<>();

    @Self Foo<T> getMe()
    {
      return this;
    }

    @Self Foo<T> getMeParam( @Self Foo<T> me )
    {
      return me;
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

    @Self Foo<T>[] getArrayFoo()
    {
      Object array = Array.newInstance( getClass(), 1 );
      Array.set( array, 0, this );
      return (Foo<T>[])array;
    }

    Foo<T> @Self [] getArrayFoo2()
    {
      return getArrayFoo();
    }

    Map<@Self Foo<T>, String> blahMap( Map<@Self Foo<T>, String> foo ){
      return Collections.singletonMap( this, "hi" );
    }
    List<@Self Foo<T>> blahList( List<@Self Foo<T>> foo ){
      return Collections.singletonList( null );
    }

    <R> R apply( Function<@Self Foo<T>, R> mapper )
    {
      return mapper.apply( this );
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
      List<Foo<T>> l = this.blahList( new ArrayList<Foo<T>>() );
      Map<Foo<T>, String> m = this.blahMap( new HashMap<Foo<T>, String>() );
      Foo<T> fooMe = this.apply( e -> e.getMe() );

      this._parent = new Foo<>();
      this._children.add( new Foo<T>() );
      Foo<T> child = _children.get(0);
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
      List<Foo<T>> l = blahList( new ArrayList<Foo<T>>() );
      Map<Foo<T>, String> m = blahMap( new HashMap<Foo<T>, String>() );
      Foo<T> fooMe = apply( e -> e.getMe() );

      _parent = new Foo<>();
      _children.add( new Foo<T>() );
      Foo<T> child = _children.get(0);
    }
  }

  static class Bar<T> extends Foo<T>
  {
    void barMethod() {}

    void noQualifier()
    {
      Bar<T> bar = getMe();
      getMeParam(this).barMethod();

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
      List<Bar<T>> l = blahList( new ArrayList<>() );
      Map<Bar<T>, String> m = blahMap( new HashMap<>() );
      Bar<T> barMe = apply( e -> e.getMe() );

      _parent = new Bar<>();
      _parent.barMethod();

      _children.add( new Bar<T>() );
      Bar<T> child = _children.get(0);
    }
  }

  static class Buz<S> extends Bar<S>
  {
    void buzMethod() {}

    void noQualifier()
    {
      Buz<S> buz = getMe();
      this.getMeParam(this).buzMethod();

      List<Buz<S>> list = getListFoo();
      List<? extends Buz> list2 = getListFoo2();
      List<? extends Map<String, Buz<S>>> list3 = getListMapFoo();
      List<? extends Map<Buz<S>, String>> list4 = getListMapFoo2();
      List<? extends Map<String, ? extends Buz>> list5 = getListMapFoo3();
      List<? extends Map<? extends Buz, String>> list6 = getListMapFoo4();
      Map<String, Buz<S>> map = getMapFoo();
      Map<Buz<S>, String> maps = getMapFoo2();
      Buz[] buzs = getArrayFoo();
      Buz[] buzs2 = getArrayFoo2();
      List<Buz<S>> l = blahList( new ArrayList<Buz<S>>() );
      Map<Buz<S>, String> m = blahMap( new HashMap<Buz<S>, String>() );
      Buz<S> buzMe = apply( e -> e.getMe() );

      _parent = new Buz<S>();
      _parent.buzMethod();

      _children.add( new Buz<S>() );
      Buz<S> child = _children.get(0);
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
    Bar[] bars2  = zeeBar.getArrayFoo2();
    List<Bar<String>> l = zeeBar.blahList( new ArrayList<Bar<String>>() );
    Map<Bar<String>, String> m = zeeBar.blahMap( new HashMap<Bar<String>, String>() );
    Bar<String> buzMe = zeeBar.apply( e -> e.getMe() );

    zeeBar._parent = new Bar<String>();
    zeeBar._parent.barMethod();

    zeeBar._children.add( new Bar<String>() );
    Bar<String> child = zeeBar._children.get(0);
  }

  private void barQualifierRaw()
  {
    Bar zeeBar = new Bar();

//    Bar bar = zeeBar.getMe();
//    bar.barMethod();

    List<Bar> list = zeeBar.getListFoo();
    List<? extends Bar> list2 = zeeBar.getListFoo2();
    List<? extends Map<String, Bar>> list3 = zeeBar.getListMapFoo();
    List<? extends Map<Bar, String>> list4 = zeeBar.getListMapFoo2();
    List<? extends Map<String, ? extends Bar>> list5 = zeeBar.getListMapFoo3();
    List<? extends Map<? extends Bar, String>> list6 = zeeBar.getListMapFoo4();
    Map<String, Bar> map = zeeBar.getMapFoo();
    Map<Bar, String> maps = zeeBar.getMapFoo2();
//    Bar[] bars = zeeBar.getArrayFoo();
    Bar[] bars2  = zeeBar.getArrayFoo2();
    List<Bar> l = zeeBar.blahList( new ArrayList<Bar>() );
    Map<Bar, String> m = zeeBar.blahMap( new HashMap<Bar, String>() );

//    zeeBar.addChild( new Bar() );
//    Bar child = zeeBar._children.get(0);

    //"".hi();
    //java.util.Date date = new java.util.Date( asdfg );
  }
}