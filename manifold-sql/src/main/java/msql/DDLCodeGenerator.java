package msql;

import manifold.util.ManClassUtil;
import msql.model.ColumnDefinition;
import msql.parser.SQLParser;
import msql.parser.SQLTokenizer;
import msql.parser.ast.CreateTable;
import msql.parser.ast.DDL;
import msql.util.NounHandler;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class DDLCodeGenerator {

    private final String _fqn;
    private final String _existing;
    private final SQLModel _model;
    private final DiagnosticListener<JavaFileObject> _errorHandler;

    public DDLCodeGenerator(String topLevelFqn, String existing, SQLModel model, DiagnosticListener<JavaFileObject> errorHandler) {
        _fqn = topLevelFqn;
        _existing = existing;
        _model = model;
        _errorHandler = errorHandler;
    }

    public String generateCode() {
        try(InputStream in = _model.getFile().openInputStream()) {
            SQLParser parser = new SQLParser(new SQLTokenizer(new InputStreamReader(in)));
            DDL parse = (DDL) parser.parse();
            return generateDDLClass(parse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String generateDDLClass(DDL parse) {
        StringBuilder code = new StringBuilder();

        code.append("package ").append(ManClassUtil.getPackage(_fqn)).append(";\n")
                .append("\n")
                .append("public class ").append(ManClassUtil.getNameNoPackage(_fqn)).append(" {\n\n");
        addDDLMethods(parse, code);
        for (CreateTable table : parse.getList()) {
            addTableClass(table, code);
        }
        code.append("}");

        return code.toString();
    }

    private void addTableClass(CreateTable table, StringBuilder code) {
        code.append("  public static class ").append(table.getTypeName()).append(" extends Object {\n");
        code.append("    \n");
        generateColumnEnum(table.getColumnDefinitions(), code);
        code.append("    \n");
        code.append("  }\n\n");
    }

    static class Foo {


    }

    public enum Fields {
        FOO(),
        BAR();
    }

    private void generateColumnEnum(List<ColumnDefinition> columnDefinitions, StringBuilder code) {
        code.append("    public enum Fields {\n");
        for (int i = 0; i < columnDefinitions.size(); i++) {
            ColumnDefinition columnDefinition = columnDefinitions.get(i);
            if (i > 0) {
                code.append(",\n");
            }
            code.append("      ").append(columnDefinition.getColumnName().toUpperCase());
        }
        code.append(";\n");
        code.append("    }\n");
    }

    private void addDDLMethods(DDL parse, StringBuilder code) {

    }
}
