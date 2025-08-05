//SSOO-P3 23/24

#ifndef HEADER_FILE
#define HEADER_FILE

/* Modificamos el valor de op ya que en el fichero es un texto
no un número*/

struct element {
    int product_id; // Identificador del producto
    char op[20];    // Operación
    int units;      // Unidades del producto
};

typedef struct {
    struct element* buffer;
    int head; // Índice del primer elemento en el buffer
    int tail; // Índice del último elemento en el buffer
    int size; // Tamaño máximo del buffer
} queue;


queue* queue_init (int size); /* Le pasaremos el buffer*/
int queue_destroy (queue *q);
int queue_put (queue *q, struct element* elem);
struct element * queue_get(queue *q);
int queue_empty (queue *q);
int queue_full(queue *q);

#endif
