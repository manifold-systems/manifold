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

package manifold.ext.rt.proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import manifold.util.JreUtil;
import manifold.util.ReflectUtil;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * Adapted from java.lang.reflect.Proxy to remove the CHECKCAST instruction if the return type is a Structural interface,
 * otherwise the proxy method call fails on such a call because the return value doesn't nominally implement the
 * interface.
 * <p/>
 * Note this Proxy class is an amalgamation of the Java 8 and Java 9+ Proxy classes to support both with a single
 * codebase, as manifold does. This Proxy class is mostly the Java 8 one, but handles modules when running in a
 * Java 9+ VM by calling into the Java 9+ Proxy.ProxyBuilder to provide module for the proxy.
 */
public class Proxy implements java.io.Serializable {

  private static final long serialVersionUID = -2222568056686623797L;

  /** parameter types of a proxy class constructor */
  private static final Class<?>[] constructorParams =
    { InvocationHandler.class };

  /**
   * a cache of proxy classes
   */
  private static final WeakCache<ClassLoader, Class<?>[], Class<?>>
    proxyClassCache = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());

  /**
   * the invocation handler for this proxy instance.
   * @serial
   */
  protected InvocationHandler h;

  /**
   * Prohibits instantiation.
   */
  private Proxy() {
  }

  /**
   * Constructs a new {@code Proxy} instance from a subclass
   * (typically, a dynamic proxy class) with the specified value
   * for its invocation handler.
   *
   * @param  h the invocation handler for this proxy instance
   *
   * @throws NullPointerException if the given invocation handler, {@code h},
   *         is {@code null}.
   */
  protected Proxy(InvocationHandler h) {
    Objects.requireNonNull(h);
    this.h = h;
  }

  /**
   * Returns the {@code java.lang.Class} object for a proxy class
   * given a class loader and an array of interfaces.  The proxy class
   * will be defined by the specified class loader and will implement
   * all of the supplied interfaces.  If any of the given interfaces
   * is non-public, the proxy class will be non-public. If a proxy class
   * for the same permutation of interfaces has already been defined by the
   * class loader, then the existing proxy class will be returned; otherwise,
   * a proxy class for those interfaces will be generated dynamically
   * and defined by the class loader.
   *
   * <p>There are several restrictions on the parameters that may be
   * passed to {@code Proxy.getProxyClass}:
   *
   * <ul>
   * <li>All of the {@code Class} objects in the
   * {@code interfaces} array must represent interfaces, not
   * classes or primitive types.
   *
   * <li>No two elements in the {@code interfaces} array may
   * refer to identical {@code Class} objects.
   *
   * <li>All of the interface types must be visible by name through the
   * specified class loader.  In other words, for class loader
   * {@code cl} and every interface {@code i}, the following
   * expression must be true:
   * <pre>
   *     Class.forName(i.getName(), false, cl) == i
   * </pre>
   *
   * <li>All non-public interfaces must be in the same package;
   * otherwise, it would not be possible for the proxy class to
   * implement all of the interfaces, regardless of what package it is
   * defined in.
   *
   * <li>For any set of member methods of the specified interfaces
   * that have the same signature:
   * <ul>
   * <li>If the return type of any of the methods is a primitive
   * type or void, then all of the methods must have that same
   * return type.
   * <li>Otherwise, one of the methods must have a return type that
   * is assignable to all of the return types of the rest of the
   * methods.
   * </ul>
   *
   * <li>The resulting proxy class must not exceed any limits imposed
   * on classes by the virtual machine.  For example, the VM may limit
   * the number of interfaces that a class may implement to 65535; in
   * that case, the size of the {@code interfaces} array must not
   * exceed 65535.
   * </ul>
   *
   * <p>If any of these restrictions are violated,
   * {@code Proxy.getProxyClass} will throw an
   * {@code IllegalArgumentException}.  If the {@code interfaces}
   * array argument or any of its elements are {@code null}, a
   * {@code NullPointerException} will be thrown.
   *
   * <p>Note that the order of the specified proxy interfaces is
   * significant: two requests for a proxy class with the same combination
   * of interfaces but in a different order will result in two distinct
   * proxy classes.
   *
   * @param   loader the class loader to define the proxy class
   * @param   interfaces the list of interfaces for the proxy class
   *          to implement
   * @return  a proxy class that is defined in the specified class loader
   *          and that implements the specified interfaces
   * @throws  IllegalArgumentException if any of the restrictions on the
   *          parameters that may be passed to {@code getProxyClass}
   *          are violated
   * @throws  SecurityException if a security manager, <em>s</em>, is present
   *          and any of the following conditions is met:
   *          <ul>
   *             <li> the given {@code loader} is {@code null} and
   *             the caller's class loader is not {@code null} and the
   *             invocation of {@link SecurityManager#checkPermission
   *             s.checkPermission} with
   *             {@code RuntimePermission("getClassLoader")} permission
   *             denies access.</li>
   *             <li> for each proxy interface, {@code intf},
   *             the caller's class loader is not the same as or an
   *             ancestor of the class loader for {@code intf} and
   *             invocation of {@link SecurityManager#checkPackageAccess
   *             s.checkPackageAccess()} denies access to {@code intf}.</li>
   *          </ul>

   * @throws  NullPointerException if the {@code interfaces} array
   *          argument or any of its elements are {@code null}
   */
  @CallerSensitive
  public static Class<?> getProxyClass(ClassLoader loader,
                                       Class<?>... interfaces)
    throws IllegalArgumentException
  {
    final Class<?>[] intfs = interfaces.clone();
    return getProxyClass0(loader, intfs);
  }

  /*
   * Check permissions required to create a Proxy class.
   *
   * To define a proxy class, it performs the access checks as in
   * Class.forName (VM will invoke ClassLoader.checkPackageAccess):
   * 1. "getClassLoader" permission check if loader == null
   * 2. checkPackageAccess on the interfaces it implements
   *
   * To get a constructor and new instance of a proxy class, it performs
   * the package access check on the interfaces it implements
   * as in Class.getConstructor.
   *
   * If an interface is non-public, the proxy class must be defined by
   * the defining loader of the interface.  If the caller's class loader
   * is not the same as the defining loader of the interface, the VM
   * will throw IllegalAccessError when the generated proxy class is
   * being defined via the defineClass0 method.
   */
  private static void checkProxyAccess(Class<?> caller,
                                       ClassLoader loader,
                                       Class<?>... interfaces)
  {
  }

  /**
   * Generate a proxy class.  Must call the checkProxyAccess method
   * to perform permission checks before calling this.
   */
  private static Class<?> getProxyClass0(ClassLoader loader,
                                         Class<?>... interfaces) {
    if (interfaces.length > 65535) {
      throw new IllegalArgumentException("interface limit exceeded");
    }

    // If the proxy class defined by the given loader implementing
    // the given interfaces exists, this will simply return the cached copy;
    // otherwise, it will create the proxy class via the ProxyClassFactory
    return proxyClassCache.get(loader, interfaces);
  }

  /*
   * a key used for proxy class with 0 implemented interfaces
   */
  private static final Object key0 = new Object();

  /*
   * Key1 and Key2 are optimized for the common use of dynamic proxies
   * that implement 1 or 2 interfaces.
   */

  /*
   * a key used for proxy class with 1 implemented interface
   */
  private static final class Key1 extends WeakReference<Class<?>> {
    private final int hash;

    Key1(Class<?> intf) {
      super(intf);
      this.hash = intf.hashCode();
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      Class<?> intf;
      return this == obj ||
        obj != null &&
          obj.getClass() == Key1.class &&
          (intf = get()) != null &&
          intf == ((Key1) obj).get();
    }
  }

  /*
   * a key used for proxy class with 2 implemented interfaces
   */
  private static final class Key2 extends WeakReference<Class<?>> {
    private final int hash;
    private final WeakReference<Class<?>> ref2;

    Key2(Class<?> intf1, Class<?> intf2) {
      super(intf1);
      hash = 31 * intf1.hashCode() + intf2.hashCode();
      ref2 = new WeakReference<Class<?>>(intf2);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      Class<?> intf1, intf2;
      return this == obj ||
        obj != null &&
          obj.getClass() == Key2.class &&
          (intf1 = get()) != null &&
          intf1 == ((Key2) obj).get() &&
          (intf2 = ref2.get()) != null &&
          intf2 == ((Key2) obj).ref2.get();
    }
  }

  /*
   * a key used for proxy class with any number of implemented interfaces
   * (used here for 3 or more only)
   */
  private static final class KeyX {
    private final int hash;
    private final WeakReference<Class<?>>[] refs;

    @SuppressWarnings("unchecked")
    KeyX(Class<?>[] interfaces) {
      hash = Arrays.hashCode(interfaces);
      refs = (WeakReference<Class<?>>[])new WeakReference<?>[interfaces.length];
      for (int i = 0; i < interfaces.length; i++) {
        refs[i] = new WeakReference<>(interfaces[i]);
      }
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj ||
        obj != null &&
          obj.getClass() == KeyX.class &&
          equals(refs, ((KeyX) obj).refs);
    }

    private static boolean equals(WeakReference<Class<?>>[] refs1,
                                  WeakReference<Class<?>>[] refs2) {
      if (refs1.length != refs2.length) {
        return false;
      }
      for (int i = 0; i < refs1.length; i++) {
        Class<?> intf = refs1[i].get();
        if (intf == null || intf != refs2[i].get()) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * A function that maps an array of interfaces to an optimal key where
   * Class objects representing interfaces are weakly referenced.
   */
  private static final class KeyFactory
    implements BiFunction<ClassLoader, Class<?>[], Object>
  {
    @Override
    public Object apply(ClassLoader classLoader, Class<?>[] interfaces) {
      switch (interfaces.length) {
        case 1: return new Key1(interfaces[0]); // the most frequent
        case 2: return new Key2(interfaces[0], interfaces[1]);
        case 0: return key0;
        default: return new KeyX(interfaces);
      }
    }
  }

  /**
   * A factory function that generates, defines and returns the proxy class given
   * the ClassLoader and array of interfaces.
   */
  private static final class ProxyClassFactory
    implements BiFunction<ClassLoader, Class<?>[], Class<?>>
  {
    // prefix for all proxy class names
    private static final String proxyClassNamePrefix = "$ManProxy";

    // next number to use for generation of unique proxy class names
    private static final AtomicLong nextUniqueNumber = new AtomicLong();

    @Override
    public Class<?> apply(ClassLoader loader, Class<?>[] interfaces) {

      Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);
      for (Class<?> intf : interfaces) {
        /*
         * Verify that the class loader resolves the name of this
         * interface to the same Class object.
         */
        Class<?> interfaceClass = null;
        try {
          interfaceClass = Class.forName(intf.getName(), false, loader);
        } catch (ClassNotFoundException e) {
        }
        if (interfaceClass != intf) {
          throw new IllegalArgumentException(
            intf + " is not visible from class loader");
        }
        /*
         * Verify that the Class object actually represents an
         * interface.
         */
        if (!interfaceClass.isInterface()) {
          throw new IllegalArgumentException(
            interfaceClass.getName() + " is not an interface");
        }
        /*
         * Verify that this interface is not a duplicate.
         */
        if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
          throw new IllegalArgumentException(
            "repeated interface: " + interfaceClass.getName());
        }
      }

      String proxyPkg = null;     // package to define proxy class in
      int accessFlags = Modifier.PUBLIC | Modifier.FINAL;

      /*
       * Record the package of a non-public proxy interface so that the
       * proxy class will be defined in the same package.  Verify that
       * all non-public proxy interfaces are in the same package.
       */
      for (Class<?> intf : interfaces) {
        int flags = intf.getModifiers();
        if (!Modifier.isPublic(flags)) {
          accessFlags = Modifier.FINAL;
          String name = intf.getName();
          int n = name.lastIndexOf('.');
          String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
          if (proxyPkg == null) {
            proxyPkg = pkg;
          } else if (!pkg.equals(proxyPkg)) {
            throw new IllegalArgumentException(
              "non-public interfaces from different packages");
          }
        }
      }

      // if Java 9+, get the module from java.lang.reflect.Proxy$ProxyBuilder
      Object proxyModule = null;
      if( !JreUtil.isJava8() )
      {
        if( proxyPkg != null && proxyPkg.endsWith( "." ) )
        {
          proxyPkg = proxyPkg.substring( 0, proxyPkg.length()-1 );
        }

        // get a module for the proxy
        Object builder = ReflectUtil.constructor( "java.lang.reflect.Proxy$ProxyBuilder", ClassLoader.class, List.class )
          .newInstance( loader, Arrays.asList( interfaces ) );
        if( JreUtil.isJava20orLater() )
        {
          proxyModule = ReflectUtil.field( ReflectUtil.field( builder, "context" ).get(), "module" ).get();
        }
        else
        {
          proxyModule = ReflectUtil.field( builder, "module" ).get();
        }
        String moduleName = (String)ReflectUtil.method( proxyModule, "getName" ).invoke();
        Class<?> moduleClass = ReflectUtil.type( "java.lang.Module" );

        // manifold.ext.rt module creates proxy instances, allow it to read from the proxy's module and access the proxy's package
        Object manifoldExtRtModule = ReflectUtil.method( (Object)Proxy.class, "getModule" ).invoke();
        ReflectUtil.method( "jdk.internal.module.Modules", "addReads", moduleClass, moduleClass )
          .invokeStatic( proxyModule, manifoldExtRtModule );
        if( proxyPkg == null )
        {
          proxyPkg = (boolean)ReflectUtil.method( proxyModule, "isNamed" ).invoke()
            ? "com.sun.proxy" + "." + moduleName
            : "com.sun.proxy";
        }
        ReflectUtil.method( "jdk.internal.module.Modules", "addExports", moduleClass, String.class, moduleClass )
          .invokeStatic( proxyModule, proxyPkg, manifoldExtRtModule );
        ReflectUtil.method( "jdk.internal.module.Modules", "addOpens", moduleClass, String.class, moduleClass )
          .invokeStatic( proxyModule, proxyPkg, manifoldExtRtModule );

        proxyPkg += '.';
      }

      if( proxyPkg == null )
      {
        proxyPkg = "com.sun.proxy" + '.';
      }

      /*
       * Choose a name for the proxy class to generate.
       */
      long num = nextUniqueNumber.getAndIncrement();
      String proxyName = proxyPkg + proxyClassNamePrefix + num;

      /*
       * Generate the specified proxy class.
       */
      byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
        proxyName, interfaces, accessFlags);
      try {
        return defineProxyClass( proxyModule, proxyName, proxyClassFile, 0, proxyClassFile.length, loader );
      } catch (ClassFormatError e) {
        /*
         * A ClassFormatError here means that (barring bugs in the
         * proxy class generation code) there was some other
         * invalid aspect of the arguments supplied to the proxy
         * class creation (such as virtual machine limitations
         * exceeded).
         */
        throw new IllegalArgumentException(e.toString());
      }
    }
  }

  /**
   * Returns an instance of a proxy class for the specified interfaces
   * that dispatches method invocations to the specified invocation
   * handler.
   *
   * <p>{@code Proxy.newProxyInstance} throws
   * {@code IllegalArgumentException} for the same reasons that
   * {@code Proxy.getProxyClass} does.
   *
   * @param   loader the class loader to define the proxy class
   * @param   interfaces the list of interfaces for the proxy class
   *          to implement
   * @param   h the invocation handler to dispatch method invocations to
   * @return  a proxy instance with the specified invocation handler of a
   *          proxy class that is defined by the specified class loader
   *          and that implements the specified interfaces
   * @throws  IllegalArgumentException if any of the restrictions on the
   *          parameters that may be passed to {@code getProxyClass}
   *          are violated
   * @throws  SecurityException if a security manager, <em>s</em>, is present
   *          and any of the following conditions is met:
   *          <ul>
   *          <li> the given {@code loader} is {@code null} and
   *               the caller's class loader is not {@code null} and the
   *               invocation of {@link SecurityManager#checkPermission
   *               s.checkPermission} with
   *               {@code RuntimePermission("getClassLoader")} permission
   *               denies access;</li>
   *          <li> for each proxy interface, {@code intf},
   *               the caller's class loader is not the same as or an
   *               ancestor of the class loader for {@code intf} and
   *               invocation of {@link SecurityManager#checkPackageAccess
   *               s.checkPackageAccess()} denies access to {@code intf};</li>
   *          <li> any of the given proxy interfaces is non-public and the
   *               caller class is not in the same {@linkplain Package runtime package}
   *               as the non-public interface and the invocation of
   *               {@link SecurityManager#checkPermission s.checkPermission} with
   *               {@code ReflectPermission("newProxyInPackage.{package name}")}
   *               permission denies access.</li>
   *          </ul>
   * @throws  NullPointerException if the {@code interfaces} array
   *          argument or any of its elements are {@code null}, or
   *          if the invocation handler, {@code h}, is
   *          {@code null}
   */
  @CallerSensitive
  public static Object newProxyInstance(ClassLoader loader,
                                        Class<?>[] interfaces,
                                        InvocationHandler h)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(h);

    final Class<?>[] intfs = interfaces.clone();
    final SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      checkProxyAccess(Reflection.getCallerClass(), loader, intfs);
    }

    /*
     * Look up or generate the designated proxy class.
     */
    Class<?> cl = getProxyClass0(loader, intfs);

    /*
     * Invoke its constructor with the designated invocation handler.
     */
    try {
      if (sm != null) {
        checkNewProxyPermission(Reflection.getCallerClass(), cl);
      }

      final Constructor<?> cons = cl.getConstructor(constructorParams);
      final InvocationHandler ih = h;
      if (!Modifier.isPublic(cl.getModifiers())) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
          public Void run() {
            cons.setAccessible(true);
            return null;
          }
        });
      }
      return cons.newInstance(new Object[]{h});
    } catch (IllegalAccessException|InstantiationException e) {
      throw new InternalError(e.toString(), e);
    } catch ( InvocationTargetException e) {
      Throwable t = e.getCause();
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new InternalError(t.toString(), t);
      }
    } catch (NoSuchMethodException e) {
      throw new InternalError(e.toString(), e);
    }
  }

  private static void checkNewProxyPermission(Class<?> caller, Class<?> proxyClass) {
  }

  /**
   * Returns true if and only if the specified class was dynamically
   * generated to be a proxy class using the {@code getProxyClass}
   * method or the {@code newProxyInstance} method.
   *
   * <p>The reliability of this method is important for the ability
   * to use it to make security decisions, so its implementation should
   * not just test if the class in question extends {@code Proxy}.
   *
   * @param   cl the class to test
   * @return  {@code true} if the class is a proxy class and
   *          {@code false} otherwise
   * @throws  NullPointerException if {@code cl} is {@code null}
   */
  public static boolean isProxyClass(Class<?> cl) {
    return java.lang.reflect.Proxy.class.isAssignableFrom(cl) && proxyClassCache.containsValue(cl);
  }

  /**
   * Returns the invocation handler for the specified proxy instance.
   *
   * @param   proxy the proxy instance to return the invocation handler for
   * @return  the invocation handler for the proxy instance
   * @throws  IllegalArgumentException if the argument is not a
   *          proxy instance
   * @throws  SecurityException if a security manager, <em>s</em>, is present
   *          and the caller's class loader is not the same as or an
   *          ancestor of the class loader for the invocation handler
   *          and invocation of {@link SecurityManager#checkPackageAccess
   *          s.checkPackageAccess()} denies access to the invocation
   *          handler's class.
   */
  @CallerSensitive
  public static InvocationHandler getInvocationHandler(Object proxy)
    throws IllegalArgumentException
  {
    /*
     * Verify that the object is actually a proxy instance.
     */
    if (!isProxyClass(proxy.getClass())) {
      throw new IllegalArgumentException("not a proxy instance");
    }

    final Proxy p = (Proxy) proxy;
    return p.h;
  }

  public static Class<?> defineProxyClass( Object module, String name, byte[] b, int off, int len, ClassLoader loader )
  {
    if( !JreUtil.isJava8() )
    {
//      return NecessaryEvilUtil.getUnsafe().defineClass( name, b, off, len, loader, null );
      loader = (ClassLoader)ReflectUtil.method( module, "getClassLoader" ).invoke();
    }
    return (Class<?>)ReflectUtil.method( loader, "defineClass", String.class, byte[].class, int.class, int.class )
      .invoke( name, b, off, len );
  }
}
