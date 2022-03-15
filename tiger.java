class tiger{
void eat()
{
System.out.println("eating..");
}
}
class dog extends tiger{
void bark()
{
System.out.println("barking");
}
}
class bebydog extends dog{
void weep()
{
System.out.println("weeping..");
}
}
class ariya{
public static void main(String args[]){
bebydog b=new bebydog();
b.eat();
b.bark();
b.weep();
}
}
