class BasicJavascriptClass {

  constructor() {
    this.x = 0
  }

  returnStr() {
    return "Hello World";
  }

  incrementAndGet() {
    this.x = this.x + 1;
    return this.x;
  }

  identity(y) {
    return y
  }
}