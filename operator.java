public class operator{
public static void main(String args[]){
int x=10;
System.out.println(x++);//10(11)
System.out.println(++x);//12
System.out.println(x--);//12(11)
System.out.println(--x);//10
}
}
/*we know x=10.x=a++ means x=a so the valu of first print remain same with the main value and the carring valu is
 a=a+1 means 10+1=11 then ++x means the valu is addition between 1 nad carring valu of privious(a=a+1).so the 
value of second print is 12 and carring x=a means the same value then carring value is 12. in third print x-- means 
the value x=a means 12 and and carring a=a-1 then carring value is 11.In fourth one --x means a=a-1 so the value is
10 amd carring value is x=a means 10.*/