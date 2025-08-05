//SSOO-P3 23/24

#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include "queue.h"


/* Creamos el buffer*/
queue* queue_init(int size){
    queue* q = (queue*)malloc(sizeof(queue));
    q->buffer = (struct element*)malloc(size * sizeof(struct element));
    q->head = 0; // Comienza el head en 0
    q->tail = 0; // Comienza el tail en 0
    q->size = size;
    return q;
}

/* Creamos una función auxiliar para determinar si la cola está llena o no*/
int queue_full(queue* q) {
    /* En caso de que la siguiente posición de la cola sea la cabecera, la cola estará llena*/
    if((q->head + 1) % q->size == q->tail){
        //printf("No inserto está llena\n");
        return 1; /* Si está llena devolvemos 1*/
    }
    return 0; /* Si no está llena devolvemos 0*/
}

/* Para añadir elementos al buffer circular*/
int queue_put(queue* q, struct element* x) {
    if (queue_full(q) == 0){
        q->buffer[q->head] = *x; /* Añadimos en la cabecera la operación a ejecutar*/
        q->head = (q->head + 1) % q->size; /* Incrementamos la posición de la cabecera*/
        return 0;
    }
    return -1;
}

/* Creamos una función auxiliar para determinar si la cola está vacia o no*/
int queue_empty(queue *q){
  /* En caso de que la cabecera y la cola estén en el mismo lugar, la cola estará vacia*/
  if(q->head == q->tail){
    //printf("No recojo esta vaía\n");
    return 1;
  } /* Si está vacia devolvemos 1*/
  return 0; /* Si no está vacia deolvemos 0*/
}

/* Para eliminar un elemento de la cola y devolverlo*/
struct element *queue_get(queue* q){
    struct element *eliminado = NULL; // Creamos una estructura para devolver el objeto borrado{
    if (queue_empty(q) == 0) {
        //printf("Extraigo\n");
        /* Nos guardamos el elemento para devolver el que está en una posición más que la cola*/  
        eliminado = &q->buffer[(q->tail)];
        q->tail = (q->tail + 1) % q->size; /* Avanzamos la cola*/
    }
    return eliminado; // Devuelve el elemento eliminado, es NULL si la cola está vacía
}

// Destruir la cola y liberar los recursos
int queue_destroy(queue* q) {
    free(q->buffer);
    free(q);
    return 0;
}