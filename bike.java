//java runtime polymorphism with data member
class bike{
int speed=90;
}
class Hero extends bike{
int speed=150;
public static void main(String args[]){
bike obj=new Hero();
System.out.println(obj.speed);
}
}