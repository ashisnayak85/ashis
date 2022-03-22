#l1=[10,10,20,40,50]
# l2=[10,10,21]
# print('l1<l2',l1<l2)
# print('l1>l2',l1>l2)
# print('l1<=l2',l1<=l2)
# print('l1>=l2',l1>=l2)
# print('l1==l2',l1==l2)
# print('l1!=l2',l1!=l2)

#l1=['cat','apple','bat']
#l2=['dog','tiger']
#print('l1>l2',l1>l2)

#NESTED LIST
#l1=eval(input('enter the element: '))
#print(l1[3][0])
   #Q1:python program to interchange first and last element in the list
# l1=eval(input('Enter the first list '))
# a=len(l1)
# temp=l1[0]
# l1[0]=l1[a-1]
# l1[a-1]=temp
# print('After interchange',l1)

# l=[10,20,30,40]
# print(10 in l)
# print(10 not in l)



#Q2:Python program to swap two elements in a list

# l=eval(input('Enter the list '))
# a=int(input('Enter the first element for swapping '))
# b=int(input('Enter the second element for swaping '))
# print('list before updation',l)
# num1=l.index(a)
# num2=l.index(b)
# l[num1]=b
# l[num2]=a
# print('after updation',l)

#Q3:Find min and maximum value of the list

# l=eval(input('Enter the element: '))
# maximum=max(l)
# minimum=min(l)
# print(maximum)
# print(minimum)

#Q4:Sum of number digits in list

#l=eval(input('Enter the element: '))
#l1=sum(l)
#print(l1)
#Q7:find out the missing value
# l=eval(input('Enter the element : '))
# f=l[0]
# l=l[-1]
# print('The missing values are: ')
# for i in range(f,l,5):
#    if i not in l:
#     print(i)
   
# l=eval(input('Enter the element : '))
# print('Before interchange :',l)
# temp=l[0]
# l[0]=l[-1]
# l[-1]=temp
# print('After interchange :',l)

l=[10,20]
l.append('ashis')
l.append('nayak')
print(l)

                                             #2:TUPLE
# Creation of tuple
# l=(10,20,30)
# print(l)

# 1:We can create a tuple with out paranthesis
# l=10,20,30,40
# print(l)

#'Type' of Tuple 
# l=10,20,30,40
# print(type(l))

# if we give only a single value with out using ',' the type of class will be 'int'
# l=10
# print(type(l))

# 2:We can create a tuple by using tuple function 'tuple()'
# l=[1,2,3,4]
# t=tuple(l)
# print(t)

# 3:we can create tuple function 'tuple()' by using range() function
# l=tuple(range(10,20,2))
# print(l)

# How to access element of tuple
    #1.By using index
# t=(1,2,3,4,5)
# print(t[0])
# print(t[1])
# print(t[2])
# print(t[155])

   #2.By using slice operator
# t=(1,2,3,4,5)
# print(t[1:5:2]) 

   # 3.By using mathematical operator  

   # '+'
# t1=(10,20,30,40)
# t2=(1,2,3,4)
# t3=t1+t2
# print(t3)

   # '*'
# t1=(10,20,30,40)

# t2=t1*3
# print(t2)

   # count()
#t=(1,2,3,4,5,6,6,5,)# count function is used to take the number of duplicate present in the 
#tuple
#print(t.count(6))   

#len()
# t=(1,2,3,4,5,6)
# print(len(t))

#index()
# t=(1,2,3,4,5,6)#index represent the position of the element present in tuple
# print(t.index(4))

#sorted()
# t=(1,2,3,4,5,6,7,8,9,13,90,67,45,23)
# t1=sorted(t)
# print(t1)

#min() and max()
# t=(1,2,3,44,5,5,666,6,78)
# print(min(t))
# print(max(t))


#tuple packing and unpacking
#tuple packing
# a=12
# b=13
# c=14
# t=a,b,c
# print(t)

#tuple unpacking
# t=(1,2,3,4,5)
# a,b,c,d,e=t
# print('a',a)
# print('b',b)
# print('c',c)
# print('d',d)
# print('e',e)

                             #SET
#creation of 'set'

#1:Direct creation
# s={1,2,3,4,5}
# print(s)
# print(type(s))

#2:Ctreate set using set() function

# s=set(range(10))
# print(s)

#3:Create set object from list
# a=[1,2,3,4,5]
# print(set(a))

#4:In set duplicate element are not present
# a=[1,2,3,4,5,6,3,4,7,8,2,3]
# print(set(a))
 
 
 #Q Wap to enter bf name an gf name store

# dict={}
# a=int(input('Enter How many name you want to insert: '))
# i=1
# while i<=a:
#   bfname=input('enter your bf name')
#   gfname=input('enter your gf name')
#   dict[bfname]=gfname
#   i=i+1

# print("bf",""," gf")
# for i in dict:
#     print(i,"",dict[i])
