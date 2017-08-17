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


// state in program should be statically maintained
var x = 10
function incrementAndGet() {
  return x++;
}