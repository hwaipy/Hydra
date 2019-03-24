region('center', 1, 4)
sides = [i for i in range(0,10) if i !=5]
for side in sides:
    region('sides', -99+side*20, -96+side*20)
region('sides', color(0.00, 0.400, 0.400))

center = region('center')
sides = region('sides')
ratio = center / sides * 9
title('Coincidence & HOMI')
display('{}\n{}'.format(counts()[0], ratio))
