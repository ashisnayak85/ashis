public class oop9{
static void mystaticmethod(){
System.out.println("Ashis is a good boy");
}
public void mypublicmethod(){
System.out.println("Ashis is not a good boy");
}
public static void main(String args[]){
mystaticmethod();
oop9 myobj=new oop9();
myobj.mypublicmethod();
}
}