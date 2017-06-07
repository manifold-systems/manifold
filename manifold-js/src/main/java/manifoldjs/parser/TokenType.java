package manifoldjs.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ecai on 6/21/2016.
 */


public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    CLASS,
    NUMBER,
    BOOLEAN,
    NULL,
    STRING,
    PUNCTUATION,
    OPERATOR,
    WHITESPACE,
    COMMENT,
    ERROR,
    EOF,
    //For template files
    RAWSTRING,
    TEMPLATEPUNC,
    TEMPLATESTRING;

    //Keywords taken from ecma-262 6 11.6.2.1, excluding class which is its own token
    private static final String[] KEYWORDS = {"break", "do", "in", "typeof",
            "case", "else", "instanceof", "var",
            "catch", "export", "new", "void",
            "extends", "return", "while",
            "const", "finally", "super", "with",
            "continue", "for", "switch", "yield",
            "debugger", "function", "this",
            "default", "if", "throw",
            "delete", "import", "try"};
    private static final Set<String> KEYWORDS_SET = new HashSet<String>(Arrays.asList(KEYWORDS));

    //Taken from mozilla expressions and operators guide
    private static String[] OPERATORS = {"=", "+=", "-=", "*=", "/=", "%=", "**=", "<<=", ">>=", ">>>=", "&=", "^=",
            "|=", //Assignment operators
            "==", "!=", "===", "!==", ">", ">=", "<", "<=", //Comparator operators
            "%", "++", "--", "-", "+", "**", "/", "*", //Arithmetic operators
            "&", "|", "^", "~", "<<", ">>", ">>>", //Bitwise operators
            "&&", "||", "!",  //Logical operators
            "?", ":", //Ternary operators
            "=>"}; //Arrow operator
    private static final Set<String> OPERATORS_SET = new HashSet<String>(Arrays.asList(OPERATORS));

    //Rules for identifier names from emca-262 section 11.6.1
    public static boolean startsIdentifier(char ch) {
        return (ch >= 'a' && ch <= 'z') ||
                (ch >= 'A' && ch <= 'Z') ||
                ch == '$' || ch == '_';
    }

    //todo: add unicode characters
    public static boolean partOfIdentifier(char ch) {
        return (ch >= '0' && ch <= '9') ||
                (ch >= 'a' && ch <= 'z') ||
                (ch >= 'A' && ch <= 'Z') ||
                ch == '$' || ch == '_';
    }

    public static boolean isKeyword(String word) {
        return KEYWORDS_SET.contains(word);
    }

    public static boolean isNull(String word) {
        return word.equals("null");
    }

    public static boolean isBoolean(String word) {
        return word.equals("false") || word.equals("true");
    }

    public static boolean isClass(String word) {
        return word.equals("class");
    }

    public static boolean isPunctuation(char ch) {
        return "(){}[].,;".indexOf(ch) >= 0;
    }

    public static boolean isPartOfOperator(char ch) {
        return "=+-*/%<>&^|!~?:".indexOf(ch) >= 0;
    }

    public static boolean isOperator(String word) {
        return OPERATORS_SET.contains(word);
    }

    public static boolean isHexCh(char ch) {
        return (ch <= '9' && ch >= '0') ||
                (ch >= 'a' && ch <= 'f') ||
                (ch >= 'A' && ch <= 'F');
    }

    public static boolean isDigit(char ch) {
        return (ch <= '9' && ch >= '0');
    }

    public static boolean isBinary(char ch) {
        return ch == '0' || ch == '1';
    }

    public static boolean isOctal(char ch) {
        return (ch <= '7' && ch >= '0');
    }

    //Taken from emca 6 language spec 11.3
    public static boolean isLineTerminator(char ch) {
        return "\n\r\u2028\u2029".indexOf(ch) >= 0;
    }


}
