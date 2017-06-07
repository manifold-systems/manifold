
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Integer
import java.lang.Double

class TypingClass () {
   constructor(a : String) {
     this.foo = a;

   }
   doubleToStringReturnCoercionTest(x : Double, y: Double) : String {
       return x + y ;
   }

   returnsDouble(x : Double) : Double {
       return x;
   }

   returnsWrongType(x : String) : Double {
       return x;
   }

}
