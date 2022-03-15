class Java84{
static int mymethod(int a,int b)
{
return a*b;
}
static int mymethod(int a,int b,int c)
{
return a*b*c;
}
}
class main{
public static void main(String args[]){
System.out.println(Java84.mymethod(4,5));
System.out.println(Java84.mymethod(4,5,6));
}
}
 