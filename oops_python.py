#1:create class using 'class' keyword
# class myclass:
#     x=5
# print(myclass)

#2:create object
# class myclass:
#     x=5
# p1=myclass()
# print(p1.x)

#3:
# class person:
#   def __init__(self,name,age):
#       self.name=name
#       self.age=age
# p1=person("John",32)

# print(p1.name)
# print(p1.age)

#4:
# class person:
#     def __init__(self,name,age):
#         self.name=name
#         self.age=age

#     def myfun(self):
#         print("Hello my name is: "+self.name)
# p1=person('ashis',36)
# p1.myfun()

#5:The self Parameter
#The self parameter is a reference to the current instance of the class, and is used to
#  access variables that belongs to the class.

#It does not have to be named self , you can call it whatever you like, but it has to be
#  the first parameter of any function in the class:
# class person:
#     def __init__(first,name,age):
#         first.name=name
#         first.age=age

#     def myfun(abc):
#         print("Hello my name is:"+abc.name)

# p1=person("john",36)
# p1.myfun()

#modify the object properties

# class person:
#     def __init__(self,name,age):
#         self.name=name
#         self.age=age

#     def myfun(self):
#         print("Hello my name is "+self.name)

# p1=person("john",34)
# p1.age=40
# print(p1.age)