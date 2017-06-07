package manifoldjs.plugin.legacy;

import gw.config.CommonServices;
import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.*;
import gw.util.GosuExceptionUtil;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import manifoldjs.parser.tree.*;

import javax.script.*;
import java.util.*;

public class JavascriptClassTypeInfo extends BaseTypeInfo implements ITypeInfo
{
  private final ScriptEngine _engine;
  private IConstructorInfo _constructor;
  private List<IConstructorInfo> _constructorList;
  private final MethodList _methods;
  private ProgramNode _programNode;
  private List<IPropertyInfo> _propertiesList;
  Map<String, IPropertyInfo> _propertiesMap;

  public JavascriptClassTypeInfo( JavascriptTypeBase javascriptType, ProgramNode programNode)
  {
    super( javascriptType );
    _programNode = programNode;
    ClassNode classNode = programNode.getFirstChild(ClassNode.class);
    _constructorList = new ArrayList<>();


    _methods = new MethodList();
    _propertiesList = new ArrayList<>();
    _propertiesMap = new HashMap<>();
    try {
      _engine = new ScriptEngineManager().getEngineByName("nashorn");
      _engine.eval(programNode.genCode());
      addConstructor(classNode);
      addMethods(classNode);
      addProperties(classNode);
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  private void addConstructor(ClassNode classNode) {
    ConstructorNode constructor = classNode.getFirstChild(ConstructorNode.class);
    ParameterInfoBuilder[] params = (constructor == null)?
            null:(constructor.getFirstChild(ParameterNode.class).toParamList());
    _constructor = new ConstructorInfoBuilder()
            .withParameters(params)
            .withConstructorHandler((args) -> {
                JSObject classObject =  (ScriptObjectMirror) _engine.get(classNode.getName());
                return classObject.newObject(args);
            }).build(this);
    _constructorList.add(_constructor);
  }

  private void addProperties(ClassNode classNode) {

    for (PropertyNode node : classNode.getChildren(PropertyNode.class)) {
      IPropertyInfo prop = new PropertyInfoBuilder()
              .withName(node.getName())
              .withStatic(node.isStatic())
              .withType(TypeSystem.getByFullName("dynamic.Dynamic"))
              .withAccessor(new IPropertyAccessor() {
                /*getProperty will accessor for static props only*/
                Object classObject = _engine.get(classNode.getName());
                //Use the classObject as the context for static properties
                @Override
                public Object getValue(Object o) {
                  return ((Bindings) classObject).get(node.getName());
                }
                @Override
                public void setValue(Object ctx, Object value) {
                  ((Bindings) classObject).put(node.getName(), value);
                }
              })
              .build(this);
      _propertiesMap.put(prop.getName(), prop);
      _propertiesList.add(prop);
    }
  }

  private void addMethods(ClassNode classNode) throws ScriptException {
    ScriptObjectMirror classObject = (ScriptObjectMirror) _engine.get(classNode.getName());
    JavascriptCoercer coercer = new JavascriptCoercer();
    for (ClassFunctionNode node : classNode.getChildren(ClassFunctionNode.class)) {
      if (!node.isOverride()) {
        try {
          _methods.add(new MethodInfoBuilder()
                  .withName(node.getName())
                  .withStatic(node.isStatic())
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
                      if (node.isStatic()) ctx = classObject;
                      ScriptObjectMirror context = (ScriptObjectMirror) ctx;
                      Object o = context.callMember(node.getName(), args);
                      String returnType = TypeSystem.getByRelativeName(node.getReturnType()).getName();
                      return coercer.coerceTypesJStoJava(o, returnType);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
                  .build(this));
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
    }

    //Add inherited methods if the class extends
    String packageName = _programNode.getPackageFromClassName(classNode.getSuperClass());
    if (packageName == null) return;
    IType superType = TypeSystem.getByFullName(packageName);
    if (superType == null) return;

    if (superType.isGenericType()) {
      IType[] typeParams = new IType[superType.getGenericTypeVariables().length];
      for (int i = 0; i < typeParams.length; i++) {
        typeParams[i] = TypeSystem.getByFullName("dynamic.Dynamic");
      }
      superType = superType.getParameterizedType(typeParams);
    }
    for (IMethodInfo method : superType.getTypeInfo().getMethods()) {
      _methods.add(new MethodInfoBuilder()
              .withName(method.getDisplayName())
              .withParameters(makeInheritedParamList(method))
              .withStatic(method.isStatic())
              .withReturnType(method.getReturnType())
              .withCallHandler((ctx, args) -> {
                try {
                  //Call the method on the superclass (which exists as a property object for now)
                  ScriptObjectMirror context = (ScriptObjectMirror) ctx;
                  Object superClass = context.callMember("_getSuperClass");
                  return method.getCallHandler().handleCall(superClass, args);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
              .build(this));
    }
  }



  /*Construct a parameter list for inherited parameters. If param matches a generic type variable, make type Dynamic*/
  private ParameterInfoBuilder[] makeInheritedParamList (IMethodInfo method) {
    IParameterInfo[]  params = method.getParameters();
    ParameterInfoBuilder[] parameterInfoBuilders = new ParameterInfoBuilder[params.length];

    for (int i = 0; i < params.length; i++) {
      ParameterInfoBuilder param = new ParameterInfoBuilder().like(params[i]);
      parameterInfoBuilders[i] = param;
    }
    return parameterInfoBuilders;
  }


  @Override
  public List<? extends IConstructorInfo> getConstructors() {
    return _constructorList;
  }

  @Override
  public IConstructorInfo getConstructor( IType... params ) {
    return _constructor;
  }

  @Override
  public IConstructorInfo getCallableConstructor ( IType... params ) {
    return _constructor;
  }

  @Override
  public List<? extends IPropertyInfo> getProperties() {
    return _propertiesList;
  }

  @Override
  public IPropertyInfo getProperty(CharSequence propName) {
    return _propertiesMap.get(propName.toString());
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

