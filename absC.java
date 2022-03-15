abstract class absC{
abstract int groi();
}
class axis extends absC{
int groi()
{
return 7;
}
}
class sbi extends absC{
int groi()
{
return 8;
}
}
class abs3{
public static void main(String args[]){
absC a;
a=new axis();
{
System.out.println("the rate of interest of AXIS:"+a.groi()+"%");
}
a=new sbi();
{
System.out.println("The rate of interest of SBI:"+a.groi()+"%");
}
}
}