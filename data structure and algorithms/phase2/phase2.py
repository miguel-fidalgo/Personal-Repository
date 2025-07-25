"""
Case #2. Exercise  3
@author: EDA Team
"""

# Classes provided by EDA Team
from dlist import DList
from bintree import BinaryNode
from bst import BinarySearchTree

class BST2(BinarySearchTree):
    def find_dist_k(self, n: int, k:int) -> list:
        """Este método recibe dos parámetros n y k siendo n el elemento del nodo
        a buscar y k la distancia de la que vamos a buscar los otros nodos para
        meterlos finalmente en una lista."""

        # Primero vamos a sacar que nodo contiene a dicho elemento, para posteriormente
        # buscar su altura.
        # Para ello usaremos el método self.search() para encontrar el elemento en el árbol,
        # esto nos devolverá el nodo en el que se encuentra y después se lo pasaremos a 
        # self.depth() para que encuentre su altura.
        
        # Primero evaluaremos los casos base
        
        if not isinstance(n, int) or not isinstance(k, int):
            #print("Los elementos introducidos no son números")
            return None
        
        elif not self.root:
            #print("Error: No existe ningún árbol")
            return None
            
        elif k < 0:
            return None
        
        elif self.search(n):
            node_target = self.search(n)     # Nodo del elemento
            prof = self.depth(node_target)   # Profundidad del nodo
            lista_nodos = []
            self._find_dist_k(self._root, (k - prof), n, True, lista_nodos)
            return lista_nodos
        else:
            #print("El elemento que has indicado no está en el árbol")
            return []
        
    def _find_dist_k(self, node: BinaryNode, k: int, n: int, direccion: bool, lista_nodos: list) -> None:
        # Cuando te acercas al nodo sumas
        # Dirección True cuando voy dirección al nodo y False cuando voy en la otra dirección            
        if not node: # Porque una vez que es negativo no va a ser positivo nunca más
            return

        # Cuando vamos a buscar en la misma rama del árbol y no necesitamos saltar
        # hacia el otro lado de la raíz
        if k < 0:
            if node.elem < n:
                self._find_dist_k(node.right, k + 1, n, True, lista_nodos) # Para coger el primer nodo a la dercha
            
            else: # node.elem > n:
                self._find_dist_k(node.left, k + 1, n, True, lista_nodos)
                
        elif k == 0:
            
            lista_nodos.append(node.elem)

            if node.elem > n and direccion:
                self._find_dist_k(node.left, k + 1, n, True, lista_nodos)
            elif node.elem < n and direccion:
                self._find_dist_k(node.right, k + 1, n, True, lista_nodos)

        else: # k > 0
              
            if node.elem == n:
                direccion = False

            if node.elem > n and direccion:
                self._find_dist_k(node.left, k + 1, n, True, lista_nodos)
                self._find_dist_k(node.right, k - 1, n, False, lista_nodos)
            
            elif node.elem < n and direccion:
                self._find_dist_k(node.left, k - 1, n, False, lista_nodos)
                self._find_dist_k(node.right, k + 1, n, True, lista_nodos)
                
            else: # Cuando direccion = False
                self._find_dist_k(node.left, k - 1, n, False, lista_nodos)
                self._find_dist_k(node.right, k - 1, n, False, lista_nodos) 
            
            
def create_tree(input_tree1: BinarySearchTree, input_tree2: BinarySearchTree, opc:str) -> BinarySearchTree:
    output_tree = BinarySearchTree()

    if input_tree1 is None and input_tree2:
        return input_tree2
    elif input_tree2 is None and input_tree1:
        return input_tree1
    elif input_tree1 is None and input_tree2 is None:
        return None

    if opc != "merge" and opc != "intersection" and opc != "diference": # "en el caso de que la opción no se correcta"
        #print(opc, "not correct")
        return None

    if opc.lower() == "merge":
        merge(input_tree1._root, output_tree)
        merge(input_tree2._root, output_tree)
    
    elif opc.lower() == "intersection":
        intersection(input_tree1._root, input_tree2, output_tree)
    
    elif opc.lower() == "diference":
        diference(input_tree1._root, input_tree2, output_tree)
    
    return output_tree

def merge(node: BinaryNode, tree: BinarySearchTree):
    if node is None:
        return None
    
    if tree.search(node.elem) is None:
        tree.insert(node.elem)
    merge(node.left, tree)
    merge(node.right, tree)


def intersection(node: BinaryNode, tree: BinarySearchTree, output_tree: BinarySearchTree):
    if node is None:
        return None
    
    if tree.search(node.elem):
        output_tree.insert(node.elem)
    intersection(node.left, tree, output_tree)
    intersection(node.right, tree, output_tree)

def diference(node: BinaryNode, tree: BinarySearchTree, output_tree: BinarySearchTree):
    if node is None:
        return None
    
    if tree.search(node.elem) is None:
        output_tree.insert(node.elem)
    diference(node.left, tree, output_tree)
    diference(node.right, tree, output_tree)


# Some usage examples
if __name__ == '__main__':
    input_list_01 = [10, 5, 25, 3, 7, 17, 43, 4, 8]
    input_list_02 = [17, 13, 20, 5, 19, 25, 4, 10, 21]

    # Build and draw first tree
    tree1 = BinarySearchTree()
    for x in input_list_01:
        tree1.insert(x)
    tree1.draw()

    # Build and draw second tree
    tree2 = BinarySearchTree()
    for x in input_list_02:
        tree2.insert(x)
    tree2.draw()

    function_names = ["merge", "intersection", "diference"] # 

    for op_name in function_names:
        res = create_tree(tree1, tree2, op_name)
        print(f"-- Result for {op_name} method.")
        res.draw()
