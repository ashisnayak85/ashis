interface interfaceB{
void draw();
}
class rectangle implements interfaceB{
public void draw()
{
System.out.println("Drawing a rectangle");
}
}
class circle implements interfaceB{
public void draw()
{
System.out.println("Drawing a circle");
}
}
class interface2{
public static void main(String args[]){
interfaceB i=new circle();
i.draw();
}
}