/*The code is only accessible in the same package. This is used when you don't specify a modifier.
 You will learn more about packages in the Packages chapter*/
class javamodifiers4{
String fname="Ashis";
String lname="Nayak";
String gmail="ashisnayak7105@gmail.com";
int age=20;
public static void  main(String args[]){
javamodifiers4 myobj=new javamodifiers4();
System.out.println("Name:- "+myobj.fname+" "+myobj.lname);
System.out.println("Gmail:- "+myobj.gmail);
System.out.println("Age:- "+myobj.age);
}
}