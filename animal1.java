class animal1{
void eat()
{
System.out.println("eating");
}
}
class dog extends animal1{
void eat()
{
System.out.println("eating fruit..");
}
}
class babydog extends dog{
void eat()
{
System.out.println("Drinking milk");
}
}
class don{
public static void main(String args[]){
animal1 a1,a2,a3;
a1=new animal1();
a2=new dog();
a3=new babydog();
a1.eat();
a2.eat();
a3.eat();
}
}