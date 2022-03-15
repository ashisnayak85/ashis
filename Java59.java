public class Java59{
static int mymethodint(int x,int y){
return x+y;
}
static double mymethoddouble(double x,double y){
return x+y;
}
public static void main(String args[]){
int num1=mymethodint(20,40);
double num2=mymethoddouble(30,50);
System.out.println("int: "+num1);
System.out.println("double: "+num2);
}
}