interface interfaceC{
float rateofinterest();
}
class sbi implements interfaceC{
public float rateofinterest()
{
return 9.15f;
}
}
class pnb implements interfaceC{
public float rateofinterest()
{
return 9.7f;
}
}
class interface3{
public static void main(String args[]){
interfaceC i=new sbi();
System.out.println("The rate of interest:"+i.rateofinterest());
}
}