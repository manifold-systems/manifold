import java.lang.Runnable

class ImplementsRunnable extends Runnable {

 constructor(msg) {
   this._msg = msg;
 }

 run() {
   print(this._msg);
 }
}