public class oop14{
int modelyear;
String modelname;
public oop14(int year,String name){
modelyear=year;
modelname=name;
}
public static void main(String args[]){
oop14 mycar=new oop14(1969,"mustang");
System.out.println(mycar.modelname+" "+mycar.modelyear);
}
}
