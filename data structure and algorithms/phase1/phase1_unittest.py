import unittest
from phase1 import SList2

class Test(unittest.TestCase):

   	#setUp is a method which is ran before a test method is executed. 
   	#This is useful if you need some data (for example) to be present before running a test.

    def setUp(self):
        self.slist = SList2()

    # -----------------------  EJERCICIO 1  -----------------------  
        
    def test1_ej_1(self):
        print("Exercise 1, case 1: If the list empty")

        self.slist.del_largest_seq()
        #self.assertEqual(len(self.slist), 0)
        self.assertEqual(self.slist.del_largest_seq(), None)
    
    def test2_ej_1(self):
        print("Exercise 1, case 2: If the length of the list is 1")
        
        self.slist.addFirst("1")
        self.slist.del_largest_seq()
        self.assertEqual(len(self.slist), 0)
        
        
    def test3_ej_1(self):
        print("Exercise 1, case 3: Exception, when the longest sequence is at the end of the list")
        
        lista_aux = [3,3,3,4,5,6,6,6,7,7,7,7]
        for i in lista_aux:
            self.slist.addLast(i)
    
        self.slist.del_largest_seq()
        self.assertEqual(str(self.slist),"3,3,3,4,5,6,6,6")
    
    def test4_ej_1(self):
        print("Exercise 1, case 4: No restrictions, just a normal example")
        
        lista_aux = [3,3,3,4,5,6,6,6,7,7,7,7,2]
        for i in lista_aux:
            self.slist.addLast(i)
    
        self.slist.del_largest_seq()
        self.assertEqual(str(self.slist),"3,3,3,4,5,6,6,6,2")
        
    # -----------------------  EJERCICIO 2  -----------------------  

    def test1_ej_2(self):
        print("Exercise 2, case 1: There is no loop")
        
        for i in range(3):
            self.slist.addLast(i)
        
        self.slist.fix_loop()
        self.assertEqual(self.slist.fix_loop(), False)
        
    def test2_ej_2(self):
        print("Exercise 2, case 2: There is a loop pointing to the first node")
        
        for i in range(5):
            self.slist.addLast(i)
        
        self.slist.create_loop(1)
        self.assertEqual(self.slist.fix_loop(), True)
        # Si imprimimos la lista una vez creado el loop se forma un bucle infinito, 
        # lo que significa que está bien creado. Mientras que si la imprimos una vez
        # ejecutado el método vemos que imprime [0,1,2,3,4] en este caso
        # print(str(self.slist))
        
    def test3_ej_2(self):
        print("Exercise 2, case 2: There is a loop pointing to the second node")
        
        for i in range(5):
            self.slist.addLast(i)
        
        self.slist.create_loop(2)
        self.assertEqual(self.slist.fix_loop(), True)

    # -----------------------  EJERCICIO 3  -----------------------    
    
    def test1_ej_3(self):
        print("Exercise 3, case 1: If the list empty")
    
        self.assertEqual(self.slist.left_right_shift(True, 2), None)
        
    def test2_ej_3(self):
        print("Exercise 3, case 2: If n is equal to the list size")

        lista_aux = [1,2,3,4,5,6,7]
        for i in lista_aux:
            self.slist.addLast(i)
        
        self.assertEqual(self.slist.left_right_shift(True, len(lista_aux)), None)
        
    def test3_ej_3(self):
        print("Exercise 3, case 3: If n is bigger than the list size. The list doesn't change")

        lista_aux = [1,2,3,4,5,6,7]
        for i in lista_aux:
            self.slist.addLast(i)
        
        self.assertEqual(self.slist.left_right_shift(True, 10), None)

    def test4_ej_3(self):
        print("Exercise 3, case 4: No restrictions, just a normal example, but starting from the left")
        
        lista_aux = [1,2,3,4,5,6,7]
        for i in lista_aux:
            self.slist.addLast(i)
        self.slist.left_right_shift(True, 2)
        self.assertEqual(str(self.slist), "3,4,5,6,7,1,2")
        
    def test5_ej_3(self):
        print("Exercise 3, case 5: No restrictions, just a normal example, but starting from the right")
        
        lista_aux = [1,2,3,4,5,6,7]
        for i in lista_aux:
            self.slist.addLast(i)
        self.slist.left_right_shift(False, 2)
        self.assertEqual(str(self.slist), "6,7,1,2,3,4,5") 

    def test6_ej_3(self):
        print("Exercise 3, case 6: Another example starting from the right, but with more numbers")
        
        lista_aux = [1,2,3,4,5,6,7]
        for i in lista_aux:
            self.slist.addLast(i)
        self.slist.left_right_shift(False, 4)
        self.assertEqual(str(self.slist), "4,5,6,7,1,2,3") 

if __name__ == "__main__":
    unittest.main()