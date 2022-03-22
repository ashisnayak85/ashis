#Missing value
l=[10,15,20,30,35,40,45,50,55,60,70]
print('The missing values:-')
for i in range(10,70,5):
    if i not in l:
        l.append(i)
    l.sort()
    print(l)