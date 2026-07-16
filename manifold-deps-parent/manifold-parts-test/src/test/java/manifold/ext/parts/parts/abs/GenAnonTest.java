package manifold.ext.parts.parts.abs;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

import java.util.*;
import java.time.LocalDate;

public class GenAnonTest extends TestCase
{
    static class MyString implements CharSequence {
        @link String string;

        public MyString(String string) {
            this.string = string;
        }
    }

    static class MyList2<E> extends ArrayList<E>
    {
        public MyList2() {
        }

        public List<E> reversed()
        {
            return null;
        }
    }


    static class MyList<E> implements List<E> {
        @link List<E> l = new ArrayList<>();
    }
    interface Fooo<E> {
        E asdf( E e );
    }
    interface Bar<E> extends Fooo<String> {
    }
    static class FooImpl<E> implements Bar<E> {
        @link Bar<E> foo;
    }

    static class StringMap<E> implements Map<String, E> {
        @link Map<String, E> m = new HashMap<>();

        public boolean equals(Object o) {return m.equals(o);}
        public int hashCode() {return m.hashCode();}
    }

    interface A {
        String a(String a);
        String b(String b);
    }
    static @part class AA implements A {
        @Override
        public String a(String a) {
            return a + b(a);
        }
        @Override
        public String b(String b) {
            return b;
        }
    }
    static @part class BB extends AA {
    }
    static @part class MyAB implements A {
        @link AA aa = new BB();

        @Override
        public String b(String b) {
            return aa.b(b) + "y_z";
        }
    }

//    static class MyOtherMap<A, E> implements List<A>, Map<A, E> {
//        @link Map<A, E> m = new HashMap<>();
//        @link List<A>  l;
//    }

    static class Foo extends MyList<String> {
        @Override
        public String get(int index) {
            return super.get(index);
        }
    }

    public void testMe() {
        MyList<String> r = new MyList<String>() {
            @Override
            public String get(int index) {
                return super.get(index);
            }
        };
        r.add( "hi" );
        String[] array = r.toArray(new String[0]);
        System.out.println( array[0] );

        MyAB ab = new MyAB();
        System.out.println(ab.a("x_"));

        MyString s = new MyString("hello");
        System.out.println(s.charAt(0));

        StringMap<LocalDate> map = new StringMap<>();
        map.put("Alabama", LocalDate.of(1819, 12, 14));
        map.put("Alaska", LocalDate.of(1959, 3, 1));
        LocalDate statehood = map.get("Alabama");
        System.out.println(statehood);

    }
}