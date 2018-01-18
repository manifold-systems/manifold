
// Program-level variable
var x = 1;

// Always ends up as double on teh way out for some reason
function nextNumber() {
  return x++;
}

function exampleFunction(x) {
  return x + " from Javascript";
}