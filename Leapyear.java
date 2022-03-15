public class Leapyear{
public static void main(String args[]){
int A=2020;//declaring a variable
if(((A%400==0)&&(A%100!=0))||(A%4==0))
{
System.out.println("Yes it is leap year");
}
else
{
System.out.println("No it is not leap year");
}
}
}