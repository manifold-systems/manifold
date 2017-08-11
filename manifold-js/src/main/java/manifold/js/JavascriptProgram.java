package manifold.js;

import manifold.api.gen.*;
import manifold.api.sourceprod.SourcePosition;
import manifold.js.parser.tree.FunctionNode;
import manifold.js.parser.tree.ParameterNode;
import manifold.js.parser.tree.ProgramNode;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStream;
import java.lang.reflect.Modifier;

import static manifold.js.Util.safe;

public class JavascriptProgram {

    /* codegen */
    static SrcClass genProgram(String fqn, ProgramNode programNode) {
        SrcClass clazz = new SrcClass(fqn, SrcClass.Kind.Class).superClass(JavascriptProgram.class)
          .imports( SourcePosition.class );

        clazz.addField(new SrcField("ENGINE", ScriptEngine.class)
                .modifiers(Modifier.STATIC)
                .initializer(new SrcRawExpression(("init(\"" + fqn + "\")"))));

        clazz.addConstructor(new SrcConstructor().modifiers(Modifier.PRIVATE).body(new SrcStatementBlock()));

        for (FunctionNode node : programNode.getChildren(FunctionNode.class)) {
            AbstractSrcMethod<SrcMethod> srcMethod = new SrcMethod()
                    .name(node.getName())
                    .modifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .returns(node.getReturnType());

            // params
            ParameterNode firstChild = node.getFirstChild(ParameterNode.class);
            for (SrcParameter srcParameter : firstChild.toParamList()) {
                srcMethod.addParam(srcParameter);
            }

            //impl
            srcMethod.body(new SrcStatementBlock()
                    .addStatement(
                            new SrcRawStatement()
                                    .rawText("return invoke(ENGINE, \"" + node.getName() + "\"" + generateArgList(firstChild.toParamList()) + ");")));
            clazz.addMethod(srcMethod);

        }
        return clazz;
    }

    static String generateArgList(SrcParameter[] srcParameters) {
        StringBuilder sb = new StringBuilder();
        for (SrcParameter srcParameter : srcParameters) {
            sb.append(",");
            sb.append(srcParameter.getSimpleName());
        }
        return sb.toString();
    }

    /* implementation */
    public static Object invoke(ScriptEngine engine, String func, Object... args) {
        return safe(() -> ((Invocable) engine).invokeFunction(func, args));
    }

    public static ScriptEngine init(String programName) {
        ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
        safe(() -> nashorn.eval(loadSrcForName(programName)));
        return nashorn;
    }

    static String loadSrcForName(String name) {
        String file = "/" + name.replace(".", "/") + ".js";
        InputStream resourceAsStream = JavascriptCodeGen.class.getResourceAsStream(file);
        return Util.loadContent(resourceAsStream);
    }
}
