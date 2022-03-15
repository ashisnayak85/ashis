//java programme to illustrate the use of break statement
//inside the inner loop
public class Java65{
public static void main(String args[]){
//outer loop
for(int i=1;i<=3;i++)
{
for(int j=1;j<=3;j++)
{
if(i==2 && j==2)
{
break;
}
System.out.println(i+" "+j);
}
}
}
}