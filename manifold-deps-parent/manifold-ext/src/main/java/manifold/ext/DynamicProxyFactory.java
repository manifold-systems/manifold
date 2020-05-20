/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Constructor;
import java.util.Map;
import javax.lang.model.type.NoType;
import manifold.ext.rt.api.ICallHandler;
import manifold.ext.rt.api.IDynamicProxyFactory;
import manifold.ext.rt.api.IProxyFactory;
import manifold.internal.host.RuntimeManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
import manifold.util.ManExceptionUtil;
import manifold.rt.api.util.Pair;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.ConcurrentWeakHashMap;

public class DynamicProxyFactory implements IDynamicProxyFactory
{
  private static final String STRUCTURAL_PROXY = "_structuralproxy_";
  private static final Map<Class, Boolean> ICALL_HANDLER_MAP = new ConcurrentWeakHashMap<>();

  @Override
  public IProxyFactory makeProxyFactory( Class iface, Class rootClass )
  {
    String relativeProxyName = rootClass.getCanonicalName().replace( '.', '_' ) + STRUCTURAL_PROXY + iface.getCanonicalName().replace( '.', '_' );
    Class proxyClass;
    if( hasCallHandlerMethod( rootClass ) )
    {
      proxyClass = DynamicTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
    }
    else
    {
      proxyClass = StructuralTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
    }
    Constructor constructor = proxyClass.getConstructors()[0];
    ReflectUtil.setAccessible( constructor );
    return new Factory( constructor );
  }

  public static class Factory implements IProxyFactory
  {
    private final Constructor _constructor;

    public Factory( Constructor constructor )
    {
      _constructor = constructor;
    }

    @Override
    public Object proxy( Object target, Class iface )
    {
      try
      {
        return _constructor.newInstance( target );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
  }

  private static boolean hasCallHandlerMethod( Class rootClass )
  {
    if( ICallHandler.class.isAssignableFrom( rootClass ) )
    {
      // Nominally implements ICallHandler
      return true;
    }
    if( ReflectUtil.method( rootClass, "call", Class.class, String.class, String.class, Class.class, Class[].class, Object[].class ) != null )
    {
      // Structurally implements ICallHandler
      return true;
    }

    // maybe has an extension satisfying ICallHandler
    return hasCallHandlerFromExtension( rootClass );
  }

  private static boolean hasCallHandlerFromExtension( Class rootClass )
  {
    Boolean isCallHandler = ICALL_HANDLER_MAP.get( rootClass );
    if( isCallHandler != null )
    {
      return isCallHandler;
    }

    String fqn = rootClass.getCanonicalName();
    BasicJavacTask javacTask = RuntimeManifoldHost.get().getJavaParser().getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( RuntimeManifoldHost.get().getSingleModule() ).getClassSymbol( javacTask, fqn );
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> callHandlerSymbol = ClassSymbols.instance( RuntimeManifoldHost.get().getSingleModule() ).getClassSymbol( javacTask, ICallHandler.class.getCanonicalName() );
    if( Types.instance( javacTask.getContext() ).isAssignable( classSymbol.getFirst().asType(), callHandlerSymbol.getFirst().asType() ) )
    {
      // Nominally implements ICallHandler
      isCallHandler = true;
    }
    else
    {
      // Structurally implements ICallHandler
      isCallHandler = hasCallMethod( javacTask, classSymbol.getFirst() );
    }
    ICALL_HANDLER_MAP.put( rootClass, isCallHandler );
    return isCallHandler;
  }

  private static boolean hasCallMethod( BasicJavacTask javacTask, Symbol.ClassSymbol classSymbol )
  {
    Name call = Names.instance( javacTask.getContext() ).fromString( "call" );
    Iterable<Symbol> elems = IDynamicJdk.instance().getMembersByName( classSymbol, call );
    for( Symbol s : elems )
    {
      if( s instanceof Symbol.MethodSymbol )
      {
        List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)s).getParameters();
        if( parameters.size() != 6 )
        {
          return false;
        }

        Symtab symbols = Symtab.instance( javacTask.getContext() );
        Types types = Types.instance( javacTask.getContext() );
        return types.erasure( parameters.get( 0 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 1 ).asType().equals( symbols.stringType ) &&
               parameters.get( 2 ).asType().equals( symbols.stringType ) &&
               types.erasure( parameters.get( 3 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 4 ).asType() instanceof Type.ArrayType && types.erasure( ((Type.ArrayType)parameters.get( 4 ).asType()).getComponentType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 5 ).asType() instanceof Type.ArrayType && ((Type.ArrayType)parameters.get( 5 ).asType()).getComponentType().equals( symbols.objectType );
      }
    }
    Type superclass = classSymbol.getSuperclass();
    if( !(superclass instanceof NoType) )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)superclass.tsym ) )
      {
        return true;
      }
    }
    for( Type iface : classSymbol.getInterfaces() )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)iface.tsym ) )
      {
        return true;
      }
    }
    return false;
  }
}
