class shape{
void draw()
{
System.out.println("Drawing");
}
}
class rectangle extends shape{
void draw()
{
System.out.println("Draw a rectangle");
}
}
class circle extends shape{
void draw()
{
System.out.println("Draw a circle");
}
}
class triangle extends shape{
void draw()
{
System.out.println("Draw a triangle");
}
}
class amit{
public static void main(String args[]){
shape s;
s=new rectangle();
s.draw();
s=new circle();
s.draw();
s=new triangle();
s.draw();
}
}