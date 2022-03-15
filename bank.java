class bank{
float gaterateofinterest()
{
return 0;
}
}
class AXIS extends bank{
float gaterateofinterest()
{
return 8.4f;
}
}
class SBI extends bank{
float gaterateofinterest()
{
return 7.3f;
}
}
class ICICI extends bank{
float gaterateofinterest()
{
return 9.7f;
}
}
class debasish
{
public static void main(String args[])
{
bank b;
b=new AXIS();
System.out.println("The rate of interest of AXIS:"+b.gaterateofinterest());
b=new SBI();
System.out.println("The rate of interest of SBI:"+b.gaterateofinterest());
b=new ICICI();
System.out.println("The rate of interest of ICICI:"+b.gaterateofinterest());
}
} 