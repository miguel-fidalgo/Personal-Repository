import constantes
import pyxel

class HUD:
    
    def __init__(self):
        
        """Aquí se almacenan los sprites de las estrellas, que represetan
        la vida del avión"""
        
        self.sprite_estrella = constantes.SPRITE_ESTRELLA
        self.sprite_estrella_vacia = constantes.SPRITE_ESTRELLA_VACIA
        
        """Aquí se almacenan los sprites de las R, que represetan los loops
        restantes del avión"""
        
        self.sprite_loop = constantes.SPRITE_LOOPS
        self.sprite_loop_vacio = constantes.SPRITE_LOOPS_VACIO
        
        """Y aquí creamos dos lista, para posteriormente poder ir restando y cambiando los sprites
        en función de cuantas vidas le queden, o cuantos loops"""
        
        self.lista_loop = [self.sprite_loop,self.sprite_loop,self.sprite_loop]
        self.lista_estrella = [self.sprite_estrella,self.sprite_estrella,self.sprite_estrella]
    
    #------------------------------METODOS----------------------------------------
    
    def draw(self):
        
        """Todos los sprites de las vidas"""
        
        pyxel.blt(0, constantes.ALTO - self.sprite_estrella[4]
                  , *self.lista_estrella[0], colkey=0)
        
        pyxel.blt(self.sprite_estrella[3] + 2, constantes.ALTO - self.sprite_estrella[4], 
                  *self.lista_estrella[1], colkey=0)
        
        pyxel.blt((self.sprite_estrella[3] + 2)*2, constantes.ALTO - self.sprite_estrella[4], 
                  *self.lista_estrella[2], colkey=0)
        
        """Todos los sprites de los loops"""
        
        pyxel.blt(constantes.ANCHO - (self.sprite_loop[3] * 3) - 4, 
                  constantes.ALTO - self.sprite_loop[4],
                  *self.lista_loop[0], colkey=7)
        
        pyxel.blt(constantes.ANCHO - (self.sprite_loop[3] * 2) - 2, 
                  constantes.ALTO - self.sprite_loop[4],
                  *self.lista_loop[1], colkey=7)
        
        pyxel.blt(constantes.ANCHO - (self.sprite_loop[3]), 
                  constantes.ALTO - self.sprite_loop[4],
                  *self.lista_loop[2], colkey=7)
        
        