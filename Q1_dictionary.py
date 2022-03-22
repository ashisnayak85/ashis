#Q:-create a empty dictionary and add value individually.
# d={}
# d[11]="surendra"
# d[12]="priyanka"
# d[13]="Ashis"
# d[14]="subash"
# print(d)

# d={11:'Ashis',12:'Debasish',13:'Srideb',14:'Abinash',15:'sanskar'}
# print(d)

# #Acess data from dictionary
# d={11:'surendra',12:'priyanka',13:'rahul'}
# print(d)
# print(d[11])
# print(d[12])

#1 The out put of the given code
# d={"namee":"priyanka","age":23}
# print(d)
# print(d["name"])#Here 'name' is not a key

# d={11:'surendra',12:'priyanka',13:'rahul'}
# print(d[78])#It give key error(Here such key is not present)

# d={10:"ashis",20:"debasish",30:'akash',40:'srima'}
# if 40 in d:
#     print("sucess")
# else:
#     print('fail')

#Q1: wap to enter bf name and gf name store in dict and display in output screen.
# dict={}

# a=int(input("Enter the size of the dictionary:"))
# i=1
# while i<=a:
#     bfname=input("Enter your bf name:")
#     gfname=input("Enter the gf name:")
#     dict[bfname]=gfname
#     i=i+1
# print('bf',' ','gf')
# for i in dict:
#  print(i," ",dict[i])

#update

# dict={10:'surendra',20:'priyanka',30:'amit'}
# print('before update the values are:',dict)
# dict[20]='ashis'
# print('after update the values are:',dict)

#Out put of the below code
# dict={10:'surendra',20:'priyanka',30:'amit'}
# dict[999]='lime'#here if the key is not available in the given dict then it added the specified key and value 
# print(dict)

#-> 
# dict={10:'surendra',20:'priyanka',30:'amit'}
# dict[999]=""#It is also added
# print(dict)
#->
# dict={10:'surendra',20:'priyanka',30:'amit'}
# dict[""]=""#It is also added
# print(dict)

#->
# dict={10:'surendra',20:'priyanka',30:'amit'}
# dict[]=""
# print(dict)# the out put will be 'invalid syntax'

#How to delete a key from a dict

#1st way(By using 'del')

# dict={10:'surendra',20:'priyanka',30:'amit'}
# del dict[20]
# print(dict)#note:-Here key delete means key along with specified value

# dict={10:'surendra',20:'priyanka',30:'amit'}
# del dict[55]
# print(dict)# Here "key error" will be shown in the output.because the key is not present in the dict

#2nd way (Here will delete the entire dict by using clear())

# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict)
# dict.clear()
# print(dict)# here the entire key and values are deleted accept the dictionary.means empty dictionary is present

#3rd way(Here the entire dict will be deleted)

# dict={10:'surendra',20:'priyanka',30:'amit'}
# print('before del',dict)
# del dict
# print(dict)

                                 #function inside the dict

#1.len()
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(len(dict))            

#2.get()
#it is used to get the value 
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.get(10))   

#->
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.get(999))#here the key is not present so the value comes by default 'none'

#4:popiteam()
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.popitem())#30:'amit'
# print(dict)

#->
# dict={10:'surendra',20:'priyanka',30:'amit'}
# dict[999]='lime'
# print(dict.popitem())
# print(dict)

#5:keys()
#It will return all the key from dict
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.keys())

#Q:how to print each key individually
#Ans: Using loop
# dict={10:'surendra',20:'priyanka',30:'amit'}
# for i in dict.keys():
#     print(i)

#6:values():-
#It will return all the values from dict
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.values())

#Q:how to print each values individually
#Ans: Using loop

# dict={10:'surendra',20:'priyanka',30:'amit'}
# for i in dict.values():
#     print(i)

#7.items()
#It will return the list of tuples as the key value pairs

# dict={10:'surendra',20:'priyanka',30:'amit'}
# print(dict.items())

#->
# dict={10:'surendra',20:'priyanka',30:'amit'}
# print("key\t values")
# print("----------")
# for key in dict.items():
#     print(key)

#8.update()
#d1.update(d2)
#Here all the iteam present in d1 will be added in d2 ans shown in d1
# d1={10:'surendra',20:'priyanka',30:'amit'}
# d2={40:'surendra',50:'priyanka'}
# print(d1)
# print(d2)
# d1.update(d2)
# print(d1)
# print(d2)

#9.copy()
#It is used to create duplicate dict
# d1={10:'surendra',20:'priyanka',30:'amit'}
# d2=d1.copy()
# print(d2)

