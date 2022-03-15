class Java86{
void print()
{
System.out.println("ashis");
}
}
class subclass1 extends Java86{
void print()
{
System.out.println("Debasish");
}
}
class subclass2 extends Java86{
void print(){
System.out.println("Srideb");
}
}
class bablue{
public static void main(String args[]){
Java86 a;
a=new subclass1();
a.print();
a=new subclass2();
a.print();
}
}