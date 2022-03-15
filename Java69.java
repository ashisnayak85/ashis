public class Java69{
int modelyear;
String modelname;

public Java69(int year,String name){
modelyear=year;
modelname=name;
}
public static void main(String args[]){
Java69 myobj=new Java69(1969,"Texa");
System.out.println(myobj.modelyear+" "+myobj.modelname);
}
}