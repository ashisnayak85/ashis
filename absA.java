abstract class absA{
abstract void run();
}
class Honda4 extends absA{
void run()
{
System.out.println("running safely");
}
public static void main(String args[]){
absA a=new Honda4();
a.run();
}
}