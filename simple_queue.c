#include<stdio.h>
#define size 5
int que[size],rear=-1,front=-1;
void enque(int val)
{
	if(rear==size-1)
	{
		printf("Queue is full \n");
	}
	else
	{
		rear++;
		que[rear]=val;
		printf("%d enqued sucessfully\n",val);
	}
}
void deque()
{
	if(rear==-1)
	{
		printf("The queue is empty\n");
	}
	else
	{
		front++;
		printf("%d dequed sucessfully\n",que[front]);
	}
}
void display()
{
	if(rear==front)
	{
		printf("Queue is empty\n");
	}
	else
	{
		int i;
		for(i=front+1;i<=rear;i++)
		{
			printf("%d",que[i]);
		}
	}
}
int main()
{
	int choice,ele;
	while(1)
	{
		printf("Enter your choice :\n");
		printf("1.enque\n 2.deque\n 3.display\n 4.exit\n");
		scanf("%d",&choice);
		switch(choice)
		{
			case 1:
				printf("Enter the element you want to insert\n");
				scanf("%d",&ele);
				enque(ele);
				break;
			case 2:
			    deque();
			    break;
			case 3:
				display();
				break;
			case 4:
			    exit(0);
				break;	
		}
	}
	return 0;
}
