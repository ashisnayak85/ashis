abstract class absD{
absD()
{
System.out.println("bike is created");
}
abstract void run();
void changegear()
{
System.out.println("Gear change..");
}
}
class honda extends absD{
void run()
{
System.out.println("running safely");
}
}
class abs4{
public static void main(String args[]){
absD a=new honda();
a.run();
a.changegear();
}
}