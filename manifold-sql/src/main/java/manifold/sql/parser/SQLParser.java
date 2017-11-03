package manifold.sql.parser;

import manifold.sql.parser.ast.*;
import msql.parser.ast.*;
import manifold.sql.model.ColumnDefinition;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SQLParser {

  /**
   * This is the parser for msql. This is an LR(1) parser using the grammar referred to in SQL.g. All valid
   * sentences in this parser are valid for use in the database; however, not necessarily all possible statements
   * according to the H2 grammar are valid in this parser. For conflicts, please see SQL.g for explanation.
   */

  private static final int MAX_ERRORS = 25;
  private SQLTokenizer _tokenizer;
  private Token currentToken;
  private HashMap<String, String> variables;
  private ArrayList<JavaVar> _vars;
  private int errPosition;
  private int errCount;
  private List<String> errors;

  public SQLParser(SQLTokenizer tokenizer) {
    _tokenizer = tokenizer;
    variables = new HashMap<>();
    _vars = new ArrayList<>();
    next();
  }

  private void next() {
    currentToken = _tokenizer.get();
  }

  private boolean tokEquals(TokenType expectedType) {
    return expectedType == currentToken.getType();
  }

  private void specialError(String s) {
    error(currentToken, "Expecting " + s + " but found '" + currentToken.getText() + "'.");
  }

  /**
   * Passes a single token
   * <p>
   *   Given a stack of tokens A, B, C, etc, calling pass with A sets the stack to
   *   B, C, etc back to the caller. Importantly, calling pass with any token but A does nothing.
   * </p>
   * @param passType
   */
  private void pass(TokenType passType) {
    if (passType == currentToken.getType()) {
      next();
    }
  }

  /**
   * Passes multiple tokens in a row
   * <p>
   *   Given a stack of tokens A, B, C, D, etc, calling pass with A, B and C
   *   will cause the parser to eat through A, B, C, returning to the caller with
   *   the stack now at D, etc. Importantly, if the first token is wrong, this method
   *   does nothing. If any token parameter is wrong, this method errors.
   * </p>
   * @param passTypes The Tokens to eat
   */
  private void pass(TokenType... passTypes) {
    if(passTypes[0] == currentToken.getType()){
      for(TokenType t: passTypes){
        match(t);
      }
    }
  }

  /**
   * Matches a single token
   * <p>
   *   If the current token is not the same as expected, an error will be reported. However, the current token
   *   will always advance regardless. The return of this method should not be used with any expectedType other than IDENT,
   *   which denotes a name. The return of this method is used to gather names.
   * </p>
   * @param expectedType the expected token type
   * @return the string text of this token, if valid
   */
  private String match(TokenType expectedType) {
    TokenType currentType = currentToken.getType();
    String s;
    if (currentType == expectedType) {
      if(currentType == TokenType.IDENT){
        s = currentToken.getText();
      } else {
        s = currentToken.toString();
      }
    } else {
      String name = currentType.getName();
      if (currentType == TokenType.IDENT) {
        name = currentToken.getText();
      }
      error(currentToken, "Expecting '" + expectedType.getName() + "' but found '" + name + "'.");
      s = name;
    }
    next();
    return s;
  }

  /**
   * Matches any number
   */
  private void matchNum() {
    TokenType currentType = currentToken.getType();
    if (currentType == TokenType.LONG || currentType == TokenType.INTERNALDOUBLE) {
      next();
    } else {
      String name = currentType.getName();
      error(currentToken, "Expecting 'LONG' or 'INTERNALDOUBLE' but found '" + name + "'.");
    }
  }

  /**
   * Matches a token to the list
   * <p>
   *   As long as the current token matches one of the tokens in list, true will be returned.
   * </p>
   * @param list The tokens against which you would like to match
   * @return true if the current token matches one, false otherwise.
   */
  private boolean matchIn(TokenType...list){
    for(TokenType t: list){
      if(currentToken.getType() == t){
        return true;
      }
    }
    return false;
  }

  /**
   * Matches lists of tokens
   * <p>
   *   Given a stack of tokens of form A ',' A ',' A ',' A B C etc, calling list with A will move the stack to B C etc
   *   back to the caller. Note that calling this method with the wrong initial token will error. If B is also ',' this
   *   method will error (ie, you cannot have a comma delimited list with different tokens and attempt to use this method).
   *   The return type of this method is based on match, and is used for getting lists of names
   * </p>
   * @param item the type of the list
   * @return names
   */
  private ArrayList<String> list(TokenType item) {
    ArrayList<String> items = new ArrayList<>();
    String _item = match(item);
    items.add(_item);
    while (tokEquals(TokenType.COMMA)) {
      next();
      _item = match(item);
      items.add(_item);
    }
    return items;
  }

  /**
   * Part of the error handling client.
   *
   * @param token
   * @param message
   */
  private void error(Token token, String message) {
    String fileName = _tokenizer.getFileName();
    if(SQLTokenizer.MEMORY_FILE.equals(fileName)) {
      fileName = "";
    } else {
      fileName = fileName + "- ";
    }
    int pos = token.getOffset();
    if (pos > errPosition && errCount < MAX_ERRORS) {
      String output = fileName + "[" + token.getLine() + ", " + token.getCol() + "] - ERROR: " + message;
      errors.add(output);
      System.err.println(output);
    }
    errCount++;
    errPosition = pos + 4;
  }

  private boolean isAValidStartSymbol() {
    return (tokEquals(TokenType.CREATE)  ||
            tokEquals(TokenType.ALTER)   ||
            tokEquals(TokenType.DROP)    ||
            tokEquals(TokenType.UPDATE)  ||
            tokEquals(TokenType.INSERT)  ||
            tokEquals(TokenType.DELETE)  ||
            tokEquals(TokenType.WITH)    ||
            tokEquals(TokenType.SELECT)  ||
            tokEquals(TokenType.EOF));
  }

  /**
   * Parses a file
   * @return a statement or a ddl type
   */
  public SQL parse() {
    errors = new ArrayList<>();
    errCount = 0;
    errPosition = -1;

    if (isAValidStartSymbol()) {
      return parseInternal();
    }
    error(currentToken, "Expecting a SQL statement (ex CREATE.., ALTER.., DROP..., ...)");
    // sync
    do {
      next();
    } while (!isAValidStartSymbol());
    return parseInternal();
  }

  private SQL parseInternal() {
    if(tokEquals(TokenType.CREATE)) {
      DDL statements = new DDL();
      while (true) {
        if (tokEquals(TokenType.EOF)) {
          statements.setErrCount(errCount);
          return statements;
        }
        statements.append(parseCreateTable());
        if (!(tokEquals(TokenType.EOF) || tokEquals(TokenType.SEMI))) {
          error(currentToken, "Expecting 'SEMI' or 'EOF but found " + currentToken.getType().getName());
        }
        if (tokEquals(TokenType.SEMI)) {
          match(TokenType.SEMI);
        }
      }
    } else if(tokEquals(TokenType.ALTER)){
      parseAlterTable();
      return new EmptyType();
    } else if(tokEquals(TokenType.DROP)){
      parseDropTable();
      return new EmptyType();
    } else if(tokEquals(TokenType.UPDATE)){
      UpdateStatement _update = parseUpdate();
      _update.setVars(_vars);
      if(!tokEquals(TokenType.EOF) && !tokEquals(TokenType.SEMI)){
        error(currentToken, "Garbage tokens at the end of the statement");
      }
      _update.setErrCount(errCount);
      return _update;
    } else if(tokEquals(TokenType.INSERT)){
      InsertStatement _insert = parseInsert();
      _insert.setVars(_vars);
      if(!tokEquals(TokenType.EOF) && !tokEquals(TokenType.SEMI)){
        error(currentToken, "Garbage tokens at the end of the statement");
      }
      _insert.setErrCount(errCount);
      return _insert;
    } else if(tokEquals(TokenType.DELETE)){
      DeleteStatement _delete = parseDelete();
      _delete.setVars(_vars);
      if(!tokEquals(TokenType.EOF) && !tokEquals(TokenType.SEMI)){
        error(currentToken, "Garbage tokens at the end of the statement");
      }
      _delete.setErrCount(errCount);
      return _delete;
    } else if(tokEquals(TokenType.SELECT)){
      SelectStatement _select = parseSelect();
      _select.setVariables(_vars);
      if(_select.getResultColumns() == null || _select.getTables() == null ||_select.getResultColumns().size() == 0 || _select.getTables().size() == 0){
        return new EmptyType();
      }
      if(!tokEquals(TokenType.EOF) && !tokEquals(TokenType.SEMI)){
        error(currentToken, "Garbage tokens at the end of the statement");
      }
      _select.setErrCount(errCount);
      return _select;
    } else if(tokEquals(TokenType.WITH)){
      SelectStatement _select = parseRecursiveQuery();
      _select.setVariables(_vars);
      if(_select.getResultColumns() == null || _select.getTables() == null ||_select.getResultColumns().size() == 0 || _select.getTables().size() == 0){
        return new EmptyType();
      }
      if(!tokEquals(TokenType.EOF) && !tokEquals(TokenType.SEMI)){
        error(currentToken, "Garbage tokens at the end of the statement");
      }
      _select.setErrCount(errCount);
      return _select;
    } else {
      return new EmptyType();
    }
  }

  private SelectStatement parseRecursiveQuery(){
    SelectStatement _ss;

    match(TokenType.WITH);
    match(TokenType.RECURSIVE);
    match(TokenType.IDENT);
    match(TokenType.LPAREN);
    list(TokenType.IDENT);
    match(TokenType.RPAREN);
    match(TokenType.AS);

    match(TokenType.LPAREN);
    SimpleSelect first = parseSimpleSelect();
    match(TokenType.UNION);
    match(TokenType.ALL);
    SimpleSelect second = parseSimpleSelect();
    match(TokenType.RPAREN);

    _ss = new SelectStatement(parseSimpleSelect());
    _ss.setRecursiveSelect(first, second);

    if(tokEquals(TokenType.ORDER)){
      next();
      match(TokenType.BY);
      OrderingTerm order = parseOrderingTerm();
      _ss.addOrderingTerm(order);
      while(tokEquals(TokenType.COMMA)){
        next();
        order = parseOrderingTerm();
        _ss.addOrderingTerm(order);
      }
    }

    if(tokEquals(TokenType.LIMIT)){
      next();
      Expression e = parseExpr();
      _ss.setLimitingTerm(e);
      if(tokEquals(TokenType.OFFSET)){
        next();
        e = parseExpr();
        _ss.setOffsetterm(e);
      }
    }

    return _ss;
  }

  private SelectStatement parseSelect() {
    SelectStatement _ss;

    _ss = new SelectStatement(parseSimpleSelect());

    while(matchIn(TokenType.UNION, TokenType.MINUS, TokenType.EXCEPT, TokenType.INTERSECT)){
      String chaintype = currentToken.getText().toUpperCase();
      if(tokEquals(TokenType.UNION)){
        next();
        if(tokEquals(TokenType.ALL)){
          chaintype += " ALL";
          next();
        }
      } else {
        next();
      }
      SimpleSelect chain = parseSimpleSelect();
      _ss.addChainedSelect(chain, chaintype);
    }

    if(tokEquals(TokenType.ORDER)){
      next();
      match(TokenType.BY);
      OrderingTerm order = parseOrderingTerm();
      _ss.addOrderingTerm(order);
      while(tokEquals(TokenType.COMMA)){
        next();
        order = parseOrderingTerm();
        _ss.addOrderingTerm(order);
      }
    }

    if(tokEquals(TokenType.LIMIT)){
      next();
      Expression e = parseExpr();
      _ss.setLimitingTerm(e);
      if(tokEquals(TokenType.OFFSET)){
        next();
        e = parseExpr();
        _ss.setOffsetterm(e);
      }
    }

    return _ss;
  }

  private SimpleSelect parseSimpleSelect(){
    SimpleSelect _sSelect;

    match(TokenType.SELECT);
    Term t = null;
    if(tokEquals(TokenType.TOP)){
      next();
      t = parseTerm();
    }

    if(matchIn(TokenType.DISTINCT, TokenType.ALL)){
      next();
    }


    if(tokEquals(TokenType.LPAREN)){
      next();
      ResultColumn firstResult = parseResultColumn();
      _sSelect = t == null ? new SimpleSelect(firstResult) : new SimpleSelect(firstResult, t);
      while(tokEquals(TokenType.COMMA)){
        next();
        _sSelect.addResultColumn(parseResultColumn());
      }
      match(TokenType.RPAREN);
    } else {
      ResultColumn firstResult = parseResultColumn();
      _sSelect = t == null ? new SimpleSelect(firstResult) : new SimpleSelect(firstResult, t);
      while(tokEquals(TokenType.COMMA)){
        next();
        _sSelect.addResultColumn(parseResultColumn());
      }
    }

    if(tokEquals(TokenType.AS)){
      next();
      String name = match(TokenType.IDENT);
      _sSelect.setAlias(name);
    }

    // sync
    if(!tokEquals(TokenType.FROM)){
      error(currentToken, "Expecting 'from' to begin the table list");
      do {
        next();
      } while(!tokEquals(TokenType.FROM) && !tokEquals(TokenType.EOF));
    }

    match(TokenType.FROM);
    parseTableOrSubquery(_sSelect);

    if(tokEquals(TokenType.WHERE)){
      next();
      Expression e = parseExpr();
      _sSelect.setWhereExpression(e);
    }
    if(tokEquals(TokenType.GROUP)){
      next();
      match(TokenType.BY);
      Expression e = parseExpr();
      _sSelect.addGroupByExpression(e);
      while(tokEquals(TokenType.COMMA)){
        next();
        e = parseExpr();
        _sSelect.addGroupByExpression(e);
      }
    }
    if(tokEquals(TokenType.HAVING)){
      next();
      Expression e = parseExpr();
      _sSelect.setHavingExpression(e);
    }

    return _sSelect;
  }

  private CreateTable parseCreateTable() {
    int line = currentToken.getLine();
    int col = currentToken.getCol();
    int offset = currentToken.getOffset();

    match(TokenType.CREATE);
    if (currentToken.getType() == TokenType.TEMP ||
      currentToken.getType() == TokenType.TEMPORARY) {
      next();
    }
    match(TokenType.TABLE);

    if (currentToken.getType() == TokenType.IF) {
      pass(TokenType.IF, TokenType.NOT, TokenType.EXISTS);
    }

    String name = currentToken.getText();
    CreateTable table = new CreateTable(name);
    table.setLoc(line, col, offset, name.length());
    match(TokenType.IDENT);
    if (currentToken.getType() == TokenType.DOT) {
      next();
      table.setName(match(TokenType.IDENT));
    }

    // sync
    if(!tokEquals(TokenType.LPAREN)) {
      error( currentToken, "Expected to find '(' to start the column definition list");
      do {
        next();
      } while(!tokEquals(TokenType.LPAREN) && !tokEquals(TokenType.EOF));
    }

    match(TokenType.LPAREN);
    if(matchIn(TokenType.CONSTRAINT, TokenType.CHECK, TokenType.UNIQUE, TokenType.FOREIGN, TokenType.PRIMARY)){
      table.append(parseConstraint());
    } else if(tokEquals(TokenType.IDENT)){
      table.append(parseColumnDef());
    } else {
      error(currentToken, "Expecting constraint or column definition");
      next();
    }
    while(tokEquals(TokenType.COMMA)){
      next();
      if(matchIn(TokenType.CONSTRAINT, TokenType.CHECK, TokenType.UNIQUE, TokenType.FOREIGN, TokenType.PRIMARY)){
        table.append(parseConstraint());
      } else if(tokEquals(TokenType.IDENT)){
        table.append(parseColumnDef());
      } else {
        error(currentToken, "Expecting constraint or column definition");
      }
    }
    match(TokenType.RPAREN);

    return table;
  }

  private InsertStatement parseInsert(){
    InsertStatement _insert;

    match(TokenType.INSERT);
    match(TokenType.INTO);
    String name = match(TokenType.IDENT);
    if(tokEquals(TokenType.DOT)){
      name += ".";
      next();
      name += match(TokenType.IDENT);
    }
    _insert = new InsertStatement(name);

    // sync
    /* usually IColumnEnum would not put such a complicated sync point but insert is sufficiently important that it needs a sync
     */
    if(!matchIn(TokenType.SET, TokenType.LPAREN, TokenType.DIRECT, TokenType.SORTED, TokenType.VALUES, TokenType.SELECT)){
      error(currentToken, "Expecting a token to start a list of columns but found '" + currentToken.getText() + "'.");
      do{
        next();
      } while(!matchIn(TokenType.SET, TokenType.LPAREN, TokenType.DIRECT, TokenType.SORTED, TokenType.VALUES, TokenType.SELECT, TokenType.EOF));
    }

    switch(currentToken.getType()){
      case SET:
        next();
        name = match(TokenType.IDENT);
        match(TokenType.EQ);
        Expression e = parseExpr();
        _insert.addColumn(name);
        _insert.addExpression(e);
        while(tokEquals(TokenType.COMMA)){
          next();
          name = match(TokenType.IDENT);
          match(TokenType.EQ);
          e = parseExpr();
          _insert.addColumn(name);
          _insert.addExpression(e);
        }
        break;

      case LPAREN:
        next();
        ArrayList<String> cols = list(TokenType.IDENT);
        _insert.setColumns(cols);
        match(TokenType.RPAREN);
        if(tokEquals(TokenType.VALUES)){
          ValuesClause _vals = parseValuesExpression();
          _insert.set(_vals);
        } else {
          pass(TokenType.DIRECT);
          pass(TokenType.SORTED);
          SelectStatement _statement = parseSelect();
          _insert.set(_statement);
        }
        break;

      case VALUES:
        ValuesClause _vals = parseValuesExpression();
        _insert.set(_vals);
        break;
      case DIRECT:
        next();
        pass(TokenType.SORTED);
        SelectStatement _statement = parseSelect();
        _insert.set(_statement);
        break;
      case SORTED:
        next();
      case SELECT:
        _statement = parseSelect();
        _insert.set(_statement);
        break;
      default:
        error(currentToken, "Unexpected token in insert statement");
        break;
    }

    return _insert;
  }

  private UpdateStatement parseUpdate(){
    UpdateStatement _update;

    match(TokenType.UPDATE);
    String name = match(TokenType.IDENT);
    if(tokEquals(TokenType.DOT)){
      name += ".";
      next();
      name += match(TokenType.IDENT);
    }
    _update = new UpdateStatement(name);

    pass(TokenType.AS);
    if(tokEquals(TokenType.IDENT)){
      String alias = match(TokenType.IDENT);
      _update.setAlias(alias);
    }

    // sync
    if(!matchIn(TokenType.SET, TokenType.LPAREN)){
      error(currentToken, "Expecting 'set' or '(' but found '" + currentToken.getText() + "'.");
      do {
        next();
      } while(!matchIn(TokenType.SET, TokenType.LPAREN, TokenType.EOF));
    }

    if(tokEquals(TokenType.SET)){
      next();
      String s = match(TokenType.IDENT);
      match(TokenType.EQ);
      Expression e = parseExpr();
      _update.set(s, e);

      while(tokEquals(TokenType.COMMA)){
        next();
        s = match(TokenType.IDENT);
        match(TokenType.EQ);
        e = parseExpr();
        _update.addColumn(s);
        _update.addExpression(e);
      }

    } else if(tokEquals(TokenType.LPAREN)){
      next();
      ArrayList<String> cols = list(TokenType.IDENT);
      _update.set(cols);
      match(TokenType.RPAREN);
      match(TokenType.EQ);
      match(TokenType.LPAREN);
      SelectStatement select = parseSelect();
      _update.setSelect(select);
      match(TokenType.RPAREN);
    }

    if(tokEquals(TokenType.WHERE)){
      next();
      Expression e = parseExpr();
      _update.setWhereExpression(e);
    }

    if(tokEquals(TokenType.LIMIT)){
      next();
      Expression e = parseExpr();
      _update.setLimitExpression(e);
    }

    return _update;
  }

  private void parseDropTable(){
    match(TokenType.DROP);
    match(TokenType.TABLE);
    pass(TokenType.IF, TokenType.EXISTS);
    match(TokenType.IDENT);
    if(tokEquals(TokenType.DOT)){
      next();
      match(TokenType.IDENT);
    }
    while(tokEquals(TokenType.COMMA)){
      match(TokenType.IDENT);
      if(tokEquals(TokenType.DOT)){
        next();
        match(TokenType.IDENT);
      }
    }
    if(tokEquals(TokenType.RESTRICT) || tokEquals(TokenType.CASCADE)){
      next();
    }
  }

  private DeleteStatement parseDelete(){
    DeleteStatement _delete;

    match(TokenType.DELETE);
    match(TokenType.FROM);
    String name = match(TokenType.IDENT);
    if(tokEquals(TokenType.DOT)){
      name += ".";
      next();
      name += match(TokenType.IDENT);
    }
    _delete = new DeleteStatement(name);

    if(tokEquals(TokenType.WHERE)){
      next();
      Expression e = parseExpr();
      _delete.setExpr(e);
    }

    if(tokEquals(TokenType.LIMIT)){
      next();
      Term t = parseTerm();
      _delete.setTerm(t);
    }

    return _delete;
  }

  private void parseAlterTable(){
    match(TokenType.ALTER);
    match(TokenType.TABLE);
    match(TokenType.IDENT);
    if(tokEquals(TokenType.DOT)){
      next();
      match(TokenType.IDENT);
    }


    if(tokEquals(TokenType.ADD)){
      next();
      pass(TokenType.COLUMN);
      if(tokEquals(TokenType.LPAREN)){
        next();
        parseColumnDef();
        while(tokEquals(TokenType.COMMA)){
          next();
          parseColumnDef();
        }
        match(TokenType.RPAREN);
      } else if(tokEquals(TokenType.IF)){
        pass(TokenType.IF, TokenType.NOT, TokenType.EXISTS);
        parseColumnDef();
        if(tokEquals(TokenType.BEFORE) || tokEquals(TokenType.AFTER)){
          next();
          match(TokenType.IDENT);
        }
      } else if(tokEquals(TokenType.CHECK) || tokEquals(TokenType.UNIQUE) || tokEquals(TokenType.FOREIGN) || tokEquals(TokenType.PRIMARY)){
        parseConstraint();
        if(tokEquals(TokenType.CHECK) || tokEquals(TokenType.NOCHECK)){
          next();
        }
      } else if(tokEquals(TokenType.IDENT)){
        Token next = _tokenizer.peek();
        if(next.getType().equals(TokenType.IDENT)){
          parseColumnDef();
          if(tokEquals(TokenType.BEFORE) || tokEquals(TokenType.AFTER)){
            next();
            match(TokenType.IDENT);
          }
        } else {
          parseConstraint();
          if(tokEquals(TokenType.CHECK) || tokEquals(TokenType.NOCHECK)){
            next();
          }
        }
      }
    } else if(tokEquals(TokenType.ALTER)){
      next();
      match(TokenType.COLUMN);
      String name = match(TokenType.IDENT);
      if(tokEquals(TokenType.RENAME)){
        next();
        match(TokenType.TO);
        match(TokenType.IDENT);
      } else if(tokEquals(TokenType.RESTART)){
        next();
        match(TokenType.WITH);
        match(TokenType.LONG);
      } else if(tokEquals(TokenType.SET)){
        next();
        if(tokEquals(TokenType.DEFAULT)){
          next();
          parseExpr();
        } else if(tokEquals(TokenType.NULL)){
          next();
        } else if(tokEquals(TokenType.NOT)){
          next();
          match(TokenType.NULL);
        }
      } else {
        parseTypeName(name);
        if(tokEquals(TokenType.DEFAULT)){
          next();
          parseExpr();
        }
        pass(TokenType.NOT);
        pass(TokenType.NULL);
        if(tokEquals(TokenType.AUTO_INCREMENT) || tokEquals(TokenType.IDENTITY)){
          next();
        }
      }
    } else if(tokEquals(TokenType.DROP)){
      next();
      if(tokEquals(TokenType.COLUMN) || tokEquals(TokenType.CONSTRAINT)){
        next();
        pass(TokenType.IF, TokenType.EXISTS);
        match(TokenType.IDENT);
      } else if(tokEquals(TokenType.PRIMARY)){
        next();
        match(TokenType.KEY);
      } else {
        error(currentToken, "Expecting COLUMN or CONSTRAINT or PRIMARY KEY but found '" + currentToken.getType() + "'.");
      }
    } else if(tokEquals(TokenType.RENAME)){
      next();
      match(TokenType.TO);
      match(TokenType.IDENT);
    } else {
      error(currentToken, "Unexpected token failure in parsing 'alter table' command. Failed on '" + currentToken.getType() + "'.");
    }
  }

  private ColumnDefinition parseTypeName(String name) {

    int datatype;

    if(currentToken.getType()!=TokenType.IDENT){
      error(currentToken, "Expecting IDENT (datatype) but found '" + currentToken.getType() + "'.");
    }

    Integer type = ColumnDefinition.lookUp.get(currentToken.getText());

    if(type == null) {
      datatype = Types.INTEGER;
    } else {
      datatype = type;
    }
    ColumnDefinition column = new ColumnDefinition(name,datatype);
    next();

    if(tokEquals(TokenType.IDENT) && "precision".equals(currentToken.getText())){
      next();
    }

    if((datatype == Types.NVARCHAR || datatype == Types.NCHAR || datatype == Types.BLOB || datatype == Types.CLOB || datatype == Types.DECIMAL )
            && tokEquals(TokenType.LPAREN)){
      next();
      column.setStartInt((int) currentToken.getLongNumber());
      matchNum();
      if( datatype == Types.DECIMAL){
        if(tokEquals(TokenType.COMMA)){
          next();
          column.setIncrementInt((int) currentToken.getLongNumber());
          matchNum();
        }
      }
      match(TokenType.RPAREN);
    }
    return column;
  }

  private OrderingTerm parseOrderingTerm() {
    OrderingTerm order;
    Expression er = parseExpr();
    order = new OrderingTerm(er);
    if(matchIn(TokenType.ASC, TokenType.DESC)){
      order.addToken(currentToken);
      next();
    }
    if(tokEquals(TokenType.NULLS)){
      order.addToken(currentToken);
      next();
      if(!matchIn(TokenType.FIRST, TokenType.LAST)){
        error(currentToken, "Expecting 'first' or 'last' but found '" + currentToken.getText() + "'.");
      }
      order.addToken(currentToken);
      next();
    }
    return order;
  }

  private ResultColumn parseResultColumn() {
    ResultColumn _rc = null;
    if(tokEquals(TokenType.TIMES)){
      next();
      _rc = new ResultColumn("*");
    } else if(tokEquals(TokenType.IDENT)){
      String name = match(TokenType.IDENT);
      if(tokEquals(TokenType.DOT)){
        name += ".";
        next();
        if(tokEquals(TokenType.IDENT)){
          name += match(TokenType.IDENT);
        } else if(tokEquals(TokenType.TIMES)){
          next();
          name += "*";
        }
      }
      _rc = new ResultColumn(name);
    } else if(tokEquals(TokenType.NULL)){
      next();
      _rc = new ResultColumn("NULL");
    } else {
      error(currentToken, "Expecting a result column (* or columnname) but found '" + currentToken.getText() + "'.");
    }
    return _rc;
  }

  private void parseTableOrSubquery(SimpleSelect simpleSelect){
    parseBasicTableOrSubquery(simpleSelect);

    if(tokEquals(TokenType.ON)){
      next();
      Expression e = parseExpr();
      simpleSelect.addOnExprToTable(e);
    }

  }

  private ValuesClause parseValuesExpression(){
    ValuesClause _values;
    ArrayList<Expression> currentExpressions = new ArrayList<>();
    match(TokenType.VALUES);

    match(TokenType.LPAREN);
    Expression e = parseExpr();
    currentExpressions.add(e);
    while(tokEquals(TokenType.COMMA)){
      next();
      e = parseExpr();
      currentExpressions.add(e);
    }
    match(TokenType.RPAREN);
    _values = new ValuesClause(currentExpressions);

    while(tokEquals(TokenType.COMMA)){
      currentExpressions = new ArrayList<>();
      next();
      match(TokenType.LPAREN);
      e = parseExpr();
      currentExpressions.add(e);

      while(tokEquals(TokenType.COMMA)){
        next();
        e = parseExpr();
        currentExpressions.add(e);
      }
      match(TokenType.RPAREN);
      if(_values.getSize() != currentExpressions.size()){
        error(currentToken, "Values Clause error: all parenthesized lists of expressions must have equal size. Failed on '" + currentToken.getText() + "'.");
      }
      _values.addExpressions(currentExpressions);
    }
    return _values;
  }

  private void parseBasicTableOrSubquery(SimpleSelect simpleSelect){
    TableOrSubquery _table = null;

    if(tokEquals(TokenType.IDENT)){
      String name = match(TokenType.IDENT);
      if(tokEquals(TokenType.DOT)){
        name += ".";
        next();
        name += match(TokenType.IDENT);
      }
      _table = new TableOrSubquery(name);
    } else if(tokEquals(TokenType.LPAREN)){
      next();
      if(tokEquals(TokenType.SELECT)){
        SelectStatement _select = parseSelect();
        _table = new TableOrSubquery(_select);
      } else if(tokEquals(TokenType.VALUES)){
        ValuesClause _values = parseValuesExpression();
        _table = new TableOrSubquery(_values);
      } else {
        error(currentToken, "Expecting 'select' or 'values' but found '" + currentToken.getText() + "'.");
      }
      match(TokenType.RPAREN);
    } else {
      error(currentToken, "Expecting table or subquery but found '" + currentToken.getText() + "'.");
    }

    pass(TokenType.AS);
    pass(TokenType.IDENT);
    if(_table != null){
      simpleSelect.addTableOrSubquery(_table);
    }

    if(matchIn(TokenType.LEFT, TokenType.RIGHT, TokenType.INNER, TokenType.CROSS, TokenType.NATURAL, TokenType.COMMA)){
      if(tokEquals(TokenType.LEFT) || tokEquals(TokenType.RIGHT)){
        next();
        match(TokenType.OUTER);
        match(TokenType.JOIN);
      } else if(tokEquals(TokenType.COMMA)){
        next();
      } else{
        next();
        match(TokenType.JOIN);
      }
      parseBasicTableOrSubquery(simpleSelect);
    }

  }

  private boolean isComparator() {
    return tokEquals(TokenType.EQ) ||
      tokEquals(TokenType.NEQ) ||
      tokEquals(TokenType.LT) ||
      tokEquals(TokenType.GT) ||
      tokEquals(TokenType.GTE) ||
      tokEquals(TokenType.LTE) ||
      tokEquals(TokenType.OVL);
  }

  private ColumnDefinition parseColumnDef() {
    String columnName = currentToken.getCasedText();
    int col = currentToken.getCol();
    int offset = currentToken.getOffset();
    int line = currentToken.getLine();
    match(TokenType.IDENT);

    ColumnDefinition column = parseTypeName(columnName);
    column.setLoc( line, col, offset, columnName.length() );

    if(tokEquals(TokenType.DEFAULT)){
      next();
      parseTerm();
    }

    if (tokEquals(TokenType.NOT)){
      next();
      match(TokenType.NULL);
      column.setNotNull(true);
    } else if (tokEquals(TokenType.NULL)) {
      next();
      column.setNull(true);
    }

    if (tokEquals(TokenType.AUTO_INCREMENT) || tokEquals(TokenType.IDENTITY)){
      if(tokEquals(TokenType.AUTO_INCREMENT)){
        column.setAutoIncrement(true);
      }
      else{
        column.setIdentity(true);
      }
      next();
      if (tokEquals(TokenType.LPAREN)) {
        next();
        column.setStartInt((int) currentToken.getLongNumber());
        matchNum();

        if (tokEquals(TokenType.COMMA)) {
          next();
          column.setIncrementInt((int) currentToken.getLongNumber());
          matchNum();
        }
        match(TokenType.RPAREN);
      }
    }

    if (tokEquals(TokenType.UNIQUE)) {
      column.setUnique(true);
      next();
    } else if (tokEquals(TokenType.PRIMARY)) {
      column.setPrimaryKey(true);
      next();
      match(TokenType.KEY);
      if(tokEquals(TokenType.HASH)){
        next();
        column.setHash(true);
      }
    }

    if (tokEquals(TokenType.CHECK)) {
      next();
      parseCondition();
    }

    return column;
  }

  private Constraint parseConstraint() {
    Constraint _constraint = new Constraint();

    if(tokEquals(TokenType.CONSTRAINT)){
      next();
      if (tokEquals(TokenType.IF)) {
        pass(TokenType.IF, TokenType.NOT, TokenType.EXISTS);
      }
      _constraint.setName(match(TokenType.IDENT));
    }

    if(tokEquals(TokenType.CHECK)){
      next();
      _constraint.setType(Constraint.constraintType.CHECK);
      Expression e = parseExpr();
      _constraint.setExpr(e);
    } else if(tokEquals(TokenType.UNIQUE)){
      String t;
      next();
      match(TokenType.LPAREN);
      t = match(TokenType.IDENT);
      _constraint.appendColumn(t);
      _constraint.setType(Constraint.constraintType.UNIQUE);
      while (tokEquals(TokenType.COMMA)) {
        next();
        t = match(TokenType.IDENT);
        _constraint.appendColumn(t);
      }
      match(TokenType.RPAREN);
    } else if(tokEquals(TokenType.PRIMARY)){
      next();
      match(TokenType.KEY);
      if(tokEquals(TokenType.HASH)){
        next();
        _constraint.setType(Constraint.constraintType.PRIMARYHASH);
      } else{
        _constraint.setType(Constraint.constraintType.PRIMARY);
      }
      match(TokenType.LPAREN);
      String t = match(TokenType.IDENT);
      _constraint.appendColumn(t);

      while (tokEquals(TokenType.COMMA)){
        next();
        t = match(TokenType.IDENT);
        _constraint.appendColumn(t);
      }
      match(TokenType.RPAREN);
    } else if (tokEquals(TokenType.FOREIGN)){
      _constraint.setType(Constraint.constraintType.FOREIGN);
      next();
      match(TokenType.KEY);
      match(TokenType.LPAREN);

      if(tokEquals(TokenType.IDENT)) {
        ArrayList<String> fKs = list(TokenType.IDENT);
        _constraint.setColumnNames(fKs);
      } else {
        error(currentToken, "Expecting a list of columns to be referenced, but did not find said list, instead found '" +
          currentToken.getText() + "'.");
        ArrayList<String> fKs = new ArrayList<>();
        _constraint.setColumnNames(fKs);
      }

      match(TokenType.RPAREN);
      match(TokenType.REFERENCES);
      if(tokEquals(TokenType.IDENT)){
        _constraint.setReferentialName(currentToken.getText());
        next();
      }
      if (tokEquals(TokenType.LPAREN)){
        next();
        ArrayList<String> refs = list(TokenType.IDENT);
        for(String r: refs){
          _constraint.appendReferentialColumn(r);
        }
        match(TokenType.RPAREN);
      }

      while (tokEquals(TokenType.ON)){
        next();
        if (tokEquals(TokenType.DELETE)){
          next();
          _constraint.setOnDelete(parseReferentialAction());
          if(tokEquals(TokenType.ON)){
            pass(TokenType.ON, TokenType.UPDATE);
            _constraint.setOnUpdate(parseReferentialAction());
          }
        } else if(tokEquals(TokenType.UPDATE)){
          next();
          _constraint.setOnUpdate(parseReferentialAction());
        } else{
          error(currentToken, "Expecting DELETE or UPDATE but found '" + currentToken.getText() + ".");
        }
      }
    } else{
      specialError("Column Definition or Constraint Definition");
    }
    return _constraint;
  }

  private Constraint.referentialAction parseReferentialAction() {


    switch (currentToken.getType()) {
      case CASCADE:
        next();
        return Constraint.referentialAction.CASCADE;
      case RESTRICT:
        next();
        return Constraint.referentialAction.RESTRICT;
      case NO:
        next();
        match(TokenType.ACTION);
        return Constraint.referentialAction.NO_ACTION;
      case SET:
        next();
        if (tokEquals(TokenType.DEFAULT)) {
          next();
          return Constraint.referentialAction.SET_DEFAULT;
        } else if(tokEquals(TokenType.NULL)){
          next();
          return Constraint.referentialAction.SET_NULL;
        } else {
          specialError("DEFAULT or NULL");
        }
        break;
      default:
        specialError("CASCADE or RESTRICT or NO or SET");
    }
    return Constraint.referentialAction.RESTRICT;
  }

  private Expression parseExpr() {
    Expression expression;
    if(tokEquals(TokenType.DEFAULT)){
      next();
      return new Default();
    }
    AndCondition condition = parseAndCondition();
    expression = new Expression(condition);
    while (tokEquals(TokenType.OR)) {
      expression.addToken(currentToken);
      next();
      condition = parseAndCondition();
      expression.addCondition(condition);
    }
    return expression;
  }

  private AndCondition parseAndCondition() {
    AndCondition andCondition;
    Condition c = parseCondition();
    andCondition = new AndCondition(c);
    while (tokEquals(TokenType.AND)) {
      andCondition.addToken(currentToken);
      next();
      c = parseCondition();
      andCondition.addCondition(c);
    }
    return andCondition;
  }

  private Condition parseCondition() {
    Condition condition;
    if (tokEquals(TokenType.NOT)) {
      next();
      return parseCondition();
    }
    if(tokEquals(TokenType.EXISTS)){
      condition = new Condition();
      condition.addToken(currentToken);
      next();
      condition.addToken(currentToken);
      match(TokenType.LPAREN);
      SelectStatement ss = parseSelect();
      condition.setFirst(new Operand(new Summand(new Factor(new GeneralTerm(ss)))));
      condition.addToken(currentToken);
      match(TokenType.RPAREN);
    } else {
      Operand o = parseOperand();
      condition = new Condition(o);
      if (tokEquals(TokenType.IS) || tokEquals(TokenType.BETWEEN) || tokEquals(TokenType.IN) || tokEquals(TokenType.NOT)
        || tokEquals(TokenType.LIKE) || tokEquals(TokenType.REGEXP) || isComparator()) {
        condition.addToken(currentToken);
        parseConditionRHS(condition);
      }
    }
    return condition;
  }

  private void parseConditionRHS(Condition existing) {

    if (isComparator()) {
      next();
      if (tokEquals(TokenType.ALL) || tokEquals(TokenType.ANY) || tokEquals(TokenType.SOME)) {
        existing.addToken(currentToken);
        next();
        match(TokenType.LPAREN);
        SelectStatement ss = parseSelect();
        Operand o = new Operand(new Summand(new Factor(new GeneralTerm(ss))));
        existing.setSecond(o);
        existing.addToken(currentToken);
        match(TokenType.RPAREN);
      } else {
        Operand o = parseOperand();
        existing.setSecond(o);
      }
    } else {
      switch (currentToken.getType()) {
        case IS:
          existing.addToken(currentToken);
          next();
          if (tokEquals(TokenType.NOT)) {
            existing.addToken(currentToken);
            next();
          }
          if (tokEquals(TokenType.NULL)) {
            existing.addToken(currentToken);
            next();
          } else {
            existing.addToken(currentToken);
            match(TokenType.DISTINCT);
            existing.addToken(currentToken);
            match(TokenType.FROM);
            Operand o = parseOperand();
            existing.setSecond(o);
          }
          break;
        case BETWEEN:
          existing.addToken(currentToken);
          next();
          Operand o = parseOperand();
          existing.setSecond(o);
          match(TokenType.AND);
          break;
        case IN:
          existing.addToken(currentToken);
          next();
          existing.addToken(currentToken);
          match(TokenType.LPAREN);
          if (tokEquals(TokenType.SELECT)) {
            SelectStatement ss = parseSelect();
            existing.setSecond(new Operand(new Summand(new Factor(new GeneralTerm(ss)))));
          } else {
            Expression e = parseExpr();
            existing.setSecond(new Operand(new Summand(new Factor(new GeneralTerm(new ExpressionArray(e))))));
            while (tokEquals(TokenType.COMMA)) {
              next();
              parseExpr();
              //TODO: FIX: need to handle more than one expression in condition
            }
          }
          existing.addToken(currentToken);
          match(TokenType.RPAREN);
          break;
        case NOT:
          existing.addToken(currentToken);
          next();
        case LIKE:
          existing.addToken(currentToken);
          next();
          o = parseOperand();
          existing.setSecond(o);
          if (tokEquals(TokenType.ESCAPE)) {
            existing.addToken(currentToken);
            next();
            match(TokenType.IDENT);
          }
          break;
        case REGEXP:
          existing.addToken(currentToken);
          next();
          o = parseOperand();
          existing.setSecond(o);
          break;
        default:
          specialError("Comparator, IS, BETWEEN, IN, NOT, LIKE, or REGEXP");
          break;
      }
    }
  }

  private Operand parseOperand() {
    Operand operand;
    Summand s = parseSummand();
    operand = new Operand(s);
    while (tokEquals(TokenType.BAR)) {
      operand.addToken(currentToken);
      next();
      s = parseSummand();
      operand.addSummand(s);
    }
    return operand;
  }

  private Summand parseSummand() {
    Summand summand;
    Factor f = parseFactor();
    summand = new Summand(f);
    while (tokEquals(TokenType.PLUS) || tokEquals(TokenType.MINUS)) {
      String nextOp = currentToken.toString();
      next();
      f = parseFactor();
      summand.add(nextOp, f);
    }
    return summand;
  }

  private Factor parseFactor() {
    Factor factor;
    Term t = parseTerm();
    factor = new Factor(t);
    while (tokEquals(TokenType.SLASH) || tokEquals(TokenType.TIMES) || tokEquals(TokenType.MOD)) {
      String nextOp = currentToken.toString();
      next();
      t = parseTerm();
      factor.add(nextOp, t);
    }
    return factor;
  }

  private Term parseTerm() {
    Term t = null;
    switch (currentToken.getType()) {
      case IDENT:
        String x = match(TokenType.IDENT);
        if(x.equals("date") || x.equals("time") || x.equals("timestamp")){
          String y = match(TokenType.IDENT);
          t = new DateTimeTerm(x, y);
          break;
        }
        if(tokEquals(TokenType.DOT)){
          next();
          x += ".";
          x += match(TokenType.IDENT);
        }
        t = new StringTerm(x);
        break;
      case AT:
        int l1 = currentToken.getLine();
        int c1 = currentToken.getCol();
        int skiplen = 1;
        next();
        String name = match(TokenType.IDENT);
        skiplen += name.length();
        JavaVar variable = new JavaVar(name);
        if(tokEquals(TokenType.COLON)){
          skiplen++;
          next();
          String _type = match(TokenType.IDENT);
          while(tokEquals(TokenType.DOT)){
            _type += '.';
            next();
            _type += currentToken.getCasedText();
            match(TokenType.IDENT);
          }
          variable.setVarType(_type);
          variables.put(variable.getVarName(), variable.getVarType());
          skiplen += _type.length();
        } else {
          String type = variables.get(variable.getVarName());
          if(type == null){
            throw new SQLParseError("ERROR: Variable " + name + " has no type.");
          }
          variable.setVarType(type);
        }
        variable.setLine(l1);
        variable.setCol(c1);
        variable.setSkiplen(skiplen);
        _vars.add(variable);
        t = new VariableTerm(variable);
        t.setLocation(l1, c1);
        break;
      case LONG:
      case INTERNALDOUBLE:
        t = currentToken.getLongNumber()!=0?new AlgebraicTerm(currentToken.getLongNumber())
          :new AlgebraicTerm(currentToken.getDoubleNumber());
        next();
        break;
      case QUESTION:
        next();
        long l;
        if(tokEquals(TokenType.MINUS)){
          l = -1*currentToken.getLongNumber();
          next();
          t = new QuestionTerm(l);
        } else if(tokEquals(TokenType.PLUS)){
          next();
          t = new QuestionTerm(currentToken.getLongNumber());
          next();
        } else if(tokEquals(TokenType.LONG)){
          t = new QuestionTerm(currentToken.getLongNumber());
          next();
        } else {
          t = new QuestionTerm();
        }
        break;
      case PLUS:
        next();
        t = parseLimitedTerm(false);
        break;
      case MINUS:
        next();
        t = parseLimitedTerm(true);
        break;
      case LPAREN:
        next();
        if (tokEquals(TokenType.SELECT)) {
          SelectStatement ss = parseSelect();
          t = new GeneralTerm(ss);
        } else {
          Expression e = parseExpr();
          ExpressionArray _exp = new ExpressionArray(e);
          while(tokEquals(TokenType.COMMA)){
            next();
            e = parseExpr();
            _exp.addExpression(e);
          }
          t = new GeneralTerm(_exp);
        }
        match(TokenType.RPAREN);
        break;
      case CASE:
        next();
        Case c;
        if (tokEquals(TokenType.WHEN)) {
          c = parseCaseWhen();
        } else {
          c = parseCase();
        }
        t = new CaseTerm(c);
        break;
      case NULL:
      case CURRENT_DATE:
      case CURRENT_TIME:
      case CURRENT_TIMESTAMP:
        next();
        break;
      default:
        specialError("Any term");
        break;
    }
    return t;
  }

  private Term parseLimitedTerm(boolean isNegative) {
    Term t;
    if(tokEquals(TokenType.LONG)){
      t = new AlgebraicTerm(currentToken.getLongNumber());
      next();
    }else if(tokEquals(TokenType.INTERNALDOUBLE)){
      t = new AlgebraicTerm(currentToken.getDoubleNumber());
      next();
    }else if(tokEquals(TokenType.LPAREN)){
      Expression e = parseExpr();
      ExpressionArray _exp = new ExpressionArray(e);
      t = new GeneralTerm(_exp);
    }else{
      specialError("Algebraic Term");
      return null;
    }
    t.setNegative(isNegative);
    return t;
  }

  private Case parseCase() {
    Case _case = new Case();
    Expression init = parseExpr();
    _case.setInitial(init);
    parseCaseWhen(_case);
    return _case;
  }

  private Case parseCaseWhen() {
    Case _case = new Case();
    match(TokenType.WHEN);
    Expression when = parseExpr();
    match(TokenType.THEN);
    Expression then = parseExpr();
    _case.addWhenThen(when, then);
    if (tokEquals(TokenType.WHEN)) {
      parseCaseWhen(_case);
    } else {
      if (tokEquals(TokenType.ELSE)) {
        next();
        Expression fin = parseExpr();
        _case.setElse(fin);
      }
      match(TokenType.END);
  }
    return _case;
  }

  private void parseCaseWhen(Case _case) {
    match(TokenType.WHEN);
    Expression when = parseExpr();
    match(TokenType.THEN);
    Expression then = parseExpr();
    _case.addWhenThen(when, then);
    if (tokEquals(TokenType.WHEN)) {
      parseCaseWhen(_case);
    } else {
      if (tokEquals(TokenType.ELSE)) {
        next();
        Expression fin = parseExpr();
        _case.setElse(fin);
      }
      match(TokenType.END);
    }
  }

  public List<String> getErrors() {
    return errors;
  }
}
