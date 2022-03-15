interface interfaceD{
void print();
}
interface showable{
void show();
}
class interface4 implements interfaceD,showable{
public void print()
{
System.out.println("Hello");
}
public void show()
{
System.out.println("Welcome");
}
public static void main(String args[])
{
interface4 obj=new interface4();
obj.print();
obj.show();
}
}