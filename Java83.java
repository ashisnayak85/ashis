class Java83{
static int multiply(int a,int b)
{
return a*b;
}
//method with the name but 2 double parameter
static double multiply(double a,double b)
{
return a*b;
}
}
class main{
public static void main(String args[]){
System.out.println(Java83.multiply(4,2));
System.out.println(Java83.multiply(6.5,5.5));
}
}