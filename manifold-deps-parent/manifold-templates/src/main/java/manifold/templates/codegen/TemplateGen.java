package manifold.templates.codegen;

import manifold.templates.manifold.TemplateIssue;
import manifold.templates.manifold.TemplateIssueContainer;
import manifold.templates.tokenizer.Tokenizer;
import manifold.templates.tokenizer.Token;
import manifold.internal.javac.IIssue;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static manifold.templates.codegen.TemplateGen.DirType.*;

public class TemplateGen {
    private final String BASE_CLASS_NAME = "BaseTemplate";
    private final String LAYOUT_INTERFACE = "ILayout";
    private List<TemplateIssue> _issues = new ArrayList<>();

    class ClassInfo {
        Map<Integer, ClassInfo> nestedClasses = new HashMap<>();
        String params = null;
        String[][] paramsList = null;
        String name;
        String fileName;
        String superClass = BASE_CLASS_NAME;
        int startTokenPos;
        Integer endTokenPos;
        int depth;
        boolean isLayout = false;
        boolean hasLayout = false;
        Directive layoutDir;
        int contentPos;

        //only for the outermost class
        ClassInfo(Iterator<Directive> dirIterator, String name, String fileName, Integer endTokenPos, boolean outermost) {
            assert(outermost);
            this.name = name;
            this.fileName = fileName;
            this.startTokenPos = 0;
            this.endTokenPos = endTokenPos;
            this.depth = 0;

            fillClassInfo(dirIterator);
        }

        ClassInfo(Iterator<Directive> dirIterator, String name, String fileName, String params, String[][] paramList, int startTokenPos, int depth, String superClass) {
            this.name = name;
            this.fileName = fileName;
            this.params = params;
            this.paramsList = paramList;
            this.startTokenPos = startTokenPos;
            this.depth = depth;
            this.superClass = superClass;

            fillClassInfo(dirIterator);
        }

        void fillClassInfo(Iterator<Directive> dirIterator) {
            boolean endSec = false;

            outerLoop:
            while (dirIterator.hasNext()) {
                Directive dir = dirIterator.next();
                switch (dir._dirType ) {
                    case IMPORT:
                        break;
                    case INCLUDE:
                        break;
                    case EXTENDS:
                        if (depth == 0) {
                            if (superClass.equals(BASE_CLASS_NAME)) {
                                superClass = dir.className;
                            } else {
                                addError("Invalid Extends Directive: class cannot extend 2 classes", dir.token.getLine());
                            }
                        } else {
                            addError("Invalid Extends Directive: class cannot extend within section", dir.token.getLine());
                        }

                        break;
                    case PARAMS:
                        if (depth == 0) {
                            if (params == null) {
                                params = dir.params;
                                paramsList = dir.paramsList;
                            } else {
                                addError("Invalid Params Directive: class cannot have 2 params directives", dir.token.getLine());
                            }
                        } else {
                            addError("Invalid Params Directive: class cannot have param directive within section", dir.token.getLine());
                        }
                        break;
                    case SECTION:
                        addNestedClass(new ClassInfo(dirIterator, dir.className, this.fileName, dir.params, dir.paramsList, dir.tokenPos + 1, depth + 1, superClass));
                        break;
                    case END_SECTION:
                        if (endTokenPos == null) {
                            endTokenPos = dir.tokenPos;
                        } else {
                            addError("Invalid End Section Directive: section declaration does not exist", dir.token.getLine());
                        }
                        endSec = true;
                        break outerLoop;
                    case CONTENT:
                        if (isLayout) {
                            addError("Invalid Layout Instantiation: cannot have two layout instantiations", dir.token.getLine());
                        } else if (depth > 0) {
                            addError("Invalid Layout Instantiation: cannot instantiate layout within section", dir.token.getLine());
                        } else {
                            isLayout = true;
                            contentPos = dir.tokenPos;
                        }
                        break;
                    case LAYOUT:
                        if (hasLayout) {
                            addError("Invalid Layout Declaration: cannot have two layout declarations", dir.token.getLine());
                        } else if (depth > 0) {
                            addError("Invalid Layout Declaration: cannot declare layout within section", dir.token.getLine());
                        } else {
                            hasLayout = true;
                            layoutDir = dir;
                        }
                        break;
                    case ERRANT:
                        //continue;

                }
            }
            if (!endSec) {
                if (depth == 0) {
                    assert(startTokenPos == 0);
                } else {
                    addError("Reached end of file before parsing section: " + name, 0); //TODO: Fix this to get the correct line number (of the end of the file?)
                }
            }
        }

        void addNestedClass(ClassInfo nestedClass) {
            nestedClasses.put(nestedClass.startTokenPos, nestedClass);
        }
    }

    protected enum DirType {
        IMPORT("import"),     //className
        EXTENDS("extends"),    //className
        PARAMS("params"),     //           params, paramsList
        INCLUDE("include"),    //className, params,            conditional
        SECTION("section"),    //className, params, paramsList
        END_SECTION("end"),//
        CONTENT("content"),    //
        LAYOUT("layout"),      //className
        ERRANT("#errant")      //the directive is invalid
        ;

        private String _keyword;

        DirType( String keyword )
        {
            _keyword = keyword;
        }

        public String keyword()
        {
            return _keyword;
        }
    }

    class Directive {
        int tokenPos;

        Token token;

        DirType _dirType;

        //imports "[class_name]"
        //extends "[class_name]"
        //params ([paramType paramName], [paramType paramName],...)                  <---nothing stored for params or end section
        //include "[templateName]"([paramVal], [paramVal],...) (conditional)
        //section "[sectionName]"([paramType paramName], [paramType paramName],...)
        //end section
        String className;

        //iff section, params, and include (empty string if params not given for include)
        String params;

        //iff section and params only (include doesn't need it broken down bc types aren't given)
        String[][] paramsList;

        //iff include
        String conditional;

        Directive(int tokenPos, Token token, List<Token> tokens) {
            assert (token.getType() == Token.TokenType.DIRECTIVE);
            this.tokenPos = tokenPos;
            this.token = token;

            identifyType();
            fillVars(tokens);
        }

        private void identifyType() {
            String text = token.getText().trim();
            Optional<DirType> dirType = Arrays.stream( DirType.values() ).filter( dt -> text.startsWith( dt.keyword() ) ).findFirst();
            _dirType = dirType.orElse( ERRANT );
            if( _dirType == ERRANT )
            {
                addError( "Unsupported Directive Type", token.getLine() );
            }
        }

        private void fillVars(List<Token> tokens) {
            String text = token.getText();
            text = text.trim();
            switch (_dirType) {
                case IMPORT:
                    className = text.substring(IMPORT.keyword().length()).trim();
                    break;
                case EXTENDS:
                    className = text.substring(EXTENDS.keyword().length()).trim();
                    break;
                case PARAMS:
                    String content = text.substring(PARAMS.keyword().length()).trim();
                    if( content.length() > 1 )
                    {
                        params = content.substring( 1, content.length() - 1 );
                        paramsList = splitParamsList( params );
                    }
                    break;
                case INCLUDE:
                    fillIncludeVars();
                    break;
                case SECTION:
                    String[] temp = text.substring(SECTION.keyword().length()).trim().split("\\(", 2);
                    className = temp[0].trim();
                    if (temp.length == 2 && !temp[1].equals(")")) {
                        params = temp[1].substring(0, temp[1].length() - 1).trim();
                        paramsList = splitParamsList(params);
                        findParamTypes(paramsList, tokenPos, tokens);
                        params = makeParamsString(paramsList);
                    }
                    break;
                case END_SECTION:
                    break;
                case CONTENT:
                    break;
                case LAYOUT:
                    className = text.substring(LAYOUT.keyword().length()).trim();
                    break;
                case ERRANT:
                    break;
            }
        }

        /**
         * Helper method: Given that the type of token is INCLUDE, will parse through the content of the token
         * and accordingly set className, params, and conditional. The format of an include statement is as follows:
         * <%@ include templateNameHere[(optional-params)][if(optional conditional)] %>
         * Note that in the if statement, parentheses around the conditional are optional.
         */
        private void fillIncludeVars() {
            String text = token.getText();
            text = text.trim();
            String content = text.substring(INCLUDE.keyword().length()).trim();
            int index = 0;
            while (index < content.length()) {
                if (content.charAt(index) == '(') {
                    this.className = content.substring(0, index).trim();
                    this.params = content.substring(index + 1, content.indexOf(')'));
                    fillConditional(content.substring(content.indexOf(')') + 1).trim());
                    return;
                } else if (index < content.length() - 2 && content.charAt(index) == ' ' && content.charAt(index + 1) == 'i' && content.charAt(index + 2) == 'f') {
                    this.className = content.substring(0, index).trim();
                    this.params = null;
                    fillConditional(content.substring(index + 1).trim());
                    return;
                }
                index += 1;
            }
            this.className = content;
        }

        /**
         * Helper Method: Takes in a conditional, properly parses it, and sets this.conditional accordingly.
         * @param conditional A statement as follows: if([INSERT CONDITIONAL HERE]), where parentheses are
         *                    optional.
         */
        private void fillConditional(String conditional) {
            if (conditional.length() < 2) {
                return;
            }
            String conditionalWithoutIf = conditional.substring(2);
            if (conditionalWithoutIf.charAt(0) == '(') {
                this.conditional = conditionalWithoutIf.substring(1, conditionalWithoutIf.length() - 1);
            } else {
                this.conditional = conditionalWithoutIf;
            }
        }

        /**
         * given a trimmed string of variables,
         * returns a list with a string list per variable with the type and variable name (when both are given)
         * or just the name if both aren't given
         */
        private String[][] splitParamsList(String params) {
            params = params.trim();
            params = params.replaceAll(" ,", ",").replace(", ", ",");
            String[] parameters = params.split(",");
            String[][] paramsList = new String[parameters.length][2];
            for (int i = 0; i < parameters.length; i++) {
                paramsList[i] = parameters[i].split(" ", 2);
            }
            return paramsList;
        }

        //given a list of 2 element String lists (0th elem is type and 1st elem is value), returns the string form
        //ex. [[String, str],[int,5]] returns "String str, int 5"
        private String makeParamsString(String[][] paramsList) {
            StringBuilder params = new StringBuilder().append(paramsList[0][0]).append(" ").append(paramsList[0][1]);
            for (int i = 1; i < paramsList.length; i++) {
                params.append(", ").append(paramsList[i][0]).append(" ").append(paramsList[i][1]);
            }
            return params.toString();
        }

        private void findParamTypes(String[][] params, int tokenPos, List<Token> tokens) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].length == 1) {
                    String name = params[i][0];
                    params[i] = new String[2];
                    params[i][0] = inferSingleArgumentType(name, tokenPos, tokens);
                    params[i][1] = name;
                }
            }
        }

        private String makeParamsStringWithoutTypes(String[][] paramsList) {
            StringBuilder params = new StringBuilder(paramsList[0][1]);
            for (int i = 1; i < paramsList.length; i++) {
                params.append(", ").append(paramsList[i][1]);
            }
            return params.toString();
        }

        private String inferSingleArgumentType(String name, int tokenPos, List<Token> tokens) {
            String pattern = "([a-zA-Z_$][a-zA-Z_$0-9]* " + //First Group: Matches Type arg format
                    name + ")|(\".*[a-zA-Z_$][a-zA-Z_$0-9]* " + //Second & Third Group: Deals with matching within strings
                    name + ".*\")|('.*[a-zA-Z_$][a-zA-Z_$0-9]* " +
                    name + ".*')";
            Pattern argumentRegex = Pattern.compile(pattern);
            for (int i = tokenPos - 1; i >= 0; i -= 1) {
                Token currentToken = tokens.get(i);
                if (currentToken.getType() == Token.TokenType.STMT) {
                    String text = currentToken.getText();
                    text = text.trim();
                    Matcher argumentMatcher = argumentRegex.matcher( text );
                    String toReturn = null;
                    while (argumentMatcher.find()) {
                        if (argumentMatcher.group(1) != null) {
                            toReturn = argumentMatcher.group(1);
                        }
                    }
                    if (toReturn != null) {
                        return toReturn.split(" ")[0];
                    }
                } else if (currentToken.getType() == Token.TokenType.DIRECTIVE) {
                    Directive cur = new Directive(i, currentToken, tokens);
                    if ( cur._dirType == PARAMS) {
                        String[][] outerClassParameters = cur.paramsList;
                        for(String[] currentParams: outerClassParameters) {
                            String parameter = currentParams[1];
                            if (name.equals(parameter)) {
                                return currentParams[0];
                            }
                        }
                    }
                }
            }
            addError("Type for argument can not be inferred: " + name, token.getLine());
            return "";
        }

    }

    class FileGenerator {
        private TemplateStringBuilder sb = new TemplateStringBuilder();
        private ClassInfo currClass;
        private List<Token> tokens;
        private Map<Integer, Directive> dirMap;

        private class TemplateStringBuilder {
            private final String INDENT = "    ";
            private StringBuilder sb = new StringBuilder();

            TemplateStringBuilder newLine(String content) {
                sb.append("\n");
                for (int i = 0; i < currClass.depth; i++) {
                    sb.append(INDENT);
                }
                sb.append(content);
                return this;
            }

            TemplateStringBuilder append(String content) {
                sb.append(content);
                return this;
            }

            public String toString() {
                return sb.toString();
            }
        }


        FileGenerator(String fullyQualifiedName, String fileName, String source) {
            String[] parts = fullyQualifiedName.split("\\.");
            String className = parts[parts.length - 1];
            StringBuilder packageName = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                packageName.append(".").append(parts[i]);
            }

            Tokenizer tokenizer = new Tokenizer();
            this.tokens = tokenizer.tokenize(source);

            List<Directive> dirList = getDirectivesList(tokens);
            this.dirMap = getDirectivesMap(dirList);
            this.currClass = new ClassInfo(dirList.iterator(), className, fileName, tokens.size() - 1, true);

            buildFile(packageName.toString(), dirList);
        }

        String getFileContents() {
            return sb.toString();
        }

        private void buildFile(String packageName, List<Directive> dirList) {
            sb.append("package ").append(packageName + ";\n")
                    .newLine("import java.io.IOException;")
                    .newLine("import manifold.templates.ManifoldTemplates;")
                    .newLine("import manifold.templates.runtime.*;\n");
            addImports(dirList);
            makeClassContent();
        }

        private boolean containsStringContentOrExpr(List<Token> tokens, Integer start, Integer end) {
            if (end == null) {
                end = tokens.size() - 1;
            }
            for (int i = start; i <= end; i++) {
                Token token = tokens.get(i);
                Token.TokenType tokenType = token.getType();
                if (tokenType == Token.TokenType.CONTENT || tokenType == Token.TokenType.EXPR) {
                    return true;
                }
            }
            return false;
        }

        private void addRenderImpl() {
            sb.newLine("    public void renderImpl(Appendable buffer, ILayout overrideLayout").append(safeTrailingString(currClass.params)).append(") {");

            boolean needsToCatchIO = currClass.depth == 0;

            if (!needsToCatchIO) {
                needsToCatchIO = containsStringContentOrExpr(tokens, currClass.startTokenPos - 1, currClass.endTokenPos);
            }

            if (needsToCatchIO) {
                sb.newLine("        try {");
            }

            if (currClass.isLayout) {
                sb.newLine("            INSTANCE.header(buffer);")
                  .newLine("            INSTANCE.footer(buffer);");
            } else {
                String isOuterTemplate = String.valueOf(currClass.depth == 0);
                sb.newLine("            beforeRender(buffer, overrideLayout, ").append(isOuterTemplate).append(");\n");
                sb.newLine("            long startTime = System.nanoTime();\n");
                makeFuncContent(currClass.startTokenPos, currClass.endTokenPos);
                sb.newLine("            long endTime = System.nanoTime();\n");
                sb.newLine("            long duration = (endTime - startTime)/1000000;\n");
                sb.newLine("            afterRender(buffer, overrideLayout, ").append(isOuterTemplate).append(", duration);\n");
            }

            if (needsToCatchIO) {
                sb.newLine("        } catch (IOException e) {\n")
                  .newLine("            throw new RuntimeException(e);\n")
                  .newLine("        }\n");
            }

            sb.newLine("    }\n\n");
        }

        private void addRender() {
            sb.newLine("")
              .newLine("    public static String render(").append(safeString(currClass.params)).append(") {")
              .newLine("      StringBuilder sb = new StringBuilder();")
              .newLine("      renderInto(sb");
            for (String[] param : safeParamsList()) {
                sb.append(", ").append(param[1]);
            }
            sb.append(");")
              .newLine("        return sb.toString();")
              .newLine("    }\n");
        }

        private void addRenderInto() {
            sb.newLine("    public static void renderInto(Appendable buffer").append(safeTrailingString(currClass.params)).append(") {\n")
              .newLine("      INSTANCE.renderImpl(buffer, null");
            for (String[] param : safeParamsList()) {
                sb.append(", ").append(param[1]);
            }
            sb.append(");\n")
              .newLine("    }\n\n");
        }

        private void addWithoutLayout() {
            sb.newLine("    public static LayoutOverride withoutLayout() {")
              .newLine("        return withLayout(ILayout.EMPTY);")
              .newLine("    }\n\n");
        }


        private void addWithLayout() {
            sb.newLine("    public static LayoutOverride withLayout(ILayout layout) {")
              .newLine("        return new LayoutOverride(layout);")
              .newLine("    }\n\n");
        }


        private void addLayoutOverrideClass() {
            sb.newLine("    public static class LayoutOverride extends BaseLayoutOverride {")
              // constructor
              .newLine("       public LayoutOverride(ILayout override) {")
              .newLine("         super(override);")
              .newLine("       }\n")
              .newLine("")
              // render
              .newLine("    public String render(").append(safeString(currClass.params)).append(") {")
              .newLine("      StringBuilder sb = new StringBuilder();")
              .newLine("      INSTANCE.renderImpl(sb, getOverride()");
            for (String[] param : safeParamsList()) {
                sb.append(", ").append(param[1]);
            }
            sb.append(");")
              .newLine("        return sb.toString();")
              .newLine("    }\n")
              // renderInto
              .newLine("    public void renderInto(Appendable sb").append(safeTrailingString(currClass.params)).append(") {")
              .newLine("      INSTANCE.renderImpl(sb, getOverride()");
            for (String[] param : safeParamsList()) {
                sb.append(", ").append(param[1]);
            }
            sb.append(");")
              .newLine("    }\n")
              // close class
              .newLine("    }\n");


        }

      private String safeTrailingString(String string) {
        if (string != null && string.length() > 0) {
            return ", " + string;
        } else {
            return "";
        }
      }

      private String safeString(String string) {
        if (string != null && string.length() > 0) {
            return string;
        } else {
            return "";
        }
      }

      private String[][] safeParamsList() {
            String[][] paramsList = currClass.paramsList;
            if (paramsList == null) {
                paramsList = new String[0][0];
            }
            return paramsList;
        }

        private void addFileHeader() {
            sb.newLine("\n");
            if (currClass.depth == 0) {
                if (currClass.isLayout) {
                    sb.newLine("public class ").append(currClass.name).append(" extends ").append(currClass.superClass).append(" implements ").append(LAYOUT_INTERFACE).append(" {");
                } else {
                    sb.newLine("public class ").append(currClass.name).append(" extends ").append(currClass.superClass).append(" {");
                }
            } else {
                sb.newLine("public static class ").append(currClass.name).append(" extends ").append(currClass.superClass).append(" {");
            }
            sb.newLine("    private static ").append(currClass.name).append(" INSTANCE = new ").append(currClass.name).append("();");
            sb.newLine("    private ").append(currClass.name).append("(){");
            if (currClass.hasLayout) {
                sb.newLine("        setLayout(").append(currClass.layoutDir.className).append(".asLayout());");
            }
            sb.newLine("    }\n");
        }


        private void makeClassContent() {
            addFileHeader();
            addRender();
            addLayoutOverrideClass();
            addWithoutLayout();
            addWithLayout();
            addRenderInto();
            addRenderImpl();

            if (currClass.isLayout) {
                addHeaderAndFooter();
            }
            for (ClassInfo nested : currClass.nestedClasses.values()) {
                currClass = nested;
                makeClassContent();
            }
            //close class
            sb.newLine("}\n");
        }

        private void addHeaderAndFooter() {
            sb.newLine("    public static ").append(LAYOUT_INTERFACE).append(" asLayout() {")
                    .newLine("        return INSTANCE;")
                    .newLine("    }\n")
                    .newLine("    @Override")
                    .newLine("    public void header(Appendable buffer) throws IOException {")
                    .newLine("        if (getExplicitLayout() != null) {")
                    .newLine("            getExplicitLayout().header(buffer);")
                    .newLine("        }");
            assert(currClass.depth == 0);
            makeFuncContent(currClass.startTokenPos, currClass.contentPos);
            sb.newLine("    }")
                    .newLine("    @Override")
                    .newLine("    public void footer(Appendable buffer) throws IOException {");
            makeFuncContent(currClass.contentPos, currClass.endTokenPos);
            sb.newLine("        if (getExplicitLayout() != null) {")
                    .newLine("            getExplicitLayout().footer(buffer);")
                    .newLine("    }\n}");
        }

        private List<Directive> getDirectivesList(List<Token> tokens) {
            ArrayList<Directive> dirList = new ArrayList<>();

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.getType() == Token.TokenType.DIRECTIVE) {
                    dirList.add(new Directive(i, token, tokens));
                }
            }
            return dirList;
        }

        private Map<Integer, Directive> getDirectivesMap(List<Directive> dirList) {
            Map<Integer, Directive> dirMap = new HashMap<>();
            for (Directive dir : dirList) {
                dirMap.put(dir.tokenPos, dir);
            }
            return dirMap;
        }

        private void addImports(List<Directive> dirList) {
            for (Directive dir: dirList) {
                if (dir._dirType == IMPORT) {
                    sb.newLine("import " + dir.className + ";");
                }
            }
        }

        private void makeFuncContent(Integer startPos, Integer endPos) {
            ArrayList<Integer> bbLineNumbers = new ArrayList<>();
            if (endPos == null) {
                endPos = tokens.size() - 1;
            }
            sb.newLine("            int lineStart = Thread.currentThread().getStackTrace()[1].getLineNumber() + 1;");
            sb.newLine("            try {");
            Token.TokenType lastTokenType = null;
            outerLoop:
            for (int i = startPos; i <= endPos; i++) {
                Token token = tokens.get(i);
                switch (token.getType()) {
                    case CONTENT:
                        String text = makeText( lastTokenType, token.getText() );
                        if( text.length() > 0 )
                        {sb.newLine("                buffer.append(\"").append(text.replaceAll("\"", "\\\\\"").replaceAll("\r", "").replaceAll("\n", "\\\\n")+ "\");");
                        bbLineNumbers.add(token.getLine());}
                        break;
                    case STMT:
                        String[] statementList = token.getText().split("\n");
                        for (int j = 0; j < statementList.length; j++) {
                            String statement = statementList[j].trim().replaceAll("\r", "");
                            sb.append("                ").append(statement).append("\n");
                            bbLineNumbers.add(token.getLine() + j);
                        }
                        break;
                    case EXPR:
                        sb.newLine("                buffer.append(toS(").append(token.getText()).append("));");
                        bbLineNumbers.add(token.getLine());
                        break;
                    case COMMENT:
                        break;
                    case DIRECTIVE:
                        Directive dir = dirMap.get(i);
                        if (dir._dirType == SECTION) {
                            ClassInfo classToSkipOver = currClass.nestedClasses.get(i + 1);
                            if (classToSkipOver.endTokenPos == null) {
                                i = endPos;
                            } else {
                                i = classToSkipOver.endTokenPos;
                            }
                            addSection(dir);
                        } else if (dir._dirType == END_SECTION) {
                            break outerLoop;
                        } else if (dir._dirType == INCLUDE) {
                            addInclude(dir);
                        } else if (dir._dirType == CONTENT) {
                            break;
                        }
                        break;
                    default:
                        continue;
                }
                lastTokenType = token.getType();
            }
            String nums = bbLineNumbers.toString().substring(1, bbLineNumbers.toString().length() - 1);

            sb.newLine("            } catch (RuntimeException e) {");
            sb.newLine("                int[] bbLineNumbers = new int[]{").append(nums).append("};");
            sb.newLine("                handleException(e, \"").append(this.currClass.fileName).append("\", lineStart, bbLineNumbers);\n            }");
        }

        private String makeText( Token.TokenType lastTokenType, String text )
        {
            if( text != null &&
                lastTokenType != Token.TokenType.CONTENT &&
                lastTokenType != Token.TokenType.EXPR )
            {
                if( text.length() > 0 )
                {
                    if( text.charAt( 0 ) == '\n' )
                    {
                        text = text.substring( 1 );
                    }
                    else if( text.length() > 1 && text.charAt( 0 ) == '\r' && text.charAt( 1 ) == '\n' )
                    {
                        text = text.substring( 2 );
                    }
                }
            }
            return text;
        }


      private void addInclude(Directive dir) {
        assert (dir._dirType == INCLUDE);
        if (dir.conditional != null) {
          sb.newLine("            if(").append(dir.conditional).append("){");
        }
        sb.newLine("            ").append(dir.className).append(".withoutLayout().renderInto(buffer").append(safeTrailingString(dir.params)).append(");");
        if (dir.conditional != null) {
          sb.newLine("            ").append("}");
        }
      }

        private void addSection(Directive dir) {
            assert(dir._dirType == SECTION);
            if (dir.params != null) {
                String paramsWithoutTypes = dir.makeParamsStringWithoutTypes(dir.paramsList);
                sb.newLine("            ").append(dir.className).append(".renderInto(buffer, ").append(paramsWithoutTypes).append(");");
            } else {
                sb.newLine("            ").append(dir.className).append(".renderInto(buffer);");
            }
        }

    }

    public String generateCode(String fullyQualifiedName, String source, String fileName) {
        FileGenerator generator = new FileGenerator(fullyQualifiedName, fileName, source);
        return generator.getFileContents();
    }

    private void addError(String message, int line) {
        TemplateIssue error = new TemplateIssue(IIssue.Kind.Error, 0, line, 0, message);
        _issues.add( error );
    }

    public TemplateIssueContainer getIssues() {
        return new TemplateIssueContainer( _issues );
    }
}