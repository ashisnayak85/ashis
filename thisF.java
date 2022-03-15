class thisF{
int rollno;
String name,course;
float fee;
thisF(int rollno,String name,String course)
{
this.rollno=rollno;
this.name=name;
this.course=course;
}
thisF(int rollno,String name,String course,float fee)
{
this(rollno,name,course);//reusing constructor
this.fee=fee;
}
void display()
{
System.out.println(rollno+" "+name+" "+course+" "+fee);
}
}
class this6{
public static void main(String arsgs[]){
thisF t1=new thisF(111,"Ankit","Java");
thisF t2=new thisF(112,"Ashis","Python",3000);
t1.display();
t2.display();
}
}