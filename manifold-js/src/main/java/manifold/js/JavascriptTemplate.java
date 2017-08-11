package manifold.js;

import manifold.api.gen.*;
import manifold.js.parser.TemplateParser;
import manifold.js.parser.TemplateTokenizer;
import manifold.js.parser.tree.ParameterNode;
import manifold.js.parser.tree.template.JSTNode;
import manifold.js.parser.tree.template.RawStringNode;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static manifold.js.JavascriptProgram.generateArgList;
import static manifold.js.Util.safe;

public class JavascriptTemplate {

  public static SrcClass genClass(String fqn, JSTNode jstNode) {
    SrcClass clazz = new SrcClass(fqn, SrcClass.Kind.Class);

    clazz.addField(new SrcField("TEMPLATE_NODE", JSTNode.class)
      .modifiers(Modifier.STATIC)
      .initializer(new SrcRawExpression(("manifold.js.JavascriptTemplate.initNode(\"" + fqn + "\")"))));

    clazz.addField(new SrcField("ENGINE", ScriptEngine.class)
      .modifiers(Modifier.STATIC)
      .initializer(new SrcRawExpression(("manifold.js.JavascriptTemplate.initEngine(TEMPLATE_NODE)"))));

    SrcParameter[] srcParameters = jstNode.getFirstChild(ParameterNode.class).toParamList();

    AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
      .name("renderToString")
      .modifiers(Modifier.PUBLIC | Modifier.STATIC)
      .returns(String.class);

    // params
    for (SrcParameter srcParameter : srcParameters) {
      srcMethod.addParam(srcParameter);
    }

    //impl
    srcMethod.body(new SrcStatementBlock()
      .addStatement(
        new SrcRawStatement()
          .rawText("return manifold.js.JavascriptTemplate.renderToStringImpl(ENGINE, TEMPLATE_NODE" + generateArgList(srcParameters) + ");")));
    clazz.addMethod(srcMethod);

    return clazz;
  }

  //Calls the generated renderToString function with raw strings from template
  public static String renderToStringImpl(ScriptEngine engine, JSTNode templateNode, Object... args) {
    try {
      //make argument list including the raw string list
      Object[] argsWithStrings = Arrays.copyOf(args, args.length + 1);

      List rawStrings = templateNode.getChildren(RawStringNode.class)
        .stream()
        .map(node -> node.genCode())
        .collect(Collectors.toList());

      argsWithStrings[argsWithStrings.length - 1] = rawStrings;

      String ret = (String) ((Invocable) engine).invokeFunction("renderToString", argsWithStrings);
      return ret;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ScriptEngine initEngine(JSTNode templateNode) {
    ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
    safe(() -> nashorn.eval(templateNode.genCode()));
    return nashorn;
  }

  public static JSTNode initNode(String programName) {
    TemplateParser parser = new TemplateParser(new TemplateTokenizer(loadJSTForName(programName), true));
    return (JSTNode) parser.parse();
  }

  public static String loadJSTForName(String name) {
    String file = "/" + name.replace(".", "/") + ".jst";
    InputStream resourceAsStream = JavascriptCodeGen.class.getResourceAsStream(file);
    return Util.loadContent(resourceAsStream);
  }

}
