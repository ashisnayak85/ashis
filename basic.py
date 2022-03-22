# l1=[[[10,20,30],[40,50,60],[70,80,90]],[[100,200,300],[400,500,600][700,800,900]]]
# l2=[[[101,201,301],[401,501,601],[701,801,901]],[[101,201,301],[401,501,601][701,801,901]]]

# for i in range(2):
#     for j in range(3):
#         for k in range(3):
#             l1[i][j][k]=l1[i][j][k]+l2[i][j][k]
#             print(l1[i][j][k])


# string="abracadabra"
# s=list(string)
# s[5]='k' 
# print(s)
# string=''.join(s)

# print(string)

def leap_year(year):
 if((year%400==0)or(year%100!=0)and(year%4==0)):
    print("given year is a leap year")
 else:
    print("given year is not leap year")
    
year=int(input("Enter the year:"))

leap_year(year)