class Java81{
float salary=40000;
}
class Java82 extends Java81{
int bonous=10000;
public static void main(String args[]){
Java82 b=new Java82();
System.out.println("Salary:"+b.salary);
System.out.println("bonous:"+b.bonous);
}
}