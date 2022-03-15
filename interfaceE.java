interface interfaceE{
void print();
}
interface showable{
void print();
}
class interface5 implements interfaceE,showable{
public void print()
{
System.out.println("Hello");
}
public static void main(String args[]){
interface5 i=new interface5();
i.print();
}
}