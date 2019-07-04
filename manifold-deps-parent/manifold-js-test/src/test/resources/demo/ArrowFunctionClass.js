class ArrowFunctionClass {
  static arrowReduceStatement() {
    //multiple params (with parens) and statement body (has curlies)
    return [1, 2, 3, 4, 5].reduce((f, g) => {return f + g});
  }

  static arrowFilterStatement() {
    //single param (no parens) and statement body (has curlies)
    return [1, 2, 3, 4, 5].filter(f => {return f == 3})[0];
  }
//
//  static arrowReduceExpression() {
//    //multiple params (with parens) and expression body (no curlies; implied return)
//    return [1, 2, 3, 4, 5].reduce((f, g) =>  (f + g));
//  }
//
//  static arrowFilterExpression() {
//    //single param (no parens)  and expression body (no curlies; implied return)
//    return [1, 2, 3, 4, 5].filter(f => (f == 3))[0];
//  }
}
