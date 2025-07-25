import pyxel
import constantes

class Avion:
    
    def __init__(self, x:int, y:int, speed_x:int, speed_y:int, cadencia:int):  
        
        self.x = x
        self.y = y
        self.speed_x = speed_x
        self.speed_y = speed_y
        self.cadencia = cadencia

        self.loop_bool = False
        
        self.sprite = constantes.AVION_SPRITE_1

        self.plane_size_x = (self.sprite[3])
        self.plane_size_y = (self.sprite[4])
        
    
    @property   
    def x (self) -> int:   
        
        return self.__x
        
    @x.setter    
    def x (self, x):
        self.__x = x
        
    @property   
    def y (self) -> int:
                        
        return self.__y
        
    @y.setter    
    def y (self, y):
        self.__y = y       

    @property
    def plane_size_x(self) -> int:
                
        return self.__plane_size_x
    
    @plane_size_x.setter
    def plane_size_x(self, plane_size_x):
        
        self.__plane_size_x = plane_size_x
        
    
    @property
    def plane_size_y(self) -> int:
                
        return self.__plane_size_y
    
    @plane_size_y.setter
    def plane_size_y(self, plane_size_y):
        
        self.__plane_size_y = plane_size_y
        
    @property
    def speed_x(self) -> int:
        
        return self.__speed_x
    
    @speed_x.setter
    def speed_x(self, speed_x):
        
        self.__speed_x = speed_x
    
    
    @property
    def speed_y(self) -> int:
        
        return self.__speed_y
    
    @speed_y.setter
    def speed_y(self, speed_y):
        
        self.__speed_y = speed_y
        
    @property
    def sprite(self) -> tuple:
        
        return self.__sprite
    
    @sprite.setter
    def sprite(self, sprite):
        
        self.__sprite = sprite
        
    #-------------------------------METODOS------------------------------------
        
    """Aquí establecemos todas las teclas necesarias para que el avión se mueva"""
        
    def move(self, direction:str, size_x:int, size_y:int):
            
        if direction.lower() == "right" and self.x  <= size_x - self.plane_size_x: 
            
            self.x += (1 * self.speed_x)
            
        if direction.lower() == "left" and self.x > 0:
            
            self.x -= (1 * self.speed_x)
            
        if direction.lower() == "up" and self.y > 0:
            
            self.y -= (1 * self.speed_y)
        
        if direction.lower() == "down" and self.y <= size_y - self.plane_size_y:
            
            self.y += (1 * self.speed_y)
                    
    """Aquí configuramos todo lo que son las animaciones del avión,
    tanto la voltereta como el giro de las hélices"""    
    
    def helices_loop(self):

        if self.loop_bool == False:
            if pyxel.frame_count % 3 == 0 :
            
                self.sprite = constantes.AVION_SPRITE_1

            else:
                self.sprite = constantes.AVION_SPRITE_2 

        else:
            if (pyxel.frame_count - self.frame0) % 25 < 5:
                self.sprite = constantes.AVION_LOOP[0]
            elif 5 <= (pyxel.frame_count - self.frame0) % 25 < 10:
                self.sprite = constantes.AVION_LOOP[1]
            elif 10 <= (pyxel.frame_count - self.frame0) % 25 < 15:
                self.sprite = constantes.AVION_LOOP[2]
            elif 15 <= (pyxel.frame_count - self.frame0) % 25 < 20:
                self.sprite = constantes.AVION_LOOP[3]
            elif 20 <= (pyxel.frame_count - self.frame0) % 25 < 25:
                self.loop_bool = False
        