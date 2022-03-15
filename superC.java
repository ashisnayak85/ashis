class superC{
superC()
{
System.out.println("parent class is created");
}
}
class dog extends superC{
dog(){
super();
System.out.println("child class is created");
}
}
class super3{
public static void main(String args[]){
dog d=new dog();
}
}