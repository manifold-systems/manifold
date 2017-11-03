package manifold.sql;

import manifold.sql.parser.SQLParser;
import manifold.sql.parser.SQLTokenizer;
import manifold.sql.parser.ast.DDL;
import org.junit.Test;

import java.io.StringReader;

public class BootstrapTest {

    @Test
    public void bootstrapTest() {
        DDLCodeGenerator codeGen = new DDLCodeGenerator("foo.Bar", "", null, null);
        DDL ddl = getSampleDDL();
        String s = codeGen.generateDDLClass(ddl);
        System.out.println(s);
    }

    private DDL getSampleDDL() {
        SQLParser parser = new SQLParser(new SQLTokenizer(new StringReader(
                "CREATE TABLE IF NOT EXISTS STATES (\n" +
                "    id int,\n" +
                "    name varchar(255)\n" +
                ");\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS CONTACTS (\n" +
                "    id bigint auto_increment,\n" +
                "    user_id  int,\n" +
                "    company_id int,\n" +
                "    first_name nchar(50),\n" +
                "    last_name nchar(50),\n" +
                "    age int,\n" +
                "    state_id int,\n" +
                "    FOREIGN KEY (state_id) REFERENCES STATES (id)\n" +
                ");\n" +
                "\n" +
                "\n" +
                "CREATE TABLE IF NOT EXISTS COMPANY (\n" +
                "    id bigint auto_increment,\n" +
                "    name nchar(50)\n" +
                ");")));
        return (DDL) parser.parse();
    }

}
