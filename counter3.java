class counter3{
int count=0;

counter3(){
count++;
System.out.println(count);
}
public static void main(String args[]){
counter3 c1=new counter3();
counter3 c2=new counter3();
counter3 c3=new counter3();
}
}