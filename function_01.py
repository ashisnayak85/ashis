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