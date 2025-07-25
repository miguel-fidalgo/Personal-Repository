from slistH import SList
from slistH import SNode

class SList2(SList):
    """Del fichero slistH importamos la clase SList, que es la que longiene los
    métodos de la TAD Lista con la referencia _head."""

    # ----------------------------PRIMER EJERICIO------------------------------
    
    def del_largest_seq(self) -> None: 

        if self.isEmpty():
            return

        elif len(self) == 1:
            self.removeFirst()
            return
       
        node_it = self._head
        actual_long, max_long = 1, 1
        actual_index, max_index = 0, 0

        while node_it.next:
            actual_num = node_it.elem
            node_it = node_it.next

            if actual_num == node_it.elem:  # si el anterior es igual al siguiente
                actual_long += 1
    
            else:
                if actual_long >= max_long:
                    max_long = actual_long
                    max_index = actual_index - actual_long + 1
                actual_long = 1

            actual_index += 1

        # Este código es para una excepción: cuando la secuencia más larga está
        # al final de la lista no entra a evaluarla porque node_it.next es None
        # por lo que no entra en el bucle y no puede acceder al else
        if actual_long >= max_long:
            max_long = actual_long
            max_index = actual_index - actual_long + 1
        
        # Ahora borramos la mayor secuencia de elementos
        previous, actual = self._head, self._head
        for _ in range(max_index - 1): # Para conseguir el anterior
            previous = previous.next          
            
        for _ in range(max_index + max_long): # Para conseguir el último
            actual = actual.next
        
        # De esta forma la secuencia será eliminada por el recolector de basura
        # ya que no existe ningún nodo que enlace con ellos.
        previous.next = actual
        # COMPLEJIDAD LINEAL O(n)
            
    # ----------------------------SEGUNDO EJERICIO-----------------------------

    def fix_loop(self) -> bool:
        # En esta primera parte, detectaremos si hay presencia o no del bucle para
        # eliminarlo posteriormente
        # Para ello contaremos con dos variables auxiliares slow_node que avanzará
        # de uno en uno y fast_node que avanzará de dos en dos.
        slow_node, fast_node = self._head, self._head
        result, found = False, False
        # La condición del bucle será que slow_node sea distinto de None porque
        # si este llega en algún momento a ser None significará que no hay bucle
        # y entonces devolveré False
        while slow_node and not found:
            slow_node = slow_node.next
            fast_node = fast_node.next
            if fast_node:
                fast_node = fast_node.next
            # Para comparar los nodos los podemos igualar ya que se compararán
            # sus direcciones de memoria, que solo son iguales si estamos comparando
            # el mismo nodo.
            if slow_node is None or fast_node is None:
                result = False
                found = True
            if slow_node == fast_node:
                result = True
                found = True 
        
        # Ahora vamos a eliminar el bucle. Para ello dejaremos el fast_node en la
        # posición en la que se encontraba y pondremos el slow_node en la posición
        # self._head de la lista. Irán avanzando de 1 en 1 y se encontrarán en el
        # primer nodo que empiece el bucle por lo que nos detendremos una posición
        # antes para hacer que el último nodo apunte a None y así romper el bucle.
        slow_node = self._head
        if slow_node and fast_node:
            while slow_node.next != fast_node.next:
                slow_node = slow_node.next
                fast_node = fast_node.next

        # De esta forma habremos conseguido dejar el fast_node en la última posición
        # de la lista enlazada y por lo tanto sabemos que ahí es donde está el bucle.
        # Lo rompemos
            fast_node.next = None

        return result
		
    def create_loop(self, position):
        # this method is used to force a loop in a singly linked list
        if position < 0 or position > len(self) - 1:
            raise ValueError(f"Position out of range [{0} - {len(self) - 1}]")

        current = self._head
        i = 0

        # We reach position to save the reference
        while current and i < position:
            current = current.next
            i += 1

        # We reach to tail node and set the loop
        start_node = current
        #print(f"Creating a loop starting from {start_node.elem}")
        while current.next:
            current = current.next        
        current.next = start_node
		
	# ------------------TERCER EJERICIO------------------
	
    def left_right_shift(self,left,n):

        # Cuando n es de mayor tamaño que la lista, esta no se modifica
        # Cuando n es de igual tamaño al de la lista, n desplazamientos tanto a
        # izquierda como a derecha devuelve la misma lista
        if n >= len(self) or len(self) == 0:
            return
        
        final_node = self._head
        # De esta forma conseguiremos tener el nodo del último elemento de la lista
        while final_node.next != None:
            final_node = final_node.next

        node_it = self._head

        if left:
            # De esta forma cogeré el último nodo de la secuencia que quiero añadir
            # al final de la lista
            for _ in range(n-1):
                node_it = node_it.next
        else:
            # De esta forma conseguiremos tener el nodo del último elemento de la lista
            for _ in range((self._size - n) - 1):
                node_it = node_it.next
        
        # Hago un bucle desde el último elemento al primero para conectarlos
        final_node.next = self._head
        # Actualizo el primer elemento de la lista
        self._head = node_it.next
        # Suelto el bucle por el nodo que me han pedido(n) para que sea el
        # último de la lista enlazada
        node_it.next = None
        # Actualizo el tail
        self._tail = node_it


if __name__=='__main__':

    l=SList2()
    print("list:",str(l))
    print("len:",len(l))

    for i in range(7):
        l.addLast(i+1)

    print(l)
    print()

    l=SList2()
    print("list:",str(l))
    print("len:",len(l))

    for i in range(7):
        l.addLast(i+1)

    print(l)
    print()

    # No loop yet, no changes applied
    l.fix_loop()
    print("No loop yet, no changes applied")
    print(l)
    print()

    # We force a loop
    l.create_loop(position=6)
    l.fix_loop()
    print("Loop fixed, changes applied")
    print(l)
    print()
    print()

    
    l = SList2()
    for i in [1,2,3,4,5]:        
        l.addLast(i)
    print(l.del_largest_seq())


    l=SList2()
    for i in range(7):
         l.addLast(i+1)

    print(l)
    l.left_right_shift(False, 2)
    print(l)
    
    