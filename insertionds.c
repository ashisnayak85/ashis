#include<stdio.h>
#include<conio.h>
void main()
{
int a[5];    
int i;
{
	printf("Enter the 1st element of the array");
	scanf("%d\n",&a[0]);
	printf("Enter the 2nd element of the array\n");
	scanf("%d\n",&a[1]);
	printf("Enter the 3rd element of the array\n");
	scanf("%d\n",&a[2]);
	printf("Enter the 4th element of the array\n");
	scanf("%d\n",&a[3]);
	printf("Enter the 5th element of the array\n");
	scanf("%d\n",&a[4]);
    
    for(i=0;i<5;i++)
    {
	printf("the value we inserted {%d}\n",a[i]);
	}
    }
}
