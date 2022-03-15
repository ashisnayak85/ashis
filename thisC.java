class thisC{
thisC()
{
System.out.println("Hello a");
}
thisC(int x){
this();
System.out.println(x);
}
}
class this3{
public static void main(String args[]){
thisC a=new thisC(10);
}
}