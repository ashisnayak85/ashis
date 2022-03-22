# a={}

# while True:
#     id=input("Enter student id: ")

#     name=input("Enter student name: ")
#     age=input("Enter student age: ")
#     course=input("Enter student course: ")
#     x=input("Do you want to insert more yes or no: ")
#     a[id]=[name,age,course]
#     if x=='no':
#         break
# print("Go to search..")
# inp=input("Enter the student id")

# if inp in a:
#     print("Student id is:",inp)

#     print("Student name",a[inp][0])
#     print("Studsent age: ",a[inp][1])
#     print("student course: ",a[inp][2])
# else:
#     print("not mentioned..")

a={}
b={}

while True:
    bf_Sno=input("Enter bf serial_no: ")
    bf_name=input("Enter the bf_name: ")

    x=input("You want to insert more yes/no: ")
    a[bf_Sno]=[bf_name]
    if x=='no':
        break
while len(b)<=len(a):
 gf_Sno=input("Enter gf serial_no: ")
 gf_name=input("Enter gf_name: ")
   len(b)=len(b)+1
   b[gf_Sno]=[gf_name]
        