# -*- coding: utf-8 -*-
"""
Test program comparing solutions with the builtin list-based one.

@author: EDA Team
"""

# Classes provided by EDA Team
from phase2 import BST2
from phase2 import create_tree
from bst import BinarySearchTree
import unittest

class Test(unittest.TestCase):
    def setUp(self):
        self.tree = BST2()
        input_list = [14,11,13,12,10,5,4,6,2,1,3,8,7,9,18,16,15,17,
            19,30,29,24,23,21,20,22,25,27,26,28,31,33,32,34,36,35,37]
        input_list_01 = [10, 5, 25, 3, 7, 17, 43, 4, 8]
        input_list_02 = [17, 13, 20, 5, 19, 25, 4, 10, 21]
    
        for x in input_list:
            
            self.tree.insert(x)
            
        self.tree1 = BinarySearchTree()
        for x in input_list_01:
            self.tree1.insert(x)

        # Build and draw second tree
        self.tree2 = BinarySearchTree()
        for x in input_list_02:
            self.tree2.insert(x)
            
    def test_test01(self):
        print("Exercise 1, case 1: If the lenght negative")
        
        self.assertEqual(self.tree.find_dist_k(10, -1), None)
    
    def test_test02(self):
        print("Exercise 1, case 2: One of the parameters is not a number")
        
        self.assertEqual(self.tree.find_dist_k("texto", -1), None)
        
    def test_test03(self):
        print("Exercise 1, case 3: If the element is not in the tree")
        self.assertEqual(self.tree.find_dist_k(100, 0), [])

    def test_test04(self):
        print("Exercise 1, case 4: If the lenght is 0")
        self.assertEqual(self.tree.find_dist_k(24, 0), [24])
    
    def test_test05(self):
        print("Exercise 1, case 5: Any other case")
        self.assertEqual(self.tree.find_dist_k(17, 7), [4,6,23,25,32,34])
        
        
    def test_test06(self):
        print("Exercise 1, case 6: Any other case 2")
        self.assertEqual(self.tree.find_dist_k(12, 6), [2,8,15,17,30])
        
    # ------------------EJERCICIO 2---------------------
    
    def test_merge01(self):
        print("Exercise 2 merge, case 1: Both of the trees are none")
        self.assertEqual(create_tree(None, None, "merge"), None)
        
    def test_merge02(self):
        print("Exercise 2 merge, case 2: One of the trees is none")
        self.assertEqual(create_tree(self.tree1, None, "merge"), self.tree1)
    
    def test_merge03(self):
        print("Exercise 2 merge, case 3:  The option is not correct")
        self.assertEqual(create_tree(self.tree1, self.tree2, "text"), None)
        
    def test_merge04(self):
        print("Exercise 2 merge, case 4: Everything is correct")
        
        print("Árbol 1:")
        self.tree1.draw()
        print("Árbol 2:")
        self.tree2.draw()

        print("Trees merged")
        output = create_tree(self.tree1, self.tree2, "merge")
        output.draw()

        expected = BinarySearchTree()
        for x in [10, 5, 25, 3, 7, 17, 43, 4, 8, 13, 20, 19, 21]:
            expected.insert(x)
        self.assertEqual(output, expected)
        
    # ---------INTERSECTION----------
        
    def test_intersection01(self):
        print("Exercise 2 intersection, case 1: One of the trees is none")
        self.assertEqual(create_tree(None, None, "intersection"), None)

    def test_intersection02(self):
        print("Exercise 2 intersection, case 2: Both of the trees are none")
        self.assertEqual(create_tree(self.tree1, None, "intersection"), self.tree1)
    
    def test_intersection03(self):
        print("Exercise 2 intersection, case 3: The option is not correct")
        self.assertEqual(create_tree(self.tree1, self.tree2, "text"), None)
        
    def test_intersection04(self):
        print("Exercise 2 intersection, case 4: Everything is correct")
        
        print("Árbol 1:")
        self.tree1.draw()
        print("Árbol 2:")
        self.tree2.draw()

        print("Trees intersected")
        output = create_tree(self.tree1, self.tree2, "intersection")        
        output.draw()

        expected = BinarySearchTree()
        for x in [10, 5, 25, 17, 4]:
            expected.insert(x)
        self.assertEqual(output, expected)
    
    # ----------DIFFERENCE---------
    
    def test_difference01(self):
        print("Exercise 2 difference, case 1: One of the trees is none")
        self.assertEqual(create_tree(None, None, "diference"), None)

    def test_difference02(self):
        print("Exercise 2 difference, case 2: Both of the trees are none")
        self.assertEqual(create_tree(self.tree1, None, "diference"), self.tree1)
    
    def test_difference03(self):
        print("Exercise 2 difference, case 3: The option is not correct")
        self.assertEqual(create_tree(self.tree1, self.tree2, "text"), None)
        
    def test_difference04(self):
        print("Exercise 2 difference, case 4: Everything is correct")
        
        print("Árbol 1:")
        self.tree1.draw()
        print("Árbol 2:")
        self.tree2.draw()

        print("Trees diference")
        res = create_tree(self.tree1, self.tree2, "diference")        
        res.draw()

        expected = BinarySearchTree()
        for x in [3, 7, 8, 43]:
            expected.insert(x)
        self.assertEqual(res, expected)

# Some usage examples
if __name__ == '__main__':
    unittest.main()

