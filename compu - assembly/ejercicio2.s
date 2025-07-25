
SinMatrix:

        # Anadimos a la pila ra/s1/s2/s3/s4/s5
        addi sp sp -28

        sw ra 24(sp)
        sw s5 20(sp)
        sw s4 16(sp)
        sw s3 12(sp)
        sw s2 8(sp)
        sw s1 4(sp)
        sw s0 0(sp)

        li s0 0  # Contador externo i
        li s1 0  # Contador interno j
        mv s2 a2 # Limite contador i (filas), suponiendo que el parametro a2 sea N filas
        mv s3 a3 # Limite contador j (columnas), suponiendo que el parametro a3 sea M columnas
        mv s4 a0 # Direccion inicio matrixA
        mv s5 a1 # Direccion inicio matrixB

bucle_1:bge s0 s2 fin1   # Bucle externo para las filas (i)
bucle_2:bge s1 s3 fin2   # Bucle interno para las columnas (j)
		flw fa0 0(s4)    # Nos guardamos el valor en la posicion [i][j] = [s0][s1]
        jal ra sin       
        fsw fa0 0(s5)    # Escribimos el seno en la misma posicion
        addi s4 s4 4     # Nos movemos a la siguiente posicion en memoria de la matrixA (+4)
        addi s5 s5 4     # Nos movemos a la siguiente posicion en memoria de la matrixB (+4)
        addi s1 s1 1     # Incrementamos en 1 el contador interno (j)
        j bucle_2
fin2:	addi s0 s0 1     # Incrementamos en 1 el contador externo (i)
		li s1 0          # Reseteamos el contador i
        j bucle_1

fin1:	
        lw ra 24(sp)     # Recuperamos de la pila
        lw s5 20(sp)
        lw s4 16(sp)
        lw s3 12(sp)
        lw s2 8(sp)
        lw s1 4(sp)
        lw s0 0(sp)
        
        addi sp sp 28

        jr ra


factorial:
	# Anadimos a la pila s0 y s1 porque vamos a trabajar con ellos
	addi sp sp -8
    fsw fs0 4(sp)
    fsw fs1 0(sp)
    
    # El parametro que se pasa a la funcion factorial es a0
    li t0 1          # Numero 1 en entero que sera el contador del bucle (i)
    fcvt.s.w ft0 t0  # Numero 1 en flotante
    fcvt.s.w fs1 t0  # Resultado del factorial (acum)
    fcvt.s.w fs0 t0  # Valor a multiplicar
    fcvt.s.w fa0 a0  # Convertimos a0 a fa0
    
    bucle_f: bgt t0 a0 end_f
        fmul.s fs1 fs1 fs0  # Incrementamos el factorial
        fadd.s fs0 fs0 ft0  # Incrementamos el siguiente valor a multiplicar
        addi t0 t0 1        # Incrementamos el contador
        j bucle_f           # Volvemos al loop

    end_f: 
        fmv.s fa0 fs1      # Movemos el resultado para devolverlo en fa0
                       
        flw fs0 4(sp)      # Recuperamos el valor de fs0 y fs1 antes de volver a la funcion principal
        flw fs1 0(sp)
        addi sp sp 8
        jr ra              # Vamos a la siguiente instruccion tras el factorial


sin:
	# Anadimos a la pila ra/s1/s2/s3/s4
	addi sp sp -20

    sw ra 16(sp)
    sw s4 12(sp)
    sw s3 8(sp)
    sw s2 4(sp)
    sw s1 0(sp)
    
    # fa0 es el parametro "x" que se le pasa a la función
    li t0 11   # Aprox para cumplir con un error menor de 0.001 (aprox)
    li t1 0    # Contador del bucle sin(n)
    li t3 0    # Contador del bucle dividendo
    li s1 2    # Numero 2
    li s2 0    # Guardamos (2n + 1)
    li s3 1    # Numero 1
    li s4 1    # Nos guardamos (-1)^n

    fcvt.s.w ft3 zero  # Almacenara toda la suma del sin (total)
    fcvt.s.w ft5 s3    # Nos guardamos x^(2n + 1)
    fmv.s ft4 fa0      # Nos guardamos el valor de fa0 para no perderlo (x)

    bucle_sen: bgt t1 t0 end_sen

        mul s2 s1 t1    # Nos guardamos el 2n
        addi s2 s2 1    # Nos guardamos el 2n + 1
        bucle_dividendo_sen: bge t3 s2 end_dividendo_sen  # Bucle para x^(2n + 1)
        
            fmul.s ft5 ft5 ft4
            addi t3 t3 1 # Incrementamos el contador que tiene que llegar a 2n + 1

            j bucle_dividendo_sen

        end_dividendo_sen:
            
            # Para ver si el resto es positivo o negativo
            rem s4 t1 s1   
            if_1_sen: beq s4 zero par_sen
				li s4 -1        # Si la n es impar (-1)^n es -1
                j salto_sen

			par_sen: li s4 1    # Si la n es par (-1)^n es 1

            salto_sen:
            fcvt.s.w ft6 s4     # Convertimos s4 (1/-1) a coma flotante (ft6)
            fmul.s ft1 ft6 ft5  # Almacena (-1)^n * x^(2n + 1)

        # Antes de llamar a factorial guardo los t que necesito en la funcion seno
        addi sp sp -16
        sw t0 12(sp)
        sw t1 8(sp)
        fsw ft3 4(sp)
        fsw ft4 0(sp)
        # No necesitamos guardar en la pila t3 y ft5 porque a la vuelta del factorial
        # reiniciamos los valores
        
        # Calculamos el (2n + 1)!
        # Usaremos el mismo registro s2 ya que tiene el valor de 2n + 1
        mv a0 s2            # Movemos s2 a a0 para pasar el parametro a la funcion factorial
        jal ra factorial    # Invocamos a factorial, devuelve resultado en fa0

        # Recupero de la pila los valores temporales que metimos antes de llamar a la funcion
        lw t0 12(sp)
        lw t1 8(sp)
        flw ft3 4(sp)
        flw ft4 0(sp)
        addi sp sp 16

        fmv.s ft0 fa0		# Movemos el resultado de fa0 a ft0
        fdiv.s ft2 ft1 ft0  # Dividendo / divisor = ((-1)^n * x^(2n + 1)) / (2n + 1)!
        fadd.s ft3 ft3 ft2  # Incrementamos la suma, guardo el resultado (ft3)

        li t3 0             # Reinicio del contador del dividendo
        addi t1 t1 1        # Incremento el contador (n)
        fcvt.s.w ft5 s3     # Reiniciamos el valor de x^(2n + 1) a 1
        j bucle_sen
        
    end_sen:
        fmv.s fa0 ft3       # Movemos el resultado a fa0 para devolverlo

        lw s1 0(sp)         # Recuperamos la pila
        lw s2 4(sp)
        lw s3 8(sp)
        lw s4 12(sp)
        lw ra 16(sp)
        
        addi sp sp 20
        jr ra               # Vamos a la instruccion posterior a la llamada a "sin"
