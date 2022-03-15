abstract class absB{
abstract void draw();
}
class rectangle extends absB{
void draw()
{
System.out.println("Draw a rectangle");
}
}
class circle extends absB{
void draw()
{
System.out.println("Draw a circle");
}
}
class abs2{
public static void main(String args[]){
absB a=new circle();
a.draw();
}
}