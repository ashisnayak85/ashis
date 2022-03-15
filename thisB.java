class thisB{
int rollno;
String name;
float fee;
thisB(int rollno,String name,float fee){
this.rollno=rollno;
this.name=name;
this.fee=fee;
}
void display()
{
System.out.println(rollno+" "+name+" "+fee);
}
}
class this2{
public static void main(String args[]){
thisB t1=new thisB(111,"ashis",5000);
thisB t2=new thisB(112,"debasish",6000);
t1.display();
t2.display();
}
}