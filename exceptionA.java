public class exceptionA{
public static void main(String args[]){
try{
//code may rise exception
int data=100/0;
}
catch(ArithmeticException e)
{
System.out.println(e);
}
//rest code of the program
System.out.println("rest of the code");
}
}