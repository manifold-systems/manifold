//==============================================================================
// simple functions
//==============================================================================

// basic string
function returnsString() {
  return "foo";
}

// basic number
function returnsNumber() {
  return 10;
}

// basic object
function returnsObject() {
  return {"foo" : "bar"};
}

// typed string
function returnsStringAsString() : String {
  return "foo";
}

// identity function
function identity(i) {
  return i
}

// typed identity function
function identityString(i : String) {
  return i
}

// two args function
function twoArgs(i, j) {
  return i + j
}

// three args function
function threeArgs(i, j, k) {
  if(k) {
    return i
  } else {
    return j
  }
}

function returnAsString(i) : String {
  return i;
}

// state in program should be statically maintained
var x = 10
function incrementAndGet() {
  return x++;
}