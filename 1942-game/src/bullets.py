import constantes

class Bullets:
    
    lista_balas = []
    
    """Esta clase contiene la información sobre las balas que dispara el avión"""
    
    def __init__(self, x, y, tipo, pos):   
        
        self.x = x
        self.y = y
        self.tipo = tipo.upper()
        self.pos = pos
        
        if self.tipo == "BOMBARDERO":
            
            if self.pos == 0: #Si sale 0, el enemigo aparece por arriba, y si no por abajo
                
                self.y = y
                self.sprite = constantes.BALA_BOMBARDERO_D
                
            else:
        
                self.y = y - constantes.BOMBARDERO_U[4] - 10
                self.sprite = constantes.BALA_BOMBARDERO_U
            
            self.x -= self.sprite[3] / 2
        
        if self.tipo == "AVION":
            self.sprite = constantes.BALA_INDIVIDUAL
        
        elif self.tipo == "REGULAR":
            self.sprite = constantes.BALA_REGULAR
            self.x -= self.sprite[3] / 2
        
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
    def sprite(self, sprite: tuple):
        
        self.__sprite = sprite   
    
    @property
    def tipo(self) -> str:
        
        return self.__tipo
    
    @tipo.setter
    def tipo(self, tipo):
        
        self.__tipo = tipo
        
    
    #--------------------------METODOS-----------------------------------------     

    """Esta función configura el movimiento de los tres objetos que 
    disparan en todo el juego, haciendo una pequeña distinción con el bombardero,
    ya que este puede aparecer desde arriba o abajo, por lo que el movimiento 
    de las balas será diferente"""
    
    def move(self):
    
        if self.tipo == "AVION":
            self.y -= 1 * constantes.VELOCIDAD_BALAS
            
        if self.tipo == "REGULAR":
            self.y += (1 * constantes.VELOCIDAD_BALAS_ENEMIGO_REGULAR)
            
        if self.tipo == "BOMBARDERO":
            
            if self.pos == 0:
                
                self.y += (1 * constantes.VELOCIDAD_BALAS_ENEMIGO_BOMBARDERO)
                
            else:
                
                self.y -= (1 * constantes.VELOCIDAD_BALAS_ENEMIGO_BOMBARDERO)
            