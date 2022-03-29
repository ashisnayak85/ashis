#for loop
#1:
# for i in range(10):
#     print("Ashis")
#2:
# for i in range(10):
#     print(i)
#3:printing the number:1 2 3 4 5 6 7 8 9 10
# for i in range(1,11,1):
#     print(i)
#4:printing the number:200,400,600,800,1000
# for i in range(200,1001,200):
#     print(i)
#5:printing the number:9 8 7 6 5 4 3 2 1
# for i in range(9,0,-1):   
#     print(i)
#6:print the number:999 777 555 333 111
# for i in range(999,110,-222):
#     print(i)
#7:Write all the even number from 100 to 120
# for i in range(100,121,2):
#     print(i)
#or
# for i in range(100,121,1):
#     if i%2==0:
#         print(i)
#8:
# name="Ashis kumar nayak"
# for i in name:
#     print(i)
#9:to print vertically
# name="ashis"
# for i in name:
#     print(i,end="")
#nested for loop

# for x in range(4):
#     for y in range(3):
#      print(f'({x},{y})')

#print using loop
#output:xxxxx
#xx
#xxxxx
#xx
#xx
# number=[5,2,5,2,2]
# for x_count in number:
#     output=''
#     for count in range(x_count):
#         output+='x'
#     print(output)
#nested for loop for drawing a pattern
#1:Square pattern
# n=int(input("Enter the number: "))
# for i in range(n):
#     for j in range(n):
#         print("*",end=' ')
#     print()
#2:increasing order
# n=int(input("Enter the number: "))
# for i in range(n):
#     for j in range(i+1):
#         print("*",end=' ')
#     print()
#3:decreasing order
# n=int(input("Enter the number: "))
# for i in range(n):
#     for j in range(i,n):
#         print("*",end=' ')
#     print()
#4:
# n=int(input("Enter the number: "))
# for i in range(n):
#     for j in range(i,n):
#         print(" ",end=' ')
#     for j in range(i+1):
#         print("*",end=' ')
#     print()
#5:
# n=int(input("Enter the number: "))
# for i in range(n):
#     for j in range(i+1):
#         print("",end=' ')
#     for j in range(i,n):
#         print("*",end="")
#     print()
#6:n=int(input("Enter the number: "))
n=int(input("Enter the number: "))
for i in range(n):
    for j in range(i,n):
        print("",end="")
    for j in range(i):
        print("*",end="")
    for j in range(i+1):
        print("*",end="")
    print()


