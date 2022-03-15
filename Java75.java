class Java74{
int id;
String name;
}
class Java75{
public static void main(String args[]){
Java74 myobj1=new Java74();
Java74 myobj2=new Java74();
myobj1.id=101;
myobj1.name="Ashis";
myobj2.id=102;
myobj2.name="Debasish";
System.out.println(myobj1.id+" "+myobj1.name);
System.out.println(myobj2.id+" "+myobj2.name);
}
}