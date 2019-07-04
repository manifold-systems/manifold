
class Person {

  constructor(firstName, lastName) {
    this._f = firstName
    this._l = lastName
  }

  displayName() {
    return this._f + " " + this._l
  }

  static staticFunction() {
    return 'hello';
  }

  get firstName() {
    java.lang.System.out.println("Here")
    return this._f
  }

  get lastName() {
    return this._l
  }

  set firstName(s) {
    java.lang.System.out.println("Here2")
    this._f = s
  }

  set lastName(s) {
    java.lang.System.out.println("Here3")
    this._l = s
  }

}