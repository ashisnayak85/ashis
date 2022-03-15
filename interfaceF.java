interface interfaceF{
void print();
}
interface showable extends interfaceF{
void show();
}
class interface6 implements showable{
public void print()
{
System.out.println("Hello");
}
public void show()
{
System.out.println("Welcome");
}
public static void main(String args[]){
interface6 obj=new interface6();
obj.print();
obj.show();
}
}