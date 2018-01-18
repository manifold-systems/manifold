package manifold.js.parser;

import manifold.js.parser.tree.*;

public class Parser
{
  private ClassNode _classNode;
  private ProgramNode _programNode;
  private Tokenizer _tokenizer;
  private Token _currentToken, _nextToken;
  private ParseContext _context;

  //Constructor sets the src from which the parser reads
  public Parser(Tokenizer tokenizer){
    _tokenizer = tokenizer;
    _programNode = new ProgramNode(tokenizer.getUrl());
    _context = new ParseContext();
  }

  public boolean isES6Class() {
    return _classNode != null;
  }

  public Node parse() {
    nextToken();
    //Can only import classes at top of program
    parseImports();
    //Maybe parse class
    parseClassStatement();
    //Parse program otherwise
    if (!isES6Class() && !match(TokenType.EOF)) {
      addParseFillerUntil(_programNode, () -> match(TokenType.EOF));
    }
    return _programNode;
  }


  private void parseClassStatement()
  {
    if( match(TokenType.CLASS))
    {
      //parse class name
      nextToken();
      Token className = _currentToken;
      skip(match(TokenType.IDENTIFIER));
      _classNode = new ClassNode( className.getValue() );
      _programNode.addChild(_classNode);
      //parse any super classes
      if(matchKeyword("extends")) {
        skip(matchKeyword("extends"));
        StringBuffer sb = new StringBuffer();
        while (match(TokenType.IDENTIFIER)) {
          sb.append(_currentToken.getValue());
          skip(match(TokenType.IDENTIFIER));
          if (match('.')) {
            skip(match('.'));
            sb.append('.');
          } else {
            break;
          }
        }
        _classNode.setSuperClass(sb.toString());
      }
      //parse class body
      skip(match('{'));
      parseClassBody(className.getValue());
      skip(match('}'));
      Token end = _currentToken;
      _classNode.setTokens(className, end);
    }
  }

  private void parseImports() {
    while (matchKeyword("import") && !match(TokenType.EOF)) {
      _programNode.addChild(parseImport());
      if (match(';')) nextToken(); //TODO: learn semi-colon syntax
    }
  }


  protected ImportNode parseImport() {
    Token start = _currentToken;
    skip(matchKeyword("import"));
    StringBuilder packageName = new StringBuilder();
    Matcher matcher = () -> match(TokenType.IDENTIFIER);
    while (matcher.match()) {
      concatToken(packageName);
      if (match(TokenType.IDENTIFIER)) matcher = () -> match('.');
      if (match('.')) matcher = () -> match(TokenType.IDENTIFIER);
      nextToken();
    }
    ImportNode importNode = new ImportNode(packageName.toString());
    importNode.setTokens(start, _currentToken);
    return importNode;
  }

  private void parseClassBody(String className)
  {
    while(!match('}') && !match(TokenType.EOF)) {
      if (matchClassKeyword("constructor")) {
        _classNode.addChild(parseConstructor(className));
      } else if (matchClassKeyword("static")) { //properties and functions can both be static
        Token staticToken = _currentToken;
        nextToken();
        if (matchClassKeyword("get") || matchClassKeyword("set")) {
          _classNode.addChild(parseStaticProperty(className, staticToken));
        } else {
          _classNode.addChild(parseStaticFunction(className, staticToken));
        }
      } else if (matchClassKeyword("get") || matchClassKeyword("set")) {
        _classNode.addChild(parseProperty(className));
      } else if (match(TokenType.IDENTIFIER)) {
        ClassFunctionNode functionNode = parseClassFunction(className);
        _classNode.addChild(functionNode);
      } else {
        error("Unexpected token: " + _currentToken.toString());
        nextToken();
      }
    }
  }

  private ConstructorNode parseConstructor(String className) {
    Token start = _currentToken; //'constructor'
    skip(matchClassKeyword("constructor"));

    ConstructorNode constructorNode = new ConstructorNode(className);
    constructorNode.setTokens(start, null);
    addParseFunctionParamAndBody(constructorNode);

    nextToken();
    return constructorNode;
  }

  private ClassFunctionNode parseStaticFunction(String className, Token staticToken) {
    ClassFunctionNode functionNode = (ClassFunctionNode) parseFunction(className);
    functionNode.setTokens(staticToken, functionNode.getEnd());
    functionNode.setStatic(true);
    return functionNode;
  }

  private ClassFunctionNode parseClassFunction(String className) {
    expect(match(TokenType.IDENTIFIER)); //name of function
    _context.inOverrideFunction = isOverrideFunction(_currentToken.getValue());
    ClassFunctionNode functionNode = (ClassFunctionNode) parseFunction(className);
    functionNode.setOverride(_context.inOverrideFunction);
    _context.inOverrideFunction = false;
    return functionNode;
  }

  private FunctionNode parseFunction(String className) {
    Token start = _currentToken; //Name of function
    String functionName = start.getValue();
    skip(match(TokenType.IDENTIFIER));

    FunctionNode functionNode;
    if (className != null) {
      functionNode = new ClassFunctionNode(functionName, className, false);
    }
    else  {
      functionNode = new FunctionNode(functionName);
    }
    functionNode.setTokens(start, null);

    addParseFunctionParamAndBody(functionNode);
    nextToken();
    return functionNode;

  }

  private PropertyNode parseStaticProperty(String className, Token staticToken) {
    PropertyNode propertyNode = parseProperty(className);
    propertyNode.setTokens(staticToken, propertyNode.getEnd());
    propertyNode.setStatic(true);
    return propertyNode;
  }


  private PropertyNode parseProperty(String className) {
    boolean isSetter = matchClassKeyword("set");
    skip(matchClassKeyword("get") || matchClassKeyword("set"));
    Token start = _currentToken; // property identifier
    String functionName = _currentToken.getValue();
    skip(match(TokenType.IDENTIFIER));

    PropertyNode node = new PropertyNode(functionName, className, false, isSetter);
    node.setTokens(start, null);
    addParseFunctionParamAndBody(node);
    nextToken();
    return node;
  }

  /*Concats parameters into a node*/
  protected ParameterNode parseParams() {
    skip(match('('));
    ParameterNode paramNode = new ParameterNode();

    Matcher matcher = () -> match(')') || match(TokenType.IDENTIFIER);
    expect(matcher);
    while (!match(')') && !match(TokenType.EOF)) {
      if (match(TokenType.IDENTIFIER)) {
        matcher = () -> match(',') || match(')') || match(':'); //ending paren or comma can follow identifier
        String paramValue = _currentToken.getValue();
        paramNode.addParam(paramValue, parseType());
      } else if (match(',')) {
        matcher = () -> match(TokenType.IDENTIFIER); //identifier must follow commas
      }
      nextToken();
      expect(matcher);
    }
    skip(match(')'));

    return paramNode;
  }

  /* Function: parseReturnType
     -------------------------
     Sees if there is a return type of the format function() : returnType {}
      and returns dynamic.Dynamic if none is specified
   */
  private String parseReturnType() {
    if(_currentToken.getValue().equals(":")) {
      nextToken();
        String returnType = _currentToken.getValue();
        nextToken();
        return returnType;
    }
    return "java.lang.Object";

  }

  /* Function: parseType()
     ---------------------
     Peeks at the next section of the argument list to see if the code
     specifies a return type
   */
  private String parseType() {
    if(peekToken().getValue().equals(":")) {
      nextToken();
      nextToken();
      return _currentToken.getValue();
    }
    return null;

  }

  private FunctionBodyNode parseFunctionBody(String functionName) {
    FunctionBodyNode bodyNode = new FunctionBodyNode(functionName);
    int currCurlyCount = _context.getCurlyCount() - 1;
    addParseFillerUntil(bodyNode, () -> _context.getCurlyCount() <= currCurlyCount);
    FillerNode lastCurly = new FillerNode();
    lastCurly.concatToken(_currentToken);
    bodyNode.addChild(lastCurly);
    return bodyNode;
  }


  /*starting from the opening parens for function parameters, parses the function params, return type,
  and function body, and appends to the parent
   */
  private void addParseFunctionParamAndBody(FunctionNode parent) {
    ParameterNode params = parseParams();
    String returnType = parseReturnType();

    FunctionBodyNode body = parseFunctionBody(parent.getName());
    parent.setReturnType(returnType);
    expect(match('}'));
    parent.setTokens(parent.getStart(), _currentToken);
    parent.addChild(params);
    parent.addChild(body);
  }


  /*Parses filler code and adds onto parent, as well watching for es6 features such as arrow functions
   and string templates
    */
  private void addParseFillerUntil(Node parent, Matcher matcher) {
    FillerNode fillerNode = parseFillerUntil(() -> matchOperator("=>")
            || match(TokenType.TEMPLATESTRING)
            || (matchKeyword("function") && _context.curlyCount == 0)
            || matcher.match());
    //Pause when seeing an arrow so we can add an arrow node
    if (matchOperator("=>"))
    {
      skip(matchOperator("=>"));
      ArrowExpressionNode arrowNode = new ArrowExpressionNode();
      arrowNode.extractParams(fillerNode);
      //Add filler node and create a new one
      parent.addChild(fillerNode);
      parent.addChild(arrowNode);
      addParseFillerUntil(parent, matcher); //continue parsing filler after consuming arrow node
    }
    //Pause when we see a backtick, and use the template parser to consume the template string
    else if (match(TokenType.TEMPLATESTRING)) {
      TemplateParser templateParser = new TemplateParser(new TemplateTokenizer(currToken().getValue(), false));
      Node templateNode = templateParser.parse();
      parent.addChild(fillerNode);
      parent.addChild(templateNode);
      nextToken();
      addParseFillerUntil(parent, matcher); //continue parsing filler after consuming template string
    }
    //Pause when we see a function declaration to parse typescript style types
    else if (matchKeyword("function") && _context.curlyCount == 0) {
      skip(matchKeyword("function"));
      parent.addChild(fillerNode);
      FunctionNode functionNode = parseFunction(null);
      parent.addChild(functionNode);
      addParseFillerUntil(parent, matcher);
    }
      else {
      //reached the end token passed in to the argument; end parsing filler
      parent.addChild(fillerNode);
    }
  }

  //Concatenate tokens onto a filler node until a token is matched
  protected FillerNode parseFillerUntil(Matcher matcher) {
    FillerNode fillerNode = new FillerNode(_context.inOverrideFunction);
    while (!(match(TokenType.EOF) || matcher.match())) {
      fillerNode.concatToken(_currentToken);
      nextAnyToken();
    }
    return fillerNode;
  }

  //========================================================================================
  // Utilities
  //========================================================================================

  /*Concats current token to a string builder*/
  private void concatToken (StringBuilder val) {
    val.append(_currentToken.getValue());
  }

  //Used to create lambda functions for matching tokens
  protected interface Matcher {
    boolean match();
  }

  protected void expect(Matcher matcher) {
    if (!matcher.match()) expect(false);
  }

  protected void expect(boolean b) {
    if (!b) error("Unexpected Token: " + _currentToken.toString());
  }

  /*assert an expectation for the current token then skip*/
  protected void skip(boolean b) {
    expect(b);
    nextToken();
  }

  private void error(String errorMsg) {
    _programNode.addError(errorMsg, currToken());
  }

  /*Match single character punctuation*/
  protected boolean match( char c )
  {
    return match(TokenType.PUNCTUATION, String.valueOf(c));
  }

  /*Match operators*/
  protected boolean matchOperator(String val )
  {
    return match(TokenType.OPERATOR, val);
  }


  /*Match reserved keywords only*/
  protected boolean matchKeyword(String val)
  {
    return match(TokenType.KEYWORD, val);
  }

  /*Matches conditional keywords such as "constructor", which are sometimes keywords within a class
   and identifiers otherwise*/
  protected boolean matchClassKeyword(String val)
  {
    if (!match(TokenType.IDENTIFIER, val)) return false;
    //If these class keywords aren't followed by an identifier, treat them as regular identifiers
    if ((val.equals("static") || val.equals("get") || val.equals("set")) &&
            peekToken().getType() != TokenType.IDENTIFIER) return false;
    return  true;
  }

  protected boolean match(TokenType type, String val) {
    return match(type) && _currentToken.getValue().equals(val);
  }

  protected boolean match( TokenType type )
  {
    return (_currentToken.getType() == type);
  }


  private Token peekToken() {
    if (_nextToken == null || _nextToken.getOffset() <= _currentToken.getOffset()) {
      _nextToken = _tokenizer.nextNonWhiteSpace();
    }
    return _nextToken;
  }

  protected Token currToken() {
    return _currentToken;
  }

  private boolean isOverrideFunction(String functionName) {
    if (_classNode == null) return false;
    String packageName = _programNode.getPackageFromClassName(_classNode.getSuperClass());
    if (packageName == null) return false;

    //TODO: figure out when overriding java method if neccessary

//    IType superType = TypeSystem.getByFullName(packageName);
//    if (superType == null) return false;
//    for (IMethodInfo method : superType.getTypeInfo().getMethods()) {
//      if (method.getDisplayName().equals(functionName)) return true;
//    }

    return false;
  }

  /*Move current token to the next token (including whitespace)*/
  private void nextAnyToken() {
    _currentToken = _tokenizer.next();
    if (match('{')) _context.curlyCount++;
    if (match('}')) _context.curlyCount--;
  }

  /*Move current token to the next non-whitespace token*/
  protected void nextToken()
  {
    if (_currentToken == null || _nextToken == null || _currentToken.getOffset() >= _nextToken.getOffset()) {
      _currentToken = _tokenizer.nextNonWhiteSpace();
    } else {
      _currentToken = _nextToken;
    }
    if (match('{')) _context.curlyCount++;
    if (match('}')) _context.curlyCount--;
  }
}