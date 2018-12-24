package manifold.js.parser;

public class ParseContext {
    public boolean inOverrideFunction = false;
    public int curlyCount = 0;

    public int getCurlyCount() {
        return curlyCount;
    }
}
