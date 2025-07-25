
class Explosiones:
    
    def __init__(self, x:int, y:int, tamaño:int):
        
        self.x = x
        self.y = y
        self.tamaño = tamaño
        
    @property
    def x (self) -> int:
        
        return self.__x 
    
    @x.setter
    def x(self, x):
        
        self.__x = x

    @property
    def y(self) -> int:
        
        return self.__y
    
    @y.setter
    def y(self, y):
        
        self.__y = y
    
    @property
    def tamaño(self) -> int:
        
        return self.__tamaño
    
    @tamaño.setter
    def tamaño(self, tamamño):
        
        self.__tamaño = tamamño