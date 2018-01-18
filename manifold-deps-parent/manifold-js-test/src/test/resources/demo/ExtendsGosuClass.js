import demo.ExampleGosuClass

class ExtendsGosuClass extends ExampleGosuClass {
  calculateTheAnswer() {
    return super.calculateTheAnswer() + 2;
  }
}