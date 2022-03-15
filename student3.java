class student3{
String name;
int rollno;
static String college="NALANDA";
static void change()
{
college="GITA";
}
student3(String n,int r)
{
name=n;
rollno=r;
}
void display()
{
System.out.println(name+" "+rollno+" "+college);
}
}
class lopa{
public static void main(String args[]){
student3.change();
student3 s1=new student3("Ashis",111);
student3 s2=new student3("debasish",112);
student3 s3=new student3("srideb",113);
student3 s4=new student3("Abinash",114);
s1.display();
s2.display();
s3.display();
s4.display();
}
}