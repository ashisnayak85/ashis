//The code is only accessible within the declared class
public class javamodifiers3{
private String fname="Ashis";
private String lname="Nayak";
private String gmail="ashisnayak7105@gmail.com";
private int age=20;
public static void main(String args[]){
javamodifiers3 myobj=new javamodifiers3();
System.out.println("Name:- "+myobj.fname+" "+myobj.lname);
System.out.println("Gmail:- "+myobj.gmail);
System.out.println("Age:- "+myobj.age);
}
}