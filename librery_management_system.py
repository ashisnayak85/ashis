def add_book():
    bn=input("Enter the book name: ")
    c=input("Enter the book code: ")
    t=input("Enter the total book: ")
    s=input("Enter the subject: ")
    a=(bn,c,t,s)
    print(">-------------------------------------------------------------------------<")
    print("Data entered sucessfully")
    main()
def issue_book():
    n=input("Enter name: ")
    r=input("Enter reg No:")
    co=input("Enter book code: ")
    d=input("Enter date: ")
    data=(n,r,co,d)
    print(">--------------------------------------------------------------------------<")
    print("Book issued to: ",n)
def submit_book():
    n=input("Enter the name: ")
    r=input("Enter the reg no: ")
    co=input("enter the book code")
    d=input("Enter date: ")
    data=(n,r,co,d)
    print(">--------------------------------------------------------------------------<")
    print("Book sumitted from:",n)
def bookup(co,u):






    main()
def dbook():
    ac=input("Enter the book code: ")
    




    main()
def display_book():




    main()
def main():
    print("""
                                   -:LIBRERY MANAGER:-
    1.ADD BOOK
    2.ISSUE BOOK
    3.SUBMIT BOOK
    4.DISPLAY BOOK
    """)
    choice=input("Enter the task No:")
    print(">---------------------------------------------------------------------<")
    if(choice=='1'):
     add_book()
    elif(choice=='2'):
     issue_book()
    elif(choice=='3'):
     submit_book()
    elif(choice=='4'):
     display_book()
    else:
     print("Wrong choice.....")
     main()


def password():
 pwd="ashis9090"
 p=input("Enter the password: ")
 if pwd==p:
     main()
 else:
     print("Wrong password")
     password()
password()

