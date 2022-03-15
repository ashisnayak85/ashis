class superB{
void eat()
{
System.out.println("Eating");
}
}
class dog extends superB{
void eat()
{
System.out.println("Eating food");
}
void bark()
{
System.out.println("Barking");
}
void work()
{
super.eat();
bark();
}
}
class super2{
public static void main(String args[]){
dog d=new dog();
d.work();
}
}