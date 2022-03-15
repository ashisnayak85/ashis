/*The code is accessible in the same package and subclasses.
 You will learn more about subclasses and superclasses in the Inheritance chapter*/
class student{
protected String fname="Ashis";
protected String lname="Nayak";
protected String gmail="ashisnayak7105@gmail.com";
protected int age=20;
}
class student extends javamodifiers5{
private int graduationYear = 2018;
public static void main(String args[]){
javamodifiers5 myobj=new javamodifiers5();
System.out.println("Name:- "+myobj.fname+" "+myobj.lname);
System.out.println("Gmail:- "+myobj.gmail);
System.out.println("Age:- "+myobj.age);
}
}