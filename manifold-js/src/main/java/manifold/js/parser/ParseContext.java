package manifold.js.parser;

/**
 * Created by ecai on 8/4/2016.
 */
public class ParseContext {
    public boolean inOverrideFunction = false;
    public int curlyCount = 0;

    public int getCurlyCount() {
        return curlyCount;
    }
}
