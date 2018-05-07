package manifold.templates.statements;

import org.junit.Test;
import statements.*;

import static org.junit.Assert.assertEquals;

public class StatementsTest {

    @Test
    public void basicStatementsWork() {
        assertEquals("5", SimpleStatement.render());
    }

    @Test
    public void ifStatementsWork() {
        assertEquals("hello", IfStatement1.render());
        assertEquals("goodbye", IfStatement2.render());
        assertEquals("hello", IfStatementWithDefinedBoolean.render());
    }

    @Test
    public void loopsWork() {
        assertEquals("aaaaaaaaaa", WhileStatement.render());
        assertEquals("bbbbbbbbbb", ForStatement.render());
    }

    /**
     * Like our tests to make sure comments properly ignore all syntax, these tests ensure that
     * all BB syntax within strings is ignored.
     */
    @Test
    public void stringsWork() {
        assertEquals("<%= bwah %>lol", StringStatement1.render());
        assertEquals("<%@ directive syntax %><% statement syntax %>", StringStatement2.render());
        assertEquals("<%-- comment syntax --%>", StringStatement3.render());
    }

}