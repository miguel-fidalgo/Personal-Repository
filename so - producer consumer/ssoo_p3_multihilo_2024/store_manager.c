//SSOO-P3 23/24

#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <pthread.h>
#include "queue.h"
#include <string.h>
#include <sys/types.h>

#define MAX_LINE_LENGTH 100 /* Máxima longitud de una línea*/


int precio_compra[5] = {2, 5, 15, 25, 100};
int precio_venta[5] = {3, 10, 20, 40, 125};

pthread_mutex_t mutex; /* Creamos un mutex para controlar la ejecución*/
pthread_cond_t vacio;  /* Creamos una variable condicional para cuando se vacie un elemento del buffer*/
pthread_cond_t lleno;  /* Creamos una variable condicional para cuando se inserte un elemento en el buffer*/

/* -------------HILO PRODUCTOR-------------*/

typedef struct argProd {
    queue *q; /* Le pasamos la cola del programa*/
    struct element *buff;
    int size;
} argProd;

/* -------------HILO CONSUMIDOR-------------*/

typedef struct argCon {
    queue *q; /* Le pasamos la cola del programa*/
    int elem; /* Le pasamos el número de elementos que debería ejecutar cada hilo consumidor*/
    int beneficio;
    int stock_parcial[5];
} argCon;

typedef struct retCon { /* Para devolver el beneficio y el stock parcial*/
    int beneficio;
    int stock_parcial[5];
} retCon;


/* Creamos una función para obtener la primera línea del archivo
y así ver cuantas operaciones vamos a tener que realizar en total*/

char *maxOperaciones(char *entrada) {
    FILE *archivo;
    char *linea = (char *)malloc(MAX_LINE_LENGTH * sizeof(char));

    /* Abrimos el archivo en modo lectura*/
    archivo = fopen(entrada, "r");

    /* Vemos si el archivo se ha abierto correctamente*/
    if (archivo == NULL) {
        printf("No se pudo abrir el archivo.\n");
        exit(-1); /* No podemos user return -1 ya que la función tiene que devolver un char*/
    }

    /* Leemos la primera línea del archivo*/
    if (fgets(linea, MAX_LINE_LENGTH, archivo) == NULL) {
        printf("El archivo está vacío\n");
        fclose(archivo);
        exit(-1); /* No podemos user return -1 ya que la función tiene que devolver un char*/
    }
    /* Cerramos el archivo*/
    fclose(archivo);
    return linea;
}

void *guardarOp(char *entrada, struct element *operacion, int total) {
    FILE *archivo;
    char *linea = (char *)malloc(MAX_LINE_LENGTH * sizeof(char));

    /* Abrimos el archivo en modo lectura*/
    archivo = fopen(entrada, "r");

    /* Vemos si el archivo se ha abierto correctamente*/
    if (archivo == NULL) {
        printf("No se pudo abrir el archivo.\n");
        exit(-1); /* No podemos user return -1 ya que la función tiene que devolver un char*/
    }

    /* Iteramos en todo el archivo*/
    if (fgets(linea, MAX_LINE_LENGTH, archivo) == NULL) {
        printf("El archivo está vacío.\n");
        fclose(archivo);
        exit(1);
    }
    else {
        int i = 0;
        /* Para guardar la información en el array*/
        while (fscanf(archivo, "%d %s %d\n", &operacion[i].product_id, operacion[i].op, &operacion[i].units) == 3 && i < total) {
            i++;
        }
    }
    /* Cerramos el archivo*/
    fclose(archivo);
    return 0;
}


void *funProductores(void *arg) {

    argProd *argumentosProd = (argProd*)arg;

    for (int i = 0; i < argumentosProd->size; i++) {
        pthread_mutex_lock(&mutex);
        while (queue_full(argumentosProd->q) == 1) { /* Si la cola está ya llena no se puede insertar mas*/
            pthread_cond_wait(&vacio, &mutex); /* Esperamos hasta que se vacie un elemento, esto lo hará el consumidor*/
        }
        queue_put(argumentosProd->q, &argumentosProd->buff[i]); /* Si nos dan paso, añadimos el elemento a la cola*/
        /* Notificamos que se ha añadido un elemento en caso de que un consumidor esté esperando
        a que se añada para poder leer*/
        pthread_cond_signal(&lleno);
        pthread_mutex_unlock(&mutex); /* Desbloqueamos el mutex*/
    }
    pthread_exit(NULL);
}


void *funConsumidores(void *arg) {

    argCon *argumentosCons = (argCon*)arg;

    for (int i = 0; i < argumentosCons->elem; i++) {
        pthread_mutex_lock(&mutex);
        while (queue_empty(argumentosCons->q) == 1) { /* Si la cola está vacia no se puede leer*/
            pthread_cond_wait(&lleno, &mutex); /* Esperamos hasta que se inserte al menos un elemento para poder leer*/
        }

        struct element *retorno = queue_get(argumentosCons->q); /* Si nos dan paso sacamos de la cola*/
        /* Gestionamos las compras o ventas y añadimos o quitamos profits*/

        if (retorno != NULL) {
            if (strcmp(retorno->op, "PURCHASE") == 0) {

                /* Calculo del stock*/
                //product_stock[retorno->product_id - 1]+= retorno->units;
                argumentosCons->stock_parcial[retorno->product_id - 1] += retorno->units;

                /* Calculo del beneficio*/
                int beneficio = precio_compra[retorno->product_id - 1] * retorno->units;
                argumentosCons->beneficio -= beneficio;
                //profits -= beneficio;

            }
            else if (strcmp(retorno->op, "SALE") == 0) {

                /* Calculo del stock*/
                //product_stock[retorno->product_id - 1]-= retorno->units;
                argumentosCons->stock_parcial[retorno->product_id - 1] -= retorno->units;

                /* Calculo del beneficio*/
                int beneficio = precio_venta[retorno->product_id - 1] * retorno->units;
                argumentosCons->beneficio += beneficio;
                //profits += beneficio;
            }
        }
        /* Notificamos que se ha eliminado un elemento en caso de que un productor esté esperando
        a que se elimine para poder escribir*/
        pthread_cond_signal(&vacio);
        pthread_mutex_unlock(&mutex);
    }
    retCon *devolver = (retCon*) malloc(sizeof(retCon));
    devolver->beneficio = argumentosCons->beneficio;
    for (int i = 0; i < 5; i++) {
        devolver->stock_parcial[i] = argumentosCons->stock_parcial[i];
    }

    pthread_exit((void*)devolver); /* Devolvemos el beneficio y el stock parcial calculado por dicho hilo*/
}


int main(int argc, char *argv[]) { /* <file_name><num_producers><num_consumers><buff_size>*/

    /* Comprobamos que el número de argumentos es el correcto*/ 
    if (argc != 5) {
        printf("El número de argumentos no es correcto\n");
        return -1;
    }

    if (atoi(argv[2]) < 1 || atoi(argv[3]) < 1) {
        printf("Tiene que haber más de un productor y consumidor\n");
        return -1;
    }

    int profits = 0;
    int product_stock[5] = {0}; /* Todos a 0 por defecto*/

    /* Recogemos el valor de la primera línea del archivo y 
    convertimos dicho char a int para poder operar con el*/

    int total = atoi(maxOperaciones(argv[1])); 

    /* Una vez obtenido el número de operaciones que vamos a realizar, reservamos
    memoria para guardarlas*/

    struct element *operaciones;
    operaciones = (struct element*) malloc(total *sizeof(struct element));

    /* Llamamos a la función para guardar todas las operaciones del fichero en un array a parte
    para posteriormetne trabajar con él */

    guardarOp(argv[1], operaciones, total); /* Le pasamos el nombre del fichero y el array donde queremos guardar*/

    pthread_t *productores = (pthread_t*) malloc(atoi(argv[2]) *sizeof(pthread_t)); /* Hilos productores*/
    pthread_t *consumidores = (pthread_t*) malloc(atoi(argv[3]) *sizeof(pthread_t)); /* Hilos consumidores*/

    /* Inicialicamos los mutex y variables condicionales*/
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&lleno, NULL);
    pthread_cond_init(&vacio, NULL);

    queue *q = queue_init(atoi(argv[4])); /* Creamos la cola*/


    /* -------------------------CREACIÓN DE LOS HIJOS PRODUCTORES-----------------------------*/


    /* Para un repartoProd equitativo de la carga del fichero entre el número de
    hilos productores que haya, dividiremos el número de operaciones a realizar
    entre el número productores que tenemos*/

    int imparProd = 0;
    int hechoProd = 0;
    int repartoProd = total / atoi(argv[2]);
    int repartoResPro = 0; /* En caso de que la división no sea exacta*/


    if ((total % atoi(argv[2])) != 0) {
        imparProd = 1; /* Para añadir más elementos al último hilo en caso de que la división no sea exacta*/
    }
    /* Para asignar los x primeros elementos al primer productor y así sucesivamente*/
    int count = 0, restante = total - (total - repartoProd);

    /* Creamos una matriz para guardar todos los argumentos de los productores*/
    argProd *argumentosProd = (argProd*)malloc(atoi(argv[2]) *sizeof(argProd));

    for (int i = 0; i < atoi(argv[2]); i++) { /* Creamos todos los hilos productores*/

        /* Creamos los argumentos para cada hilo productor*/

        argumentosProd[i].q = q; /* Le pasamos la cola del buffer circular*/

        if ((atoi(argv[2]) - i == 1) && imparProd == 1) { /* Por si es el último hilo y la división no ha sido exacta, reservar más espacio*/
            argumentosProd[i].buff = (struct element*)malloc((total - (repartoProd * ((atoi(argv[2]) - 1)))) *sizeof(struct element));
            argumentosProd[i].size = (total - (repartoProd * ((atoi(argv[2]) - 1)))); /* Tamaño del buffer*/
            repartoResPro = total - (repartoProd * ((atoi(argv[2]) - 1)));
        }
        else {
            argumentosProd[i].buff = (struct element*)malloc(repartoProd *sizeof(struct element));
            argumentosProd[i].size = repartoProd; /* Tamaño del buffer*/
        }

        /* Agrego al último hilo las operaciones que sean necesarias en caso de que la división no 
        haya sido exacta para así repartir el archivo al compelto*/

        if ((atoi(argv[2]) - i == 1) && imparProd == 1) {
            for (int t = 0; t < repartoResPro; t++) {
                argumentosProd[i].buff[t]  = operaciones[(repartoProd * (atoi(argv[2]) - 1)) + t];
            }
            hechoProd = 1;
        }

        /* Reparto el resto de operaciones entre todos los hilos de manera equitativa*/

        int x = 0;
        if (hechoProd == 0) {
            for (int j = count; j < restante; j++) {
                argumentosProd[i].buff[x] = operaciones[j];
                x++;
            }
        }

        /* Creamos el hilo productor asignadole su función y sus argumentos propios*/

        pthread_create(&productores[i], NULL, &funProductores, (void*)&argumentosProd[i]);
        count = count + repartoProd;
        restante = restante + repartoProd;
    }


    /* -------------------------CREACIÓN DE LOS HIJOS CONSUMIDORES-----------------------------*/


    /* Para un repartoCon equitativo de la carga del fichero entre el número de
    hilos consumidores que haya, dividiremos el número de operaciones a realizar
    entre el número consumidores que tenemos*/

    int imparCon = 0;
    int hechoCon = 0;
    int repartoCon = total / atoi(argv[3]);

    if (total % atoi(argv[3]) != 0) { /* Para añadir más lecturas al último hilo en caso de que la división no sea exacta*/
        imparCon = 1;
    }

    /* Para asignar los x primeros elementos al primer productor y así sucesivamente*/
    count = 0, restante = total - (total - repartoCon);

    argCon *argumentosCon = (argCon*)malloc(atoi(argv[3]) *sizeof(argCon));

    for (int i = 0; i < atoi(argv[3]); i++) { /* Creamos todos los hilos consumidores*/

        argumentosCon[i].elem = 0; /* Inicialmente son 0*/
        argumentosCon[i].q = q;
        argumentosCon[i].beneficio = 0;
        for(int j=0; j < 5; j++){
          argumentosCon[i].stock_parcial[j] = 0;
        }

        if ((atoi(argv[3]) - i == 1) && imparCon == 1) { /* Por si es el último hilo y la división no ha sido exacta, reservar más espacio*/
            int repartoResCon = total - (repartoCon * ((atoi(argv[3]) - 1)));
            for (int t = 0; t < repartoResCon; t++) {
                argumentosCon[atoi(argv[3]) - 1].elem++;
            }
            hechoCon = 1;
        }

        /* Reparto el resto de operaciones entre todos los hilos de manera equitativa*/

        if (hechoCon == 0) {
            for (int j = count; j < restante; j++) {
                argumentosCon[i].elem++;
            }
        }

        /* Creamos el hilo consumidor asignadole su función y sus argumentos propios*/

        pthread_create(&consumidores[i], NULL, &funConsumidores, (void*)&argumentosCon[i]);
        count = count + repartoCon;
        restante = restante + repartoCon;
    }


    /* -------------------------RECOGEMOS A AMBOS HILOS-----------------------------*/


    for (int i = 0; i < atoi(argv[2]); i++) { /* Recogemos a todos los hilos consumidores*/
        pthread_join(productores[i], NULL);
    }
    free(productores);

    for (int i = 0; i < atoi(argv[3]); i++) { /* Recogemos a todos los hilos consumidores*/
		
		void *retorno;
        pthread_join(consumidores[i], &retorno);
		retCon *devuelto = (retCon*)retorno;
		profits += devuelto->beneficio;
		for (int i = 0; i < 5; i++){
			product_stock[i] += devuelto->stock_parcial[i];
		}
    }
    free(consumidores);
	
    /* Destruimos los dos mutex y variables condicionales*/
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&lleno);
    pthread_cond_destroy(&vacio);

	free(argumentosProd);
	free(argumentosCon);

    // Output
    printf("Total: %d euros\n", profits);
    printf("Stock:\n");
    printf("  Product 1: %d\n", product_stock[0]);
    printf("  Product 2: %d\n", product_stock[1]);
    printf("  Product 3: %d\n", product_stock[2]);
    printf("  Product 4: %d\n", product_stock[3]);
    printf("  Product 5: %d\n", product_stock[4]);
    queue_destroy(q); /* Destruimos la cola*/

    return 0;
}
