package manifoldjs.plugin.legacy;

import gw.lang.reflect.*;
import gw.util.GosuExceptionUtil;
import manifoldjs.parser.tree.ParameterNode;
import manifoldjs.parser.tree.template.RawStringNode;
import manifoldjs.parser.tree.template.JSTNode;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JavascriptTemplateTypeInfo extends BaseTypeInfo implements ITypeInfo
{
  private final ScriptEngine _engine;
  private final MethodList _methods;
  private final JSTNode _templateNode;

  public JavascriptTemplateTypeInfo(JavascriptTemplateType javascriptType, JSTNode templateNode)
  {
    super( javascriptType );
    _templateNode = templateNode;
    _engine = new ScriptEngineManager().getEngineByName("nashorn");
    try {
      _engine.eval(templateNode.genCode());
    } catch (ScriptException e) {
      throw new RuntimeException( e );
    }
    JavascriptCoercer coercer = new JavascriptCoercer();

    _methods = new MethodList();
    //Only one method to render template to string
    _methods.add(new MethodInfoBuilder()
            .withName("renderToString")
            .withReturnType(TypeSystem.getByFullName("java.lang.String"))
            .withStatic()
            .withParameters(templateNode.getFirstChild(ParameterNode.class).toParamList())
            .withCallHandler( ( ctx, args ) -> {
              for(int i = 0 ; i < args.length; i ++) {
                String paramType = templateNode.getFirstChild(ParameterNode.class).getTypes().get(i);
                if(!paramType.equals("dynamic.Dynamic")) {
                  args [i] = coercer.coerceTypesJavatoJS(args[i], paramType);
                }
              }
                return renderToString(args);
              })
            .build( this ) );
  }

  //Calls the generated renderToString function with raw strings from template
  private String renderToString(Object ...args) {
    try {
      //make argument list including the raw string list
      Object[] argsWithStrings = Arrays.copyOf(args, args.length + 1);
      List rawStrings =  _templateNode.getChildren(RawStringNode.class)
              .stream()
              .map(node -> node.genCode())
              .collect(Collectors.toList());
      argsWithStrings[argsWithStrings.length-1] = rawStrings;
      String  ret = (String) ((Invocable)_engine).invokeFunction( "renderToString", argsWithStrings);
      return ret;
    } catch (Exception e) {
      throw new RuntimeException( e );
    }
  }

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
}