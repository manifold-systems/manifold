package manifoldjs.plugin.legacy;

import gw.config.CommonServices;
import gw.lang.reflect.BaseTypeInfo;
import gw.lang.reflect.IExpando;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeInfo;
import gw.lang.reflect.MethodInfoBuilder;
import gw.lang.reflect.MethodList;
import gw.lang.reflect.TypeSystem;
import gw.util.GosuExceptionUtil;
import manifoldjs.parser.tree.FunctionNode;
import manifoldjs.parser.tree.ParameterNode;
import manifoldjs.parser.tree.ProgramNode;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class JavascriptProgramTypeInfo extends BaseTypeInfo implements ITypeInfo
{
  private final ScriptEngine _engine;
  private final MethodList _methods;

  public JavascriptProgramTypeInfo(JavascriptProgramType javascriptType, ProgramNode programNode)
  {
    super( javascriptType );
    String source = programNode.genCode();
    try
    {
      // init runtime
      // TODO cgross - make lazy
      _engine = new ScriptEngineManager().getEngineByName("nashorn");
      _engine.eval( source );
      _methods = new MethodList();
      addMethods(programNode);
    }
    catch( ScriptException e )
    {
      throw new RuntimeException( e );
    }
  }

  public void addMethods(ProgramNode programNode) throws ScriptException {
    JavascriptCoercer coercer = new JavascriptCoercer();
    for (FunctionNode node : programNode.getChildren(FunctionNode.class)) {
      try {
        _methods.add(new MethodInfoBuilder()
                .withName(node.getName())
                .withStatic()
                .withParameters((node.getFirstChild(ParameterNode.class)).toParamList())
                .withReturnType(TypeSystem.getByRelativeName(node.getReturnType()))
                .withCallHandler((ctx, args) -> {
                  for(int i = 0 ; i < args.length; i ++) {
                    String paramType = node.getFirstChild(ParameterNode.class).getTypes().get(i);
                    if(!paramType.equals("dynamic.Dynamic")) {
                      args [i] = coercer.coerceTypesJavatoJS(args[i], paramType);
                    }
                  }
                  try {
                    Object o = ((Invocable) _engine).invokeFunction(node.getName(), args);
                    String returnType = TypeSystem.getByRelativeName(node.getReturnType()).getName();
                    if (o == null) return  null;
                    return coercer.coerceTypesJStoJava(o, returnType);
                  }
                  catch (Exception e) {
                    throw new RuntimeException( e );
                  }
                })
                .build(this));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException( e );
      }
    }
  }

//  private static Object maybeExpand( Object o )
//  {
//    if( o instanceof ScriptObjectMirror )
//    {
//      return new ExpandWrapper( (ScriptObjectMirror)o );
//    }
//    else
//    {
//      return o;
//    }
//  }


  @Override
  public MethodList getMethods()
  {
    return _methods;
  }

  @Override
  public IMethodInfo getCallableMethod(CharSequence strMethod, IType... params) {
    return FIND.callableMethod( getMethods(), strMethod, params );
  }

  @Override
  public IMethodInfo getMethod( CharSequence methodName, IType... params )
  {
    return FIND.method(getMethods(), methodName, params);
  }

//  private static class ExpandWrapper implements IExpando
//  {
//    private final ScriptObjectMirror _mirror;
//
//    public ExpandWrapper( ScriptObjectMirror o )
//    {
//      _mirror = o;
//    }
//
//    @Override
//    public Object getFieldValue( String field )
//    {
//      return _mirror.getMember( field );
//    }
//
//    @Override
//    public void setFieldValue( String field, Object value )
//    {
//      _mirror.setMember( field, value );
//    }
//
//    @Override
//    public void setDefaultFieldValue( String field )
//    {
//      // ignore
//    }
//
//    @Override
//    public Object invoke( String methodName, Object... args )
//    {
//      return maybeExpand(_mirror.callMember( methodName, args ));
//    }
//
//    @Override
//    public Map getMap()
//    {
//      HashMap map = new HashMap();
//      for( String ownKey : _mirror.getOwnKeys( false ) )
//      {
//        map.put( ownKey, _mirror.getMember( ownKey ) );
//      }
//      return map;
//    }
//  }
}