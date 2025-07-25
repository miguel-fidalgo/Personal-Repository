import constantes
import pyxel

class Inicio:
    
    def __init__(self, tipo: str) -> None:
        
        self.tipo = tipo

        """Aquí se guarda toda la información del sprite 1942"""
        
        if self.tipo == "1942":
            self.sprite = constantes.SPRITE_1942
            self.x = (constantes.ANCHO - self.sprite[3])//2
            self.y = (constantes.ALTO - self.sprite[4])//2
            
        """Aquí se guarda toda la información del sprite CAPCOM"""
        
        if self.tipo.upper() == "CAPCOM":
            self.sprite = constantes.SPRITE_CAPCOM
            self.x = (constantes.ANCHO - self.sprite[3])//2
            self.y = constantes.ALTO//2 + 80

        """"Aquí se guarda toda la información del sprite YOU WIN (pantalla victoria)"""
        
        if self.tipo.upper() == "YOUWIN":
            self.sprite = constantes.SPRITE_YOUWIN
            self.x = (constantes.ANCHO - self.sprite[3])/2
            self.y = (constantes.ALTO - self.sprite[4])/2

  
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
        
    #----------------------------METODOS---------------------------------------
        
    def draw(self):
        
        """Aqui ponemos todo lo que se muestra en la pantalla de incio,
        tanto el texto como los sprite"""
        
        if self.tipo == "YOUWIN":

            pyxel.blt(self.x, self.y , *self.sprite ,colkey=0)
        
        else:

            pyxel.text(constantes.ANCHO//2 - 22, 5, "HIGH SCORE", 10)
            
            pyxel.blt(self.x, self.y , *self.sprite ,colkey=0)
            
            pyxel.text((constantes.ANCHO//2 - 35)
                        , constantes.ALTO//2 + 40, "Press SPACE to play", pyxel.frame_count % 16)
            
            pyxel.text((constantes.ANCHO//2 - 20)
                        , constantes.ALTO//2 + 60, "INSERT COIN", pyxel.frame_count % 16)
        