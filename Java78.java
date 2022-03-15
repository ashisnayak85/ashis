//constructer overloading
class Java78{
int id;
String name;
int age;
//creating two arg constructor
Java78(int i,String n){
id=i;
name=n;
}
//creating three arg contructor
Java78(int i,String n,int a){
id=i;
age=a;
}
void display()
{
System.out.println(id+" "+name+" "+age);
}
public static void main(String args[]){
Java78 s1=new Java78(111,"karan");
Java78 s2=new Java78(222,"Aryan");
s1.display();
s2.display();
}
}