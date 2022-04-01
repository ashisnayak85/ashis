                                    #function
#There are mainly two types of function
# 1:user-defined function:The user-defined function is defined by the 'user' to perform a specific task. 
# 2:Built_in_function:It is those function which is predefined by 'python'

#                           Advantage of Functions in Python

#There are following advantages of python function
#1:Using function ,we can avoid rewriting the same logic/code again and again in a program.
#2:We can call python function multiple times in a program and anywhere in a program.
#3:We can track large Python program easily when it is divided in to multiple function.
#4.Reusability is the main achivement of python functions.
#5.However ,function calling is always overhead in a python program.

# def my_function():#function defination
#1:without prameter without return type
#   print("Hello world! ")

# my_function()#function calling
# my_function()
# my_function()
# my_function()

#2:without parameter with treturn type
   #defining function
# def sum():
#     a=12
#     b=23
#     c=a+b
#     return c

# print("The sum is:",sum())

#3:with parameter without return type
# def fun(name):
#     print("Hii",name)

# fun("Ashis")

#4:Python function can calculate the sum of two variable
#define the function

# def fun(a,b):
#  return a+b
  
# #taking value from user
# a=int(input("Enter a: "))
# b=int(input("Enter b: "))

# print("sum: ",fun(a,b))

#5:Call by reference in Python

#difining the function

# def change_list(list1):
#    list1.append(20)
#    list1.append(30)
#    print("Print inside the function=",list1)

# #defining the list
# list1=[10,30,40,50]

# #calling the function
# change_list(list1)
# print("list outside the function= ",list1)   

#6:Passing mutable object(string)
#defining the function

# def change_string(str):
#    str=str+" Hows you"
#    print("printing the string inside the function:",str)
# string1="Hi i am there"
# #calling the function
# change_string(string1)

# print("printing the string outside the function:",string1)

#Class question
#1:create a function which will perform addition operation

# def add():
#    a=int(input("enter a value: "))
#    b=int(input("enter b value: "))
#    c=a+b
#    print(c)
# add()

#2:create a function which will perform square  operation of a number.

# def square():
#    a=int(input("Enter a value: "))
#    b=a*a
#    print(b)
# square()   

#3:create a function take your name and gf_name .and joint both using function
# def display():
#    bf_name=input("Enter your bf_name: ")
#    gf_name=input("Enter your gf_name: ")
#    result=bf_name+gf_name
#    print("after joint of ",bf_name,"and",gf_name,"is",result)
# display()

#DIFFERENT CATAGORIES OF FUNCTION:-
#1:function with no argument and no return value

# def fun():
#    a,b=10,20
#    c=a+b
#    print(c)
# fun()   

#2:function with no argument with return value
# def fun():
#    a,b=10,20
#    return a+b,b%a
# print(fun())   

#3:function with argument and no return value
# def fun(a,b):
#    c=a+b
#    print(c)
# fun(10,20)

#4:function with argument and with return value
# def fun(a,b):
#    c=a+b
#    return c
# print(fun(10,20))

#Note:In c,java and other some language one function can return only 1 value but in python 1 function can return
#multiple value....

                                 #ARGUMENT
#Argument can be classified into 4 types
#1:positional argument
#2:keyword argument
#3:default argument
#4variable length argument

#1:positonal argument
# def add(x,y):
#    print(x+y)
# add(-500,400)
# add(400,-500)

# def sub(x,y):
#    print(x-y)
# sub(-500,400)
# sub(400,-500)

# def fun(x,y):
#    print(x)
# fun(500)#Error:-missing anpositional argument

#2:Keyword argument
#ex:1
# def fun(x,y):
#    print(x+y)
# fun(y=10,x=30)

#ex:2
# def display(a,wish):
#    print('hii',a,wish)
# display(a='hello',wish='good_morning')
# display(wish='good_morning',a='hello')

#ex:3 Is it valid
# def wish(name,msg):
#    print('hyy',name,msg)
# wish('ashis',msg='good_morning')

#ans:Yes it is valid

#Is it valid?
# def wish(name,msg):
#    print('hyy',name,msg)
# wish(msg='good_morning','ashis')#error:positional argument followes keyword argument
#ans:No it is not valid

#3:default argument:if we want we can add default value to our positional arguments

#ex:1
# def wish_me(name="unknown"):
#    print("hello",name)
# wish_me('Ashis')
# wish_me()#Here if we will not pass any value then it will consider the default value

#ex:2
# def wish_me(msg,name="unknown"):
#    print('hello',msg,name)
# wish_me('ashis',msg='thanks')#error:-Here got multiple value for argument 'msg'

#ex:3
# def wish_me(msg,name='unknown'):
#    print('hello',name,msg)
# wish_me(msg='thanks','ashis')#positional argument follows key-word argument

#note:-After default argument we can not use non-default argument

#4:Variable length argument:-if we want to pass variable number of arguments to our function then that argument 
#is called as variable length arguments
#Herein variable length argument we will use *symbol
#ex:1
# def fun(*n):
#    print(n)
# fun()#()
# fun(10)#(10,)
# fun(10,20,30,40)#(10,20,30,40)

#Q1:find out the sum of all number based on the argument
# def fun(*n):
#    total=0
#    for i in n:
#       total=total+i
#    print(total)
# fun()
# fun(11,22)
# fun(11,22,1)
# fun(1,2,3,4)
#note:-we can use variable length argument along with positional argument

#ex:2
# def fun1(s,*n):
#    print(s)
#    print(n)
# fun1(10,20,30,40)

#ex:3
# def fun1(s,*n):
#    print(s)
#    print(n)
#    for i in n:
#       print(i)
# fun1(1,2,3,'a','b','c')

#ex:4
# def fun1(*n,s):
#    print(n)
#    print(s)
# fun1(10,20,30,s=101)

#ex:5
# def fun1(*n,s):
#    print(n)
#    print(s)
# fun1(s=101,10,20,30)#error:positional argument followes key word argument

#keyword variable length argument
#1:Here we will use **
#2:Here we will pass any number of keyword arguments,it will iternally stores argument in dict format

#ex:1
# def fun(**n):
#    print(n)
# fun(a=10,b=20,c=30)

# Indentation is used to identify code blocks
 
#logical program of function
#function to count average mark
# def find_average_mark(marks):
#    sum_of_marks=sum(marks)
#    total_subject=len(marks)
#    average_mark=sum_of_marks/total_subject
#    return average_mark

# #calculate the grade and return it
# def compute_grade(average_mark):
#    if average_mark>=80:
#       grade ='A'
#    elif average_mark>=60:
#       grade='B'
#    elif average_mark>=50:
#       grade='C'
#    else:
#       grade='F'
#    return grade
# marks=[50,60,30,70,80]
# average_mark=find_average_mark(marks)
# print("Your average mark is:",average_mark)

# grade=compute_grade(average_mark)
# print("your grade is:",grade)

#WITHOUT USING LOOP FUNCTION WE WRITE PROGRAME USING RECURSION
#1:factorial of a number
# def fact(num):
#    if num==0:
#       return 1
#    else:
#       result=num*fact(num-1)
#    return result
# print(fact(5))

#2:fibonacii number
# def fib(num):
#    if(num<=1):
#       return num
#    return(fib(num-1)+fib(num-2))
# print(fib(6))

#3:print 1 to 10
# def fun(num):
#    print(num)

#    if(num==10):
#       return
#    fun(num+1)
# fun(1)

#4:print 10 to 1 using recursion
# def fun(n):
#    print(n)
#    if n==1:
#       return
#    fun(n-1)
# fun(5)