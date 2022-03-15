/*Create a car object named mycar.call the fullthrottle() and speed() method on the 
mycar object,and run the programme*/
public class oop10{
public void fullthrottle(){
System.out.println("this car is going as fast as it can");
}
public void speed(int maxspeed){
System.out.println("max speed is= "+maxspeed);
}
public static void main(String args[])
{
oop10 mycar=new oop10();
mycar.fullthrottle();
mycar.speed(200);
}
}