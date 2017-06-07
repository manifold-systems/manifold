package manifoldjs.parser;

import java.util.HashMap;
import java.util.Stack;

public class TemplateTokenizer extends Tokenizer {

  private boolean inRawString;
  private boolean inStatementOrExpression;
  private boolean _isJST; //true if a JST template file, false if a template literal in a Util file
  private String exprStart; //token that enters an expressionOrStatement
  private HashMap <String, String> puncEnterExitMap; //maps enter punctuation to exit punctuation (ex: "${" : "}")
  private Stack<String> curlyStack; //used to match curlies when exiting an expression

  public TemplateTokenizer(String source, boolean isJST) {
    super(source);
    _isJST = isJST;
    inRawString = true;
    inStatementOrExpression = false;
    curlyStack = new Stack<>();
    puncEnterExitMap = new HashMap<>();
    puncEnterExitMap.put("${", "}");
    if (isJST) {
        puncEnterExitMap.put("<%", "%>");
        puncEnterExitMap.put("<%=", "%>");
        puncEnterExitMap.put("<%@", "%>");
    }
  }

  public boolean isJST() {
      return _isJST;
  }

  @Override
  public Token next() {
    Token toke;
    if (reachedEOF()) {
      toke = newToken(TokenType.EOF, "EOF");
    } else if (inRawString) {
      toke = consumeRawString();
    } else if (inStatementOrExpression){
      if (checkForExpressionExit()) return consumeTemplatePunc(); //transition from expression to rawstring
      Token supe = super.next();
      if (supe.getType() == TokenType.PUNCTUATION && supe.getValue().equals("}")) curlyStack.pop();
      if (supe.getType() == TokenType.PUNCTUATION && supe.getValue().equals("{")) curlyStack.push("{");
      return supe; //if in statement, tokenize as normal
    } else {
      toke = consumeTemplatePunc(); //transition from rawstring to expression; ${, <%, <%@, or <%=
    }
    return toke;
  }

  private Token consumeTemplatePunc() {
    String punc = String.valueOf(currChar());
    switch (currChar()) {
      //Entrance punctuations
      case '$': nextChar();
                punc += currChar(); //should be '{'
                nextChar();
                setInStatementOrExpression();
                curlyStack.push("${");
                break;
      case '<': nextChar();
                punc += currChar();
                nextChar();
                if (currChar() == '@' || currChar() == '=') {
                  punc += currChar();
                  nextChar();
                }
                setInStatementOrExpression();
                break;
      //Exit punctuations
      case '%': nextChar();
                punc += currChar(); //should be '>'
                nextChar();
                setInRawString();
                break;
      case '}': nextChar();
                setInRawString();
                break;
    }
    if (inStatementOrExpression) exprStart = punc;
    return newToken(TokenType.TEMPLATEPUNC, punc);
  }

  private Token consumeRawString() {
    StringBuilder val = new StringBuilder();
    while (!reachedEOF()) {
      if (checkForExpressionEnter()) {
        inRawString = false;
        break;
      }
      if (!_isJST && TokenType.isLineTerminator(currChar())) {
          if (currChar() == '\r' && peek() == '\n') nextChar(); //skip over the \r in \r\n for windows files
          val.append("\\n"); //escape newlines since template literals can be multiline
      } else {
          val.append(currChar());
      }
      nextChar();
    }
    return newToken(TokenType.RAWSTRING, val.toString());
  }

  private boolean checkForExpressionEnter() {
    //If escaped, skip escape character and return false
    if (currChar() == '\\' && (peek() == '<' || peek() == '$')) {
        nextChar(); return false;
    }

    exprStart = (puncEnterExitMap.get(String.valueOf(currChar()) + peek()));
    return exprStart != null;
  }

  private boolean checkForExpressionExit() {
    //'}' only exits expression if it matches with at the top of the stack ${
    if (exprStart.equals("${") && currChar() == '}' && curlyStack.peek().equals("${")) return true;
    if (!exprStart.equals("${") && currChar() == '%' && peek() == '>') return true;
    return false;
  }

  private void setInRawString() {
    inRawString = true;
    inStatementOrExpression = false;
  }

  private void setInStatementOrExpression() {
    inStatementOrExpression = true;
    inRawString = false;
  }


}
