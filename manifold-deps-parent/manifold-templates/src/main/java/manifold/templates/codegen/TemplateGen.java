package manifold.templates.codegen;

import manifold.templates.manifold.TemplateIssue;
import manifold.templates.manifold.TemplateIssueContainer;
import manifold.templates.tokenizer.Tokenizer;
import manifold.templates.tokenizer.Token;
import manifold.internal.javac.IIssue;
import manifold.api.templ.DisableStringLiteralTemplates;

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
                switch (dir.dirType) {
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
        IMPORT,     //className
        EXTENDS,    //className
        PARAMS,     //           params, paramsList
        INCLUDE,    //className, params,            conditional
        SECTION,    //className, params, paramsList
        END_SECTION,//
        CONTENT,    //
        LAYOUT,      //className
        ERRANT      //the directive is invalid
    }

    class Directive {
        int tokenPos;

        Token token;

        DirType dirType;

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
            String content = token.getContent();

            if (content.matches("import.*")) {
                dirType = IMPORT;
            } else if (content.matches("extends.*")) {
                dirType = EXTENDS;
            } else if (content.matches("params.*")) {
                dirType = PARAMS;
            } else if (content.matches("include.*")) {
                dirType = INCLUDE;
            } else if (content.matches("section.*")) {
                dirType = SECTION;
            } else if (content.trim().matches("end section")) {
                dirType = END_SECTION;
            } else if (content.trim().matches("content")) {
                dirType = CONTENT;
            } else if (content.trim().matches("layout.*")) {
                dirType = LAYOUT;
            } else {
                addError("Unsupported Directive Type", token.getLine());
                dirType = ERRANT;
            }
        }

        private void fillVars(List<Token> tokens) {
            switch (dirType) {
                case IMPORT:
                    className = token.getContent().substring(6).trim();
                    break;
                case EXTENDS:
                    className = token.getContent().substring(7).trim();
                    break;
                case PARAMS:
                    String content = token.getContent().substring(6).trim();
                    params = content.substring(1, content.length() - 1);
                    paramsList = splitParamsList(params);
                    break;
                case INCLUDE:
                    fillIncludeVars();
                    break;
                case SECTION:
                    String[] temp = token.getContent().substring(7).trim().split("\\(", 2);
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
                    className = token.getContent().substring(6).trim();
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
            String content = token.getContent().substring(8).trim();
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
                if (currentToken.getType() == Token.TokenType.STATEMENT) {
                    Matcher argumentMatcher = argumentRegex.matcher(currentToken.getContent());
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
                    if (cur.dirType == PARAMS) {
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

            TemplateStringBuilder append(String content) {
                for (int i = 0; i < currClass.depth; i++) {
                    sb.append(INDENT);
                }
                sb.append(content);
                return this;
            }

            TemplateStringBuilder reAppend(String content) {
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
            _issues.addAll(tokenizer.getIssues());

            List<Directive> dirList = getDirectivesList(tokens);
            this.dirMap = getDirectivesMap(dirList);
            this.currClass = new ClassInfo(dirList.iterator(), className, fileName, tokens.size() - 1, true);

            buildFile(packageName.toString(), dirList);
        }

        String getFileContents() {
            return sb.toString();
        }

        private void buildFile(String packageName, List<Directive> dirList) {
            sb.append("package ").reAppend(packageName + ";\n\n")
                    .append("import java.io.IOException;\n")
                    .append("import manifold.templates.ManifoldTemplates;\n")
                    .append("import manifold.templates.runtime.*;\n\n");
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
                if (tokenType == Token.TokenType.STRING_CONTENT || tokenType == Token.TokenType.EXPRESSION) {
                    return true;
                }
            }
            return false;
        }

        private void addRenderImpl() {

            if (currClass.paramsList == null) {
                sb.append("    public void renderImpl(Appendable buffer, ILayout overrideLayout) {\n");
            } else {
                sb.append("    public void renderImpl(Appendable buffer, ILayout overrideLayout, ").reAppend(currClass.params).reAppend(") {\n");
            }

            boolean needsToCatchIO = currClass.depth == 0;

            if (!needsToCatchIO) {
                needsToCatchIO = containsStringContentOrExpr(tokens, currClass.startTokenPos - 1, currClass.endTokenPos);
            }

            if (needsToCatchIO) {
                sb.append("        try {\n");
            }

            if (currClass.isLayout) {
                sb.append("            INSTANCE.header(buffer);\n")
                        .append("            INSTANCE.footer(buffer);\n");
            } else {
                sb.append("            beforeRender(buffer, overrideLayout, ").reAppend(String.valueOf(currClass.depth == 0)).reAppend(");\n");

                sb.append("            long startTime = System.nanoTime();\n");

                makeFuncContent(currClass.startTokenPos, currClass.endTokenPos);

                sb.append("            long endTime = System.nanoTime();\n");
                sb.append("            long duration = (endTime - startTime)/1000000;\n");

                sb.append("            afterRender(buffer, overrideLayout, ").reAppend(String.valueOf(currClass.depth == 0)).reAppend(", duration);\n");

            }

            if (needsToCatchIO) {
                sb.append("        } catch (IOException e) {\n")
                        .append("            throw new RuntimeException(e);\n")
                        .append("        }\n");
            }

            //close the renderImpl
            sb.append("    }\n\n");
        }

        private void addRender() {
            if (currClass.paramsList == null) {
                //without layout
                sb.append("\n")
                        .append("    public static String render() {\n")
                        .append("        StringBuilder sb = new StringBuilder();\n")
                        .append("        renderInto(sb);\n")
                        .append("        return sb.toString();\n")
                        .append("    }\n\n");
                //with layout
                sb.append("\n")
                        .append("    public static String render(ILayout overrideLayout) {\n")
                        .append("        StringBuilder sb = new StringBuilder();\n")
                        .append("        renderInto(sb, overrideLayout);\n")
                        .append("        return sb.toString();\n")
                        .append("    }\n\n");
            } else {
                //without layout
                sb.append("\n")
                        .append("    public static String render(").reAppend(currClass.params + ") {\n")
                        .append("        StringBuilder sb = new StringBuilder();\n")
                        .append("        renderInto(sb");
                for (String[] p : currClass.paramsList) {
                    sb.reAppend(", ").reAppend(p[1]);
                }
                sb.reAppend(");\n")
                        .append("        return sb.toString();\n")
                        .append("    }\n\n");
                //with Layout
                sb.append("\n")
                        .append("    public static String render(ILayout overrideLayout, ").reAppend(currClass.params + ") {\n")
                        .append("        StringBuilder sb = new StringBuilder();\n")
                        .append("        renderInto(sb, overrideLayout");
                for (String[] p : currClass.paramsList) {
                    sb.reAppend(", ").reAppend(p[1]);
                }
                sb.reAppend(");\n")
                        .append("        return sb.toString();\n")
                        .append("    }\n\n");
            }
        }

        private void addFileHeader() {
            sb.append("\n");
            sb.append("@${DisableStringLiteralTemplates.class.getName()}\n");
            if (currClass.depth == 0) {
                if (currClass.isLayout) {
                    sb.append("public class ").reAppend(currClass.name).reAppend(" extends ").reAppend(currClass.superClass).reAppend(" implements ").reAppend(LAYOUT_INTERFACE).reAppend(" {\n");
                } else {
                    sb.append("public class ").reAppend(currClass.name).reAppend(" extends ").reAppend(currClass.superClass).reAppend(" {\n");
                }
            } else {
                sb.append("public static class ").reAppend(currClass.name).reAppend(" extends ").reAppend(currClass.superClass).reAppend(" {\n");
            }
            sb.append("    private static ").reAppend(currClass.name).reAppend(" INSTANCE = new ").reAppend(currClass.name).reAppend("();\n");
            sb.append("    private ").reAppend(currClass.name).reAppend("(){\n");
            if (currClass.hasLayout) {
                sb.append("        setLayout(").reAppend(currClass.layoutDir.className).reAppend(".asLayout());\n");
            }
            sb.append("}\n\n");
        }
        private void addRenderInto() {
            if (currClass.paramsList == null) {
                //without Layout
                sb.append("    public static void renderInto(Appendable buffer) {\n")
                        .append("        INSTANCE.renderImpl(buffer, null);\n")
                        .append("    }\n\n");
                //with Layout
                sb.append("    public static void renderInto(Appendable buffer, ILayout overrideLayout) {\n")
                        .append("        INSTANCE.renderImpl(buffer, overrideLayout);\n")
                        .append("    }\n\n");
            } else {
                //without Layout
                sb.append("    public static void renderInto(Appendable buffer, ").reAppend(currClass.params).reAppend(") {\n")
                        .append("        INSTANCE.renderImpl(buffer, null");
                for (String[] param: currClass.paramsList) {
                    sb.reAppend(", ").reAppend(param[1]);
                }
                sb.reAppend(");\n")
                        .append("    }\n\n");
                //with Layout
                sb.append("    public static void renderInto(Appendable buffer, ILayout overrideLayout, ").reAppend(currClass.params).reAppend(") {\n")
                        .append("        INSTANCE.renderImpl(buffer, overrideLayout");
                for (String[] param: currClass.paramsList) {
                    sb.reAppend(", ").reAppend(param[1]);
                }
                sb.reAppend(");\n")
                        .append("    }\n\n");
            }
        }


        private void makeClassContent() {
            addFileHeader();
            addRender();
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
            sb.append("}\n");
        }

        private void addHeaderAndFooter() {
            sb.append("    public static ").reAppend(LAYOUT_INTERFACE).reAppend(" asLayout() {\n")
                    .append("        return INSTANCE;\n")
                    .append("    }\n\n")
                    .append("    @Override\n")
                    .append("    public void header(Appendable buffer) throws IOException {\n")
                    .append("        if (getExplicitLayout() != null) {\n")
                    .append("            getExplicitLayout().header(buffer);\n")
                    .append("        }\n");
            assert(currClass.depth == 0);
            makeFuncContent(currClass.startTokenPos, currClass.contentPos);
            sb.append("    }\n")
                    .append("    @Override\n")
                    .append("    public void footer(Appendable buffer) throws IOException {\n");
            makeFuncContent(currClass.contentPos, currClass.endTokenPos);
            sb.append("        if (getExplicitLayout() != null) {\n")
                    .append("            getExplicitLayout().footer(buffer);\n")
                    .append("    }\n}\n");
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
                if (dir.dirType == IMPORT) {
                    sb.append("import " + dir.className + ";\n");
                }
            }
        }

        private void makeFuncContent(Integer startPos, Integer endPos) {
            ArrayList<Integer> bbLineNumbers = new ArrayList<>();
            if (endPos == null) {
                endPos = tokens.size() - 1;
            }
            sb.append("            int lineStart = Thread.currentThread().getStackTrace()[1].getLineNumber() + 1;\n");

            sb.append("            try {\n");
            outerLoop:
            for (int i = startPos; i <= endPos; i++) {
                Token token = tokens.get(i);
                switch (token.getType()) {
                    case STRING_CONTENT:
                        sb.append("                buffer.append(\"").reAppend(token.getContent().replaceAll("\"", "\\\\\"").replaceAll("\r", "").replaceAll("\n", "\\\\n") + "\");\n");
                        bbLineNumbers.add(token.getLine());
                        break;
                    case STATEMENT:
                        String[] statementList = token.getContent().split("\n");
                        for (int j = 0; j < statementList.length; j++) {
                            String statement = statementList[j].trim().replaceAll("\r", "");
                            sb.append("                ").reAppend(statement).reAppend("\n");
                            bbLineNumbers.add(token.getLine() + j);
                        }
                        break;
                    case EXPRESSION:
                        sb.append("                buffer.append(toS(").reAppend(token.getContent()).reAppend("));\n");
                        bbLineNumbers.add(token.getLine());
                        break;
                    case COMMENT:
                        break;
                    case DIRECTIVE:
                        Directive dir = dirMap.get(i);
                        if (dir.dirType == SECTION) {
                            ClassInfo classToSkipOver = currClass.nestedClasses.get(i + 1);
                            if (classToSkipOver.endTokenPos == null) {
                                i = endPos;
                            } else {
                                i = classToSkipOver.endTokenPos;
                            }
                            addSection(dir);
                        } else if (dir.dirType == END_SECTION) {
                            break outerLoop;
                        } else if (dir.dirType == INCLUDE) {
                            addInclude(dir);
                        } else if (dir.dirType == CONTENT) {
                            break;
                        }
                        break;
                }
            }
            String nums = bbLineNumbers.toString().substring(1, bbLineNumbers.toString().length() - 1);

            sb.append("            } catch (RuntimeException e) {\n");
            sb.append("                int[] bbLineNumbers = new int[]{").reAppend(nums).reAppend("};\n");
            sb.append("                handleException(e, \"").reAppend(this.currClass.fileName).reAppend("\", lineStart, bbLineNumbers);\n            }\n");
        }


        private void addInclude(Directive dir) {
            assert(dir.dirType == INCLUDE);
            if (dir.conditional == null) {
                if (dir.params != null) {
                    sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer, manifold.templates.runtime.ILayout.EMPTY,").reAppend(dir.params).reAppend(");\n");
                } else {
                    sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer, manifold.templates.runtime.ILayout.EMPTY);\n");
                }
            } else {
                sb.append("            if(").reAppend(dir.conditional).reAppend("){\n");
                if (dir.params != null) {
                    sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer, manifold.templates.runtime.ILayout.EMPTY,").reAppend(dir.params).reAppend(");\n");
                } else {
                    sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer, manifold.templates.runtime.ILayout.EMPTY);\n");
                }
                sb.append("            ").reAppend("}\n");
            }
        }

        private void addSection(Directive dir) {
            assert(dir.dirType == SECTION);
            if (dir.params != null) {
                String paramsWithoutTypes = dir.makeParamsStringWithoutTypes(dir.paramsList);
                sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer, ").reAppend(paramsWithoutTypes).reAppend(");\n");
            } else {
                sb.append("            ").reAppend(dir.className).reAppend(".renderInto(buffer);\n");
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