class Java76{
int rollno;
String name;

void insertrecord(int r,String n){
rollno=r;
name=n;
}

void displayinformation(){
System.out.println(rollno+" "+name);
}
}
class Java77{
public static void main(String args[]){
Java76 s1=new Java76();
Java76 s2=new Java76();
s1.insertrecord(111,"Ashis");
s2.insertrecord(222,"Ariya");
s1.displayinformation();
s2.displayinformation();
}
}