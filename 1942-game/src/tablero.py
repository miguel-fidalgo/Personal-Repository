import pyxel
import constantes
import random
import math

from avion import Avion
from fondo import Fondo
from bullets import Bullets
from enemigo import Enemigo
from inicio import Inicio
from hud import HUD
from explosiones import Explosiones


class Tablero:
    
    def __init__(self, w:int, h:int):
        
        self.width = w
        self.heigth = h
        self.start_mov_isla = True
        self.play, self.end, self.win = False, False, False
        self.contador_loops = 1
        self.contador_vidas = 3
        self.score = 0
        self.cooldown_enemigo = constantes.COOLDOWN_ENEMIGO
        self.cooldown_avion = constantes.COOLDOWN 
        self.count_enemigo_normal, self.count_enemigo_rojo = 0, 0
        self.count_enemigo_bomb, self.count_enemigo_super_bomb = 0, 0
        self.total_enemigos = 0
        
        # Configuramos pyxel
        pyxel.init(self.width, self.heigth, title="1942")
        
        # Para agregar las imágenes
        pyxel.image(0).load(0, 0, "assets/img/map/portaviones.png")
        pyxel.image(0).load(130, 0, "assets/img/img_recortadas/HUD/estrella.png")
        pyxel.image(0).load(150, 0, "assets/img/img_recortadas/HUD/estrella_1.png")
        pyxel.image(0).load(170, 0, "assets/img/img_recortadas/HUD/loops.png")
        pyxel.image(0).load(185, 0, "assets/img/img_recortadas/HUD/loops_1.png")
        pyxel.image(0).load(122, 20, "assets/img/img_recortadas/enemigos/bombardero_1.png")
        pyxel.image(0).load(122, 65,  "assets/img/img_recortadas/enemigos/bombardero_2.png")  
        pyxel.image(0).load(122, 110, "assets/img/img_recortadas/enemigos/super_bombardero_1.png")
        pyxel.image(0).load(122, 160, "assets/img/img_recortadas/enemigos/super_bombardero_2.png") 
        pyxel.image(0).load(200, 0, "assets/img/img_recortadas/HUD/bala_enemigo.png")      
        pyxel.image(0).load(205, 0, "assets/img/img_recortadas/HUD/bala_bomb_up.png")
        pyxel.image(0).load(215, 0, "assets/img/img_recortadas/HUD/bala_bomb_down.png")
              
        pyxel.image(1).load(0,0, "assets/img/img_recortadas/aviones/avion_pequeño_1.png")
        pyxel.image(1).load(0,130, "assets/img/img_recortadas/aviones/avion_pequeño_2.png")
        pyxel.image(1).load(26, 0, "assets/img/map/isla1.png")
        pyxel.image(1).load(26, 130, "assets/img/map/isla2.png")
        pyxel.image(1).load(140, 0, "assets/img/img_recortadas/HUD/youwin.png")
        
        pyxel.image(2).load(0, 0, "assets/img/img_recortadas/HUD/bala1.png")
        pyxel.image(2).load(2, 0, "assets/img/img_recortadas/HUD/1942.png")
        pyxel.image(2).load(0, 50, "assets/img/img_recortadas/HUD/capcom.png")
        pyxel.image(2).load(0, 65, "assets/img/img_recortadas/aviones/avion_pequeño_3.png")
        pyxel.image(2).load(0, 95, "assets/img/img_recortadas/aviones/avion_pequeño_4.png")
        pyxel.image(2).load(0, 130, "assets/img/img_recortadas/aviones/avion_pequeño_5.png")
        pyxel.image(2).load(0, 140, "assets/img/img_recortadas/enemigos/enemigo_basico_abajo.png")
        pyxel.image(2).load(0, 155, "assets/img/img_recortadas/enemigos/enemigo_basico_arriba.png")
        pyxel.image(2).load(0, 175, "assets/img/img_recortadas/HUD/explosion.png")
        pyxel.image(2).load(0, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_1.png")
        pyxel.image(2).load(15, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_2.png")
        pyxel.image(2).load(30, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_3.png")
        pyxel.image(2).load(45, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_4.png")
        pyxel.image(2).load(65, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_5.png")
        pyxel.image(2).load(85, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_6.png")
        pyxel.image(2).load(100, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_7.png")
        pyxel.image(2).load(115, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_8.png")
        pyxel.image(2).load(130, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_9.png")
        pyxel.image(2).load(150, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_10.png")
        pyxel.image(2).load(165, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_11.png")
        pyxel.image(2).load(180, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_12.png")
        pyxel.image(2).load(200, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_13.png")
        pyxel.image(2).load(220, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_14.png")
        pyxel.image(2).load(235, 195, "assets/img/img_recortadas/enemigos/enemigo_rojo_15.png")
        pyxel.image(2).load(0, 215,  "assets/img/img_recortadas/enemigos/enemigo_rojo_16.png")


        # Implementamos el fondo del inicio
        self.inicio_1942 = Inicio("1942")
        self.inicio_capcom = Inicio("CAPCOM")
        self.HUD = HUD()

        self.you_win = Inicio("YOUWIN")

        # Implementamos el avión por composición (objeto de la clase Avion)
        self.avion = Avion(*constantes.AVION_INICIAL, constantes.AVION_VELOCIDAD_X, constantes.AVION_VELOCIDAD_Y,
                           constantes.CADENCIA)
        self.fondo_portaviones = Fondo("PORTAVIONES")

        # Implementamos las balas
        self.lista_balas = []
        self.lista_balas_enemigos = []
        
        # Implementamos las islas
        self.lista_islas = []
        self.lista_portaviones = [self.fondo_portaviones]
        
        # Hacemos los loops
        self.lista_loops = [constantes.AVION_LOOP]
        
        # Implementamos los enemigos
        self.lista_enemigos = []
        self.lista_explosiones = []

        """Inciamos el juego"""
           
        pyxel.run(self.update, self.draw)
    
    def update(self):
        
        if pyxel.btnp(pyxel.KEY_Q):
                pyxel.quit()
                
        if self.contador_vidas == 0:
            
            self.end = True
            self.play = False
            
        if self.score >= 1000: 
            
            self.win = True
            self.play = False
            
        # Aquí va todo el código que se ejecuta una vez que el juego esta activo 
          
        if self.play:

            """Aqui establecemos el movimiento del avión"""
            
            if pyxel.btn(pyxel.KEY_W):
                self.avion.move("up", self.width, self.heigth)
            if pyxel.btn(pyxel.KEY_S):
                self.avion.move("down", self.width, self.heigth)
            if pyxel.btn(pyxel.KEY_A):
                self.avion.move("left", self.width, self.heigth)
            if pyxel.btn(pyxel.KEY_D):
                self.avion.move("right", self.width, self.heigth)
                
                
    #----------------------LOOP DE AVION-----------------------------
            
            """Aquí configuramos todo lo relativo al loop del avión,
            siendo 3 el número de veces máximo que se puede ejecutar"""
            
            if pyxel.btnp(pyxel.KEY_SPACE) and self.contador_loops <= 3:
                self.avion.loop_bool = True
                self.avion.frame0 = pyxel.frame_count
                self.HUD.lista_loop[self.contador_loops - 1] = self.HUD.sprite_loop_vacio
                self.contador_loops += 1
            
            self.avion.helices_loop()
                
    #----------------------DISPARO BALA----------------------------------------
    
            self.cooldown_avion = constantes.COOLDOWN 
            
            """A continuación configuramos el disparo de la bala.
            Hacemos uso de la función pyxel.frame_count para regular
            la velocidad del disparo del avión, y que así no sea muy rapida"""

            if pyxel.btnp(pyxel.KEY_J) and pyxel.frame_count - self.cooldown_avion >= self.avion.cadencia:
            
                bala_creada = Bullets((self.avion.x + (self.avion.plane_size_x//2)), 
                                      (self.avion.y - self.avion.plane_size_y), "AVION", 0)
                self.lista_balas.append(bala_creada)
                
                self.cooldown_avion = pyxel.frame_count
                
                
    #------------------------PORTAVIONES---------------------------------------   
    
            """Esta parte del código configura el movimiento del portaviones 
            una vez iniciado el juego"""

            for portaviones in self.lista_portaviones:

                if portaviones.y < constantes.ALTO:    
                    portaviones.move()
                    self.posicion_portaviones = portaviones.y
                
                else:
                    # Guarda la posición en la que se guarda el portaviones y después lo borra
                    self.posicion_portaviones = constantes.ALTO
                    self.lista_portaviones.remove(portaviones)
                    
    #--------------------------ISLAS-----------------------------------------------            
    
            """Esta parte del código se asemeja mucho a la del portaviones, 
            excepto que las islas son infinitas y el portaviones nó"""

            for isla in self.lista_islas:
                
                # Para que las islas no se sobreescriban entre sí
                if isla.y == (isla.sprite[4] + 30):
                    self.start_mov_isla = True
                            
                if isla.y < constantes.ALTO:
                    isla.move()
                else:
                    self.lista_islas.remove(isla)
                    
            """Las siguientes líneas establecen los criterios de aparición de las islas, 
            siguiendo las siguientes condiciones:
                     - La posibilidad es de 1/10
                     - El portaviones haya llegado al final del mapa"""        
            
            spawn_isla = random.randint(1,10)
            if spawn_isla == 1 and self.posicion_portaviones == constantes.ALTO and self.start_mov_isla:
                isla_creada = Fondo("isla")
                self.lista_islas.append(isla_creada)
                self.start_mov_isla = False
                
    #-------------------------ENEMIGOS-----------------------------------------
      
            """Los cuatro siguientes "if", establecen los criterios de aparición de
            los diferentes enemigos, siendo el regular el más facil de aparecer,
            y el superbombardero el más difícil"""
            
            spawn_enemigo = random.randint(1, 400)
            
            # Aquí se crean los enemigos y se añaden a la lista
            if 1 <= spawn_enemigo < 10 and self.posicion_portaviones >= (constantes.ALTO // 2)\
                and self.count_enemigo_normal == 0:
                
                self.aleatorio = random.randint(2, 5)
                for i in range(self.aleatorio):
                    self.lista_enemigos.append(Enemigo("REGULAR"))
                    self.count_enemigo_normal += 1

            if 12 <= spawn_enemigo < 15 and self.posicion_portaviones >= (constantes.ALTO // 2)\
                and self.count_enemigo_rojo == 0:
                    
                self.aleatorio = random.randint(2, 3)
                for i in range(self.aleatorio):
                    self.lista_enemigos.append(Enemigo("ROJO"))
                    self.count_enemigo_rojo += 1  
                    
            if 19 <= spawn_enemigo <=  20 and self.posicion_portaviones >= (constantes.ALTO)\
                and self.count_enemigo_bomb == 0:
                self.lista_enemigos.append((Enemigo("BOMBARDERO")))
                self.count_enemigo_bomb += 1
                
            if spawn_enemigo == 400 and self.posicion_portaviones >= (constantes.ALTO)\
                and self.count_enemigo_super_bomb == 0 and self.count_enemigo_bomb == 0:
                self.lista_enemigos.append((Enemigo("SUPERBOMBARDERO")))
                self.count_enemigo_super_bomb += 1

            self.total_enemigos = self.count_enemigo_normal +\
            self.count_enemigo_rojo + self.count_enemigo_bomb + self.count_enemigo_super_bomb

            for enemigo in self.lista_enemigos:
                            
                """Esta parte llama a la función que hace la pirueta del avión, en función
                de la posición x en la que se encuentre"""
                
                if enemigo.tipo == "ROJO":

                    if enemigo.x != 100 and not enemigo.voltereta_bool:
                        enemigo.sprite = constantes.ENEMIGO_ROJO_1

                    elif enemigo.x == 100 and not enemigo.voltereta_bool and enemigo.contador_vueltas <= enemigo.vueltas:
                        enemigo.voltereta_bool = True
                        enemigo.frame0 = pyxel.frame_count
                    
                    else:
                        enemigo.sprite = constantes.ENEMIGO_ROJO_1
                        
                """Una vez establecidas sus caracterísiticas principales,
                establecemos su movimiento, junto con las condiciones necesarias
                para que cualquier objeto del tablero desaparezca. 
                Las condiciones son las siguientes:
                    - Una bala del avión da a un enemigo
                    - Un enemigo y el avión se chocan
                    - Un enemigo se sale del mapa
                    - Una bala del enemigo da al avión (En este caso se eliminará con 3 golpes)"""                
                    
                if  -constantes.ALTO <= enemigo.y <= (constantes.ALTO)\
                    and -constantes.ANCHO < enemigo.x < constantes.ANCHO:
                        
                    enemigo.move()

                    """Los enemigos mueren por bala del avión"""
                    
                    for bala in self.lista_balas:
                        
                        if enemigo.x < bala.x < enemigo.x + enemigo.sprite[3] + bala.sprite[3]\
                        and enemigo.y < bala.y < enemigo.y + enemigo.sprite[4]:
                            
                            if enemigo.tipo == "REGULAR":
                                self.count_enemigo_normal -= 1
                                self.score += enemigo.score
                                self.lista_enemigos.remove(enemigo)  
                                
                            if enemigo.tipo == "ROJO":
                                self.count_enemigo_rojo -= 1
                                self.score += enemigo.score                                                                    
                                self.lista_enemigos.remove(enemigo)

                            if enemigo.tipo == "BOMBARDERO":
        
                                if enemigo.vida > 1:
                                    enemigo.vida -= 1
                                else:
                                    self.lista_enemigos.remove(enemigo)   
                                    self.count_enemigo_bomb -= 1
                                    self.score += enemigo.score
                                    
                                    """Aquí hacemos que el avión gane loops matando a bombarderos"""
                                    
                                    if self.contador_loops > 1:
                                        
                                        self.contador_loops -= 1
                                        self.HUD.lista_loop[self.contador_loops - 1] = self.HUD.sprite_loop
                                    
                            if enemigo.tipo == "SUPERBOMBARDERO":
        
                                if enemigo.vida > 1:
                                    enemigo.vida -= 1
                                else:
                                    self.lista_enemigos.remove(enemigo)   
                                    self.count_enemigo_super_bomb -= 1
                                    self.score += enemigo.score

                                    """Aquí hacemos que el avión gane vida matando a superbombarderos"""

                                    if self.contador_vidas < 3:
                                        self.contador_vidas += 1
                                        self.HUD.lista_estrella[self.contador_vidas - 1] = self.HUD.sprite_estrella
                                
                            self.lista_explosiones.append(Explosiones((enemigo.x + enemigo.sprite[3] / 2), (enemigo.y + enemigo.sprite[4] / 2), 2.5))
                            self.lista_balas.remove(bala)

                    """Los enemigos mueren por la colisión contra el avión.
                    Además cabe destacar que hacemos uso de la fórmula matemática 
                    para calcular la distancia entre dos vectores, y así comprobar 
                    si ambos están colisionando.
                    También restamos una vida al avión para que una vez que pierda las tres muera"""
                    
                    r_enemigo, r_avion = enemigo.sprite[4]/2, self.avion.sprite[4]/2
                    dif_x = ((self.avion.x + self.avion.sprite[3]/2) - (enemigo.x + enemigo.sprite[3]/2))**2 
                    dif_y = ((self.avion.y + self.avion.sprite[4]/2) - (enemigo.y + enemigo.sprite[4]/2))**2
                    # Importamos función math para hacer la raíz cuadrada
                    distancia = math.sqrt(dif_x + dif_y)
                    
                    if distancia <= (r_enemigo + r_avion) and not self.avion.loop_bool:
                        
                        if enemigo.tipo == "REGULAR":
                            self.count_enemigo_normal -= 1
                            
                        if enemigo.tipo == "ROJO":
                            self.count_enemigo_rojo -= 1
                            
                        if enemigo.tipo == "BOMBARDERO":
                            self.count_enemigo_bomb -= 1
                            
                        if enemigo.tipo == "SUPERBOMBARDERO":
                            self.count_enemigo_super_bomb -= 1

                
                        self.lista_explosiones.append(Explosiones((enemigo.x + enemigo.sprite[3] / 2), (enemigo.y + enemigo.sprite[4] / 2), 2.5))
                        self.lista_enemigos.remove(enemigo)
                        self.HUD.lista_estrella[self.contador_vidas - 1] = self.HUD.sprite_estrella_vacia
                        self.contador_vidas -= 1

                    """Aquí los enemigos mueren por salirse del mapa"""    
                    
                else:
                    
                    if enemigo.tipo == "REGULAR":
                        self.count_enemigo_normal -= 1

                    if enemigo.tipo == "ROJO":
                        self.count_enemigo_rojo -= 1
                        
                    if enemigo.tipo == "BOMBARDERO":
                        self.count_enemigo_bomb -= 1
                        
                    if enemigo.tipo == "SUPERBOMBARDERO":
                        self.count_enemigo_super_bomb -= 1
                    self.lista_enemigos.remove(enemigo)

                """Una vez creados los enemigos, procedemos a configurar sus características especiales
                siendo en el caso del enemigo regular y el superbombardero, la habilidad de disparar"""

                self.cooldown_enemigo = constantes.COOLDOWN_ENEMIGO
                
                # Creamos las balas para los enemigos que disparan
                if enemigo.tipo == "REGULAR" or enemigo.tipo == "BOMBARDERO":
                    
                    # pyxel.frame_count - self.cooldown_enemigo >= constantes.CADENCIA_ENEMIGO

                    if enemigo.y >= 0 and not enemigo.back and pyxel.frame_count % 16 == 0 and \
                        pyxel.frame_count - self.cooldown_enemigo >= constantes.CADENCIA_ENEMIGO:
                        
                        bala_creada = Bullets((enemigo.x + (enemigo.sprite[3]/2)),
                                            (enemigo.y + enemigo.sprite[4]), enemigo.tipo, enemigo.pos)
                        
                        self.lista_balas_enemigos.append(bala_creada)
                        self.cooldown_enemigo = pyxel.frame_count 

                    for bala in self.lista_balas_enemigos:
                        
                        if bala.y <= constantes.ALTO or\
                            (enemigo.tipo == "BOMBARDERO" and enemigo.pos == 1 and bala.y >= 0):
                            bala.move()
                            
                            """Aquí se configura cuando la bala del enemigo, impacta contra 
                            el avión"""

                            if self.avion.x < bala.x < self.avion.x + self.avion.sprite[3] + bala.sprite[3]\
                                and self.avion.y < bala.y < self.avion.y + self.avion.sprite[4] and\
                                    not self.avion.loop_bool:
                                
                                self.lista_explosiones.append(Explosiones((self.avion.x + self.avion.sprite[3] / 2), 
                                                                          (self.avion.y + self.avion.sprite[4] / 2), 4))
                                self.lista_balas_enemigos.remove(bala)
                                self.HUD.lista_estrella[self.contador_vidas - 1] = self.HUD.sprite_estrella_vacia
                                self.contador_vidas -= 1
                            
                        else:
                            self.lista_balas_enemigos.remove(bala)

#--------------------------BALAS-----------------------------------------------     

            """Recorremos la lista de las balas del avión para configurar el disparo"""
            
            for bala in self.lista_balas:
                if bala.y > 0 - bala.sprite[4]:
                    bala.move()
                else:
                    self.lista_balas.remove(bala)
                    
            for explosion in self.lista_explosiones:
                explosion.tamaño += 3
                if explosion.tamaño > 12:
                    self.lista_explosiones.remove(explosion)

            """Aquí lo que hacemos es configurar la pantalla de inicio, para que una vez
            que se presione el espacio, empiece a jugar"""  
        
        elif not self.play and not self.end and not self.win:
            
            if pyxel.btnp(pyxel.KEY_SPACE):
                self.play = True
                
            """Aquí reiniciamos todas las variables una vez que el juego acaba,
            ya sea porque el avión se queda sin vidas, o porque la puntuación llega al máximo"""
        
        elif self.end:
            
            self.lista_portaviones = []
            self.lista_balas = []
            self.lista_balas_enemigos = []
            self.lista_islas = []
            self.lista_enemigos = []
            self.lista_explosiones = []
            
            if pyxel.btnp(pyxel.KEY_SPACE):
                self.end = False
                self.play = True
                self.win = False
                self.start_mov_isla = True
                self.contador_loops = 1
                self.contador_vidas = 3
                self.avion = Avion(*constantes.AVION_INICIAL, constantes.AVION_VELOCIDAD_X
                    , constantes.AVION_VELOCIDAD_Y, constantes.CADENCIA)
                self.fondo_portaviones = Fondo("PORTAVIONES")  
                self.lista_portaviones = [self.fondo_portaviones]
                self.HUD = HUD()
                self.score = 0
                self.count_enemigo_normal, self.count_enemigo_rojo = 0, 0
                self.count_enemigo_bomb, self.count_enemigo_super_bomb = 0, 0
                self.total_enemigos = 0

    def draw(self):
        
        """En esta parte del draw, mostramos todo lo que se ve mientras se juega"""

        if self.play:      
            
            pyxel.cls(5)
             
            pyxel.blt(self.fondo_portaviones.x, self.fondo_portaviones.y,
                    *self.fondo_portaviones.sprite)
            
            for isla in self.lista_islas:
                pyxel.blt(isla.x, isla.y, *isla.sprite, colkey=0)

            pyxel.blt(self.avion.x, self.avion.y, *self.avion.sprite, colkey=0)
                
            for enemigo in self.lista_enemigos:
                 pyxel.blt(enemigo.x, enemigo.y, *enemigo.sprite, colkey=0)

            for bala in self.lista_balas:
                pyxel.blt(bala.x, bala.y, *bala.sprite)
            
            for bala_enemigo in self.lista_balas_enemigos:
                pyxel.blt(bala_enemigo.x, bala_enemigo.y, *bala_enemigo.sprite, colkey=0)
            
            for explosiones in self.lista_explosiones:
                pyxel.circ(explosiones.x, explosiones.y, explosiones.tamaño, 7)
                pyxel.circb(explosiones.x, explosiones.y, explosiones.tamaño, 10)
                
            
            self.HUD.draw()

            pyxel.text(constantes.ANCHO//2 - 20, 5, "HIGH SCORE: ", 10)
            pyxel.text(constantes.ANCHO//2, 15, str(self.score) , 10)
        
            """En esta parte se muestra todo lo que es la pantalla de carga, donde se espera 
            a que el jugador pulse el espacio"""
        
        elif not self.play and not self.end and not self.win:
            
            pyxel.cls(3)
            
            self.inicio_1942.draw()
            self.inicio_capcom.draw()
            
            """Y en esta parte se muestra todo lo que es la pantalla que se ve una vez que se acaba
            el juego, que se cambia una vez que el jugador vuelve a pulsar espacio"""
        
        elif self.end:
            
            pyxel.cls(0)    
            
            pyxel.text(constantes.ANCHO//2 - 5, constantes.ALTO//2, "GAME \nOVER", pyxel.frame_count % 16)
                        
            pyxel.text((constantes.ANCHO//2 - 50)
                    , constantes.ALTO//2 + 40, "Press SPACE to play again", pyxel.frame_count % 16)
            
        elif self.win:
            
            pyxel.cls(11)
            self.you_win.draw()                      