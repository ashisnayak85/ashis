class thisA{
int rollno;
String name;
float fee;
thisA(int rollno,String name,float fee)
{
rollno=rollno;
name=name;
fee=fee;
}
void display()
{
System.out.println(rollno+" "+name+" "+fee);
}
}
class this1{
public static void main(String args[]){
thisA t1=new thisA(111,"Ashis",5000);
thisA t2=new thisA(112,"Debasish",6000);
t1.display();
t2.display();
}
}
