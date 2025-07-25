import constantes
import random
import pyxel

# Esto hay que repensarlo para la herencia
class Enemigo:
    def __init__(self, tipo:str):
        self.tipo = tipo.upper()
        self.back = False
        
        """A lo largo de todo este init, establecemos todos los atributos,
        que son especificos de cada uno de los enemigos, como puede ser su vida, 
        velocidad, o puntuación"""
        
        if (tipo == "REGULAR"):
            self.sprite = constantes.ENEMIGO_BASICO_D
            self.vida = 1
            self.speed = 3
            self.score = 10
            self.x = random.randint(0,200)
            self.y = -100
            self.pos = random.randint(0, 1)
            self.turn = random.randint(0, 1) #Este random, establece que una vez que el enemigo
                                             # llegue al medio, gire a la derecha si == 0, 
                                             # o a la izquierda si == 1
            
        if (tipo == "ROJO"):
            self.sprite = constantes.ENEMIGO_ROJO_1
            self.speed = 2
            self.score = 15
            self.x = -100
            self.y = random.randint(constantes.ALTO//4, 2*constantes.ALTO//3)
            self.voltereta_bool = False
            self.contador_vueltas = 0
            self.vueltas = random.randint(2,3) #Número de vueltas que puede dar el enemigo rojo

        if (tipo == "BOMBARDERO"):
            self.vida = 3
            self.speed = 1.5
            self.score = 30
            self.x = random.randint(0, 180)
            self.pos = random.randint(0, 1) #Si sale 0, el enemigo aparece por arriba, y si no por abajo
            
            if self.pos == 0:
                self.y = -100
                self.sprite = constantes.BOMBARDERO_D

            elif self.pos == 1:
                self.y = 256
                self.sprite = constantes.BOMBARDERO_U


        if (tipo == "SUPERBOMBARDERO"):
            self.vida = 5
            self.speed = 0.75
            self.score = 50
            self.x = random.randint(0, 180)
            self.pos = random.randint(0, 1) #Si sale 0, el enemigo aparece por arriba, y si no por abajo
            
            if self.pos == 0:
                self.y = -100
                self.sprite = constantes.SUPER_BOMBARDERO_D

            elif self.pos == 1:
                self.y = 256
                self.sprite = constantes.SUPER_BOMBARDERO_U
    
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
        
    @property
    def score(self) -> str:
        
        return self.__score
    
    @score.setter
    def score(self, score):
        
        self.__score = score

    #----------------------------METODOS---------------------------------------

    """Esta función, establece el movimiento para cada uno de los enemigos,
    haciendo distinción entre el tipo de enemigo, ya que cada uno actua de forma diferente"""

    def move(self):
        
        if self.tipo == "REGULAR":
            
            # Si el enemigo está en la parte de arriba de la pantalla, baja
            if self.y < constantes.ALTO // 2 and not self.back:
                self.y += (1 * self.speed)

            # Una vez que el enemigo esté en la mitad de la pantalla, retrocede    
            elif self.y >= constantes.ALTO// 2 and not self.back: 
                
                self.sprite = constantes.ENEMIGO_BASICO_U
                self.back = True
                
                if self.turn == 0:     
                            
                    self.x += 10 
                    self.y -= (1 * self.speed)
                    
                elif self.turn == 1:       
                            
                    self.x -= 10
                    self.y -= (1 * self.speed)

            elif self.back:

                self.y -= (1 * self.speed)

        
        if self.tipo == "ROJO":
            
            """Este código configura la voltereta que hace el enemigo rojo, 
            siendo aleatorio el número de veces que lo repite"""
            
            if self.voltereta_bool == True:
                
                if 0 <= (pyxel.frame_count - self.frame0) % 85 < 5:
                    self.x += 1 *3
                    # self.y += 0
                    self.sprite = constantes.ENEMIGO_ROJO_1
                elif 5 <= (pyxel.frame_count - self.frame0) % 85 < 10:
                    self.x += 0.75 *3
                    self.y += 0.25*3
                    self.sprite = constantes.ENEMIGO_ROJO_2
                elif 10 <= (pyxel.frame_count - self.frame0) % 85 < 15:
                    self.x += 0.5*3
                    self.y += 0.5*3
                    self.sprite = constantes.ENEMIGO_ROJO_3
                elif 15 <= (pyxel.frame_count - self.frame0) % 85 < 20:
                    self.x += 0.25*3
                    self.y += 0.75*3
                    self.sprite = constantes.ENEMIGO_ROJO_4
                elif 20 <= (pyxel.frame_count - self.frame0) % 85 < 25:
                    self.x += 0*3
                    self.y += 1*3
                    self.sprite = constantes.ENEMIGO_ROJO_5
                elif 25 <= (pyxel.frame_count - self.frame0) % 85 < 30:
                    self.x -= 0.25*3
                    self.y += 0.75*3
                    self.sprite = constantes.ENEMIGO_ROJO_6
                elif 30 <= (pyxel.frame_count - self.frame0) % 85 < 35:
                    self.x -= 0.5*3
                    self.y += 0.5*3
                    self.sprite = constantes.ENEMIGO_ROJO_7
                elif 35 <= (pyxel.frame_count - self.frame0) % 85 < 40:
                    self.x -= 0.75*3
                    self.y += 0.25*3
                    self.sprite = constantes.ENEMIGO_ROJO_8
                elif 40 <= (pyxel.frame_count - self.frame0) % 85 < 45:
                    self.x -= 1*3
                    self.y += 0*3
                    self.sprite = constantes.ENEMIGO_ROJO_9
                elif 45 <= (pyxel.frame_count - self.frame0) % 85 < 50:
                    self.x -= 0.75*3
                    self.y -= 0.25*3
                    self.sprite = constantes.ENEMIGO_ROJO_10
                elif 50 <= (pyxel.frame_count - self.frame0) % 85 < 55:
                    self.x -= 0.5*3
                    self.y -= 0.5*3
                    self.sprite = constantes.ENEMIGO_ROJO_11
                elif 55 <= (pyxel.frame_count - self.frame0) % 85 < 60:
                    self.x -= 0.25*3
                    self.y -= 0.75*3
                    self.sprite = constantes.ENEMIGO_ROJO_12
                elif 60 <= (pyxel.frame_count - self.frame0) % 85 < 65:
                    self.x += 0*3
                    self.y -= 1*3
                    self.sprite = constantes.ENEMIGO_ROJO_13
                elif 65 <= (pyxel.frame_count - self.frame0) % 85 < 70:
                    self.x += 0.25*3
                    self.y -= 0.75*3
                    self.sprite = constantes.ENEMIGO_ROJO_14
                elif 70 <= (pyxel.frame_count - self.frame0) % 85 < 75:
                    self.x += 0.5*3
                    self.y -= 0.5*3
                    self.sprite = constantes.ENEMIGO_ROJO_15
                elif 75 <= (pyxel.frame_count - self.frame0) % 85 < 80:
                    self.x += 0.75*3
                    self.y -= 0.25*3
                    self.sprite = constantes.ENEMIGO_ROJO_16
                elif 80 <= (pyxel.frame_count - self.frame0) % 85 < 85:
                    self.sprte = constantes.ENEMIGO_ROJO_1
                    self.voltereta_bool = False
                    self.contador_vueltas += 1

            else:            
                self.x += (1 * self.speed)
        
        """En las dos siguientes condiciones, cabe destacar, que se hace distinción entre
        si el enemigo está mirando para arriba o abajo, ya que avanzará de manera distinta"""
        
        if self.tipo == "SUPERBOMBARDERO":
                        
            if self.pos == 0:
            
                self.y += (1 * self.speed)

            else:

                self.y -= (1 * self.speed)
                
        if self.tipo == "BOMBARDERO":
                        
            if self.pos == 0:
            
                self.y += (1 * self.speed)

            else:

                self.y -= (1 * self.speed)
