
class Person {

  constructor(firstName, lastName) {
    this._firstName = firstName
    this._lastName = lastName
  }

  displayName() {
    return this._firstName + " " + this._lastName
  }

  get firstName() {
    return this._firstName
  }

  get lastName() {
    return this._lastName
  }

}