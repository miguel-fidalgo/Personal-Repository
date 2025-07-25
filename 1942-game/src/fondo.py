import random
import constantes

class Fondo:

    def __init__(self, tipo: str):

        self.x = 0
        self.y = 0
        self.tipo = tipo.upper()
        self.isla = 0


        if self.tipo == "PORTAVIONES":
            self.sprite = constantes.SPRITE_PORTAVIONES
            self.x = 51.5
            
        if self.tipo == "ISLA":
            
            """Aquí elegimos entre la isla 1 y la 2"""
            
            self.isla = random.randint(0 , len(constantes.SPRITE_ISLA) - 1)
            
            """Aquí seleccionamos el sprite de la isla aleatoria obtenida"""
            
            self.sprite = constantes.SPRITE_ISLA[self.isla]           
            self.x = random.randint(-(self.sprite[3]//2), (constantes.ANCHO - self.sprite[3]//3))
            self.y = -(self.sprite[4])


    @property
    def x(self) -> int:
        
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
    def sprite(self) -> tuple:
        
        return self.__sprite
    
    @sprite.setter
    def sprite(self, sprite):
        
        self.__sprite = sprite
    
    @property
    def tipo(self) -> str:
        
        return self.__tipo
    
    @tipo.setter
    def tipo(self, tipo):
        
        self.__tipo = tipo
        
    #------------------------METODOS-------------------------------------------
    
    """Esta función, configura el movimiento de las islas y la del portaviones"""
    
    def move(self):

        self.y += 1
