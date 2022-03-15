class superA{
String color="white";
}
class dog extends superA{
String color="black";
void printcolor(){
System.out.println(color);//print color of dog class 
System.out.println(super.color);//print color of superA class
}
}
class super1{
public static void main(String args[]){
dog d=new dog();
d.printcolor();
}
}