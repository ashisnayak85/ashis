class thisD{
thisD(){
this(5);
System.out.println("Hello A");
}
thisD(int x){
System.out.println(x);
}
}
class this4{
public static void main(String args[]){
thisD a=new thisD();
}
}