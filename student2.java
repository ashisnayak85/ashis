//Example of static variable
class student2{
String name;
int rollno;
static String college="NALANDA";
student2(String n,int r){
name=n;
rollno=r;
}
void display(){
System.out.println(name+" "+rollno+" "+college);
}
}
class ariya{
public static void main(String args[]){
student2 s1=new student2("ariya",111);
student2 s2=new student2("ashis",112);
s1.display();
s2.display();
}
}