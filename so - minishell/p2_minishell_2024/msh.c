//P2-SSOO-23/24

//  MSH main file
// Write your msh source code here

//#include "parser.h"
#include <stddef.h>			/* NULL */
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>

#define MAX_COMMANDS 8

// files in case of redirection
char filev[3][64];

//to store the execvp second parameter
char *argv_execvp[8];

void siginthandler(int param)
{
	printf("****  Exiting MSH **** \n");
	//signal(SIGINT, siginthandler);
	exit(0);
}


struct command
{
  // Store the number of commands in argvv
  int num_commands;
  // Store the number of arguments of each command
  int *args;
  // Store the commands
  char ***argvv;
  // Store the I/O redirection
  char filev[3][64];
  // Store if the command is executed in background or foreground
  int in_background;
};

int history_size = 20;
struct command * history;
int head = 0;
int tail = 0;
int n_elem = 0;

void free_command(struct command *cmd)
{
    if((*cmd).argvv != NULL)
    {
        char **argv;
        for (; (*cmd).argvv && *(*cmd).argvv; (*cmd).argvv++)
        {
            for (argv = *(*cmd).argvv; argv && *argv; argv++)
            {
                if(*argv){
                    free(*argv);
                    *argv = NULL;
                }
            }
        }
    }
    free((*cmd).args);
}

void store_command(char ***argvv, char filev[3][64], int in_background, struct command* cmd)
{
    int num_commands = 0;
    while(argvv[num_commands] != NULL){
        num_commands++;
    }

    for(int f=0;f < 3; f++)
    {
        if(strcmp(filev[f], "0") != 0)
        {
            strcpy((*cmd).filev[f], filev[f]);
        }
        else{
            strcpy((*cmd).filev[f], "0");
        }
    }

    (*cmd).in_background = in_background;
    (*cmd).num_commands = num_commands-1;
    (*cmd).argvv = (char ***) calloc((num_commands) ,sizeof(char **));
    (*cmd).args = (int*) calloc(num_commands , sizeof(int));

    for( int i = 0; i < num_commands; i++)
    {
        int args= 0;
        while( argvv[i][args] != NULL ){
            args++;
        }
        (*cmd).args[i] = args;
        (*cmd).argvv[i] = (char **) calloc((args+1) ,sizeof(char *));
        int j;
        for (j=0; j<args; j++)
        {
            (*cmd).argvv[i][j] = (char *)calloc(strlen(argvv[i][j]),sizeof(char));
            strcpy((*cmd).argvv[i][j], argvv[i][j] );
        }
    }
}


/**
 * Get the command with its parameters for execvp
 * Execute this instruction before run an execvp to obtain the complete command
 * @param argvv
 * @param num_command
 * @return
 */
void getCompleteCommand(char*** argvv, int num_command) {
	//reset first
	for(int j = 0; j < 8; j++)
		argv_execvp[j] = NULL;

	int i = 0;
	for ( i = 0; argvv[num_command][i] != NULL; i++)
		argv_execvp[i] = argvv[num_command][i];
}


/**
 * mycalc <operando1> <add/mul/div> <operando2>
 * 
 * @param number1
 * @param operation
 * @param number2
 * @return
*/

int mycalc(int num1, char op[3], int num2, int Acc){

    char commands[3][4] = {"add", "mul", "div"};

    if (strcmp(op, commands[0]) == 0){ // add
        Acc = atoi(getenv("Acc"));
        Acc += (num1 + num2);
        fprintf(stderr, "[OK] %d + %d = %d; Acc %d\n", num1, num2, (num1 + num2), Acc);
        char buf[100];
        sprintf(buf, "%d", Acc);
        setenv("Acc", buf, 1);
        return (num1 + num2); /* Devolvemos la suma para después poder incrementar la variable
        global Acc*/
    }    

    else if (strcmp(op, commands[1]) == 0){ // mull

        fprintf(stderr, "[OK] %d * %d = %d\n", num1, num2, (num1*num2));
        return 0;
    }

    else if (strcmp(op, commands[2]) == 0){ // div

        if (num2 == 0)
        {
            printf("[ERROR] La división entre 0 no está permitida\n");
            return -1;
        }
        
        fprintf(stderr, "[OK] %d / %d = %d; Resto %d\n", num1, num2, (num1/num2), (num1%num2));
        return 0;
    }
    printf("[ERROR] La estructura del comando es mycalc<operando_1><add/mul/div><operando_2>\n");
    return -1;
}

void print_history(){

    for (int i = 0; i < n_elem - 1; i++){
        // Primer mandato de todos
        fprintf(stderr, "%d", i);
        fprintf(stderr, " %s", history[i].argvv[0][0]);

        /* Este fragmento se encarga de imprimir los argumentos del primer mandato porque
        el resto de ellos hay que separarlos con "|" */

        for (int j=1; j < *history[i].args; j++){
            fprintf(stderr,  " %s", history[i].argvv[0][j]);
        }

        /*Para mostrar los mandatos anidados*/
        for (int j=1; j < history[i].num_commands; j++){ 
            fprintf(stderr, " | %s", history[i].argvv[j][0]);
            /*Para mostrar los parámetros de los mandatos anidados*/
            int t = 1;
            while (history[i].argvv[j][t] != NULL){
                fprintf(stderr, " %s", history[i].argvv[j][t]);
                t++;
            }
        }
        if (strcmp((history[i].filev[0]), "0\0") != 0){ // Para mostrar redirección de entrada
            fprintf(stderr, " < %s", history[i].filev[0]);
        }
        if (strcmp((history[i].filev[1]), "0\0") != 0){ // Para mostrar redirección de salida
            fprintf(stderr, " > %s", history[i].filev[1]);
        }
        if (strcmp((history[i].filev[2]), "0\0") != 0){ // Para mostrar redirección de error
            fprintf(stderr, " !> %s", history[i].filev[2]);
        }
        if (history[i].in_background != 0){ // Para mostrar background "&"
            fprintf(stderr, " &");
        }
        fprintf(stderr, "\n");
    }
}

int execute_history(int posicion, char ***argvv, int in_background, int run_history){
    
    if (n_elem >= posicion){
        fprintf(stderr, "Ejecutando el comando  %d\n", posicion);

        /* Para almacenar los mandatos */
        for (int j=0; j < history[posicion].num_commands; j++){
            //memcmp(argvv[j][0], history[posicion].argvv[j][0], sizeof(history[posicion].argvv[j][0]));
            //strcpy(argvv[j][0], history[posicion].argvv[j][0]); 
            argvv[j][0] = history[posicion].argvv[j][0];
            /* Para almacenar los parámetros de los mandatos anidados */
            int t = 1;
            while (history[posicion].argvv[j][t] != NULL){
                //memcmp(argvv[j][t], history[posicion].argvv[j][t], sizeof(history[posicion].argvv[j][t]));
                //strcpy(argvv[j][t], history[posicion].argvv[j][t]); 
                argvv[j][t] = history[posicion].argvv[j][t];
                t++;
            }
        }
        if (strcmp((history[posicion].filev[0]), "0\0") != 0){ // Para mostrar redirección de entrada
            strcpy(filev[0], history[(head + posicion) % history_size].filev[0]);
        }
        if (strcmp((history[posicion].filev[1]), "0\0") != 0){ // Para mostrar redirección de salida
            strcpy(filev[1], history[(head + posicion) % history_size].filev[1]);
        }
        if (strcmp((history[posicion].filev[2]), "0\0") != 0){ // Para mostrar redirección de error
            strcpy(filev[2], history[(head + posicion) % history_size].filev[2]);
        }
        if (history[posicion].in_background != 0){ // Para mostrar background "&"
            fprintf(stderr, " &");
        }
        return 1;
    }else{
        printf("ERROR: Comando no encontrado\n");
        return 0;
    }
}  

/**
 * Main sheell  Loop  
 */
int main(int argc, char* argv[]){
    
	/**** Do not delete this code.****/
	int end = 0; 
	int executed_cmd_lines = -1;
	char *cmd_line = NULL;
	char *cmd_lines[10];

	if (!isatty(STDIN_FILENO)) {
		cmd_line = (char*)malloc(100);
		while (scanf(" %[^\n]", cmd_line) != EOF){
			if(strlen(cmd_line) <= 0) return 0;
			cmd_lines[end] = (char*)malloc(strlen(cmd_line)+1);
			strcpy(cmd_lines[end], cmd_line);
			end++;
			fflush(stdin);
			fflush(stdout);
		}
	}

	/*********************************/

	char ***argvv = NULL;
	int num_commands;

	history = (struct command*) malloc(history_size *sizeof(struct command));
	int run_history = 0;

    int Acc = 0;
    char buf[100];
    sprintf(buf, "%d", Acc);
    setenv("Acc", buf, 1);

	while (1){
    
        int internal = 0, correcto = 0; // Para comprobar si se ha pedido un mandato interno
		int status = 0;
		int command_counter = 0;
		int in_background = 0;

        int myhistory = 0; // Para saber si se ha ejecutado un myhistory <num>

		signal(SIGINT, siginthandler);

		if (run_history){
            
            /* Para ver cuantos comandos son si viene del myhistory ya que al no pasar 
            por el parser, tengo que contarlos yo a mano */

            while (*argvv[command_counter] != NULL)command_counter++;          
            run_history=0;
        }
        else{
            // Prompt 
            write(STDERR_FILENO, "MSH>>", strlen("MSH>>"));

            // Get command
            //********** DO NOT MODIFY THIS PART. IT DISTINGUISH BETWEEN NORMAL/CORRECTION MODE***************
            executed_cmd_lines++;
            if( end != 0 && executed_cmd_lines < end) {
                command_counter = read_command_correction(&argvv, filev, &in_background, cmd_lines[executed_cmd_lines]);
            }
            else if( end != 0 && executed_cmd_lines == end)
                return 0;
            else
                command_counter = read_command(&argvv, filev, &in_background); //NORMAL MODE
        }
		//************************************************************************************************


		/************************ STUDENTS CODE ********************************/

	   if (command_counter > 0) {
			if (command_counter > MAX_COMMANDS){
				//printf("Error: Maximum number of commands is %d \n", MAX_COMMANDS);
			}
			else {
				// Print command
				//print_command(argvv, filev, in_background);
			}
		}
        // command_counter numero de comandos en total
        //printf("Numero de comandos: %d\n", command_counter);

        /* El siguiente fragmento de código se encarga de añadir los mandatos a la lista
        para posteriormente usarlos en el myhistory. En caso de que no se hayan añadido
        20 mandatos, los añade al final y ya, pero si son 20, borra el primero y desplaza
        todos a la izquierda para hacer hueco al que acaba de llamarse al final para poder
        añadirse y guardarse el orden */

        getCompleteCommand(argvv, 0); // Obtenemos el mandato completo de la ejecución

        /* Creamos una estructura tipo "command" para guardar el comando que se acaba 
        de ejecutar para posteriormente guardarlo en el array "history" para su posterior
        impresión o ejecución*/

        if(n_elem < 20){
            struct command cmd;
            store_command(argvv, filev, in_background, &cmd); // fallo
            history[n_elem] = cmd;
            n_elem++;

        }else if(n_elem >= 20){
            // free_command(&history[0]);
            for(int i=0; i<19; i++){
                history[i] = history[i + 1];
            }
            struct command cmd;
            store_command(argvv, filev, in_background, &cmd);
            history[19] = cmd;
        }

        if (strcmp(argv_execvp[0], "myhistory") == 0){ // En caso de que se llame a myhistory

            /* Usamos la variable internal para que no pase por los procesos
            padre y así evitar que el mandato "myhistory" pase por el execvp
            y de error */

            internal = 1;

            if (argv_execvp[2] != NULL){
                
                printf("La estructura del mandato myhistory es: myhistory / myhistory<valor>\n");
            }
            else if (argv_execvp[1] == NULL){
                print_history();
            }
            else{

                /* Uso esta variable para que la función que coja del historial
                no se ejecute en esta ejecución, si no que espere a la siguiente ejecucuón con
                la variable run_history a 1 y así evite el parser */

                myhistory = 1; 
                int x = 0;
                for(int i=0; argvv[i][x] != NULL; i++){
                    while (argvv[i][x] != NULL){
                        argvv[i][x] = NULL;
                        x++;
                    }
                }

                // for (int i = 0; argvv[i] != NULL; i++) {
                //     for (int j = 0; argvv[i][j] != NULL; j++) {
                //         // Asignamos NULL a cada elemento
                //         argvv[i][j] = NULL;
                //     }
                // }

                /* Reseteamos el vector filev[] */
                int j=0;
                while (strcmp(filev[j], "0\0") != 0)strcmp(filev[j], "0\0");

                in_background = 0;
                run_history = execute_history(atoi(argv_execvp[1]), argvv, in_background, run_history);
                command_counter = 1;
                internal = 0;
            }
        }

        if (strcmp(argv_execvp[0], "mycalc") == 0){ // En caso de que se llame a mycalc

            internal = 1;
            for(int i=1; i < 4; i++){ // Comprobamos que al menos haya 4 elementos insertados
                if (argv_execvp[i] == NULL)
                {
                    correcto = 1;
                    printf("[ERROR] La estructura del comando es mycalc<operando_1><add/mul/div><operando_2>\n");
                    break;
                }
            }
            if(correcto == 0){
                int respuesta = mycalc(atoi(argv_execvp[1]), argv_execvp[2], atoi(argv_execvp[3]), Acc);
                if(strcmp(argv_execvp[2], "add") == 0)Acc+=respuesta;
            }
            /* Reinicio argvv yo porque el store_command no lo hace */
            for(int i = 0; i < 4; i++){
                argvv[0][i] = NULL;
            }
        }
        
        if(command_counter > 1 && internal == 0 && myhistory == 0){
            
            int pid, pipes[command_counter-1][2]; // [pipe1, pip2, pipe3......]
            int ids[command_counter];
            for(int i=0; i<command_counter -1; i++){
                pipe(pipes[i]);
            }

            for (int i = 0; i < command_counter; i++){
                pid = fork();
                switch (pid){
                case -1:
                    printf("Error en el fork\n");
                    exit(-1);

                case 0: // Hijo

                    /* En caso de que haya redirección de error, modificamos el descriptor
                    de fichero para todos los hijos */

                    if(strcmp(filev[2], "0\0") != 0){
                        close(STDERR_FILENO);
                        int fd = open(filev[2], O_CREAT | O_WRONLY, 0666);
                        if (fd == -1){
                            printf("Error al abrir\n");
                        }
                    }

                    if (i == 0){ // Primera pipe

                        /* Para el primer mandato cerramos su salida y la unimos con la primera
                        del pipe y en caso de que haya redirección de entrada también cerramos 
                        la entrada y abrimos el fichero correspondiente para que así ocupe 
                        el lugar de la entrada */

                        close(STDOUT_FILENO);
                        dup(pipes[0][1]);

                        if (strcmp(filev[0], "0\0") != 0){ // Si hay un fichero
                            close(STDIN_FILENO);
                            int fd = open(filev[0], O_RDONLY);
                            if (fd == -1){
                                perror("error");
                            }
                        }  
                                        
                    }else if (i==(command_counter -1)){ // Última pipe 

                        /* Para el último mandato cerramos su entrada y la unimos con la entrada
                        de la pipe que ocupa la posición (i - 1) y en caso de que haya redirección
                        de salida también cerramos la salida y abrimos el fichero correspondiente 
                        para que así ocupe el lugar de la salida */
                 
                        close(STDIN_FILENO);
                        dup(pipes[i-1][0]);
                        
                        if(strcmp(filev[1], "0\0") != 0){
                            close(STDOUT_FILENO);
                            int fd = open(filev[1], O_CREAT | O_WRONLY, 0666);
                            if (fd == -1){
                                perror("error");
                            }
                        }
                    }else{ // Todas las pipes
                        
                        /* Para el resto de mandatos simplemente enlazamos la entrada
                        de cada uno con la entrada del pipe en la posición (i - 1)
                        y su salida con la del pipe en la posición (i) */

                        close(STDIN_FILENO);
                        dup(pipes[i-1][0]);
                        close(STDOUT_FILENO);
                        dup(pipes[i][1]);
                    }
                    for(int j=0;j<command_counter -1;j++){ // Cerramos todas las pipes

                        /* Una vez adaptado el descriptor de fichero, cerramos
                        todas las pipes que resten */
                        close(pipes[j][0]);
                        close(pipes[j][1]);
                    }

                    /* Ejecutamos el comando */
                    getCompleteCommand(argvv, i);                   
                    if (execvp(argv_execvp[0], argv_execvp) == -1) {perror("error");}

                    /* Si llega aquí es que ha fallado y el hijo termina*/
                    exit(-1);

                default: // Padre
                    //ids[i] = pid;
                    break;
                }      
            } 
            for(int j=0;j<command_counter -1;j++){
                
                /* Cerramos también todas las pipes del padre que sobren*/
                close(pipes[j][0]);
                close(pipes[j][1]);
            }
            // for (int j=0; j<command_counter; j++){ // Esperamos a todos los hijos
            //     waitpid(ids[j], NULL, 0);
            // }  
            if (in_background == 0){ // En caso de que el proceso sea en background
                while(wait(&status)!= pid);
            }
            
        }else if (command_counter == 1 && internal == 0 && myhistory == 0){ /* Si el número de comandos == 1 */

            int pid;
            pid = fork();
            switch (pid){
                case -1:
                    printf("Error en el fork\n");
                    exit(-1);

                case 0: // Hijo

                    /* Al ser solo un mandato, en caso de que haya redirecciones de
                    cualquier tipo se cerrará su salida o entrada correspondiente
                    para ser remplazada por su fichero */

                    if(strcmp(filev[2], "0\0") != 0){ // Si hay un fichero
                        close(STDERR_FILENO);
                        int fd = open(filev[2], O_CREAT | O_WRONLY, 0666);
                        if (fd == -1){
                            printf("Error al abrir\n");
                        }
                    }
                    if (strcmp(filev[0], "0\0") != 0) { // Si hay un fichero
                        close(STDIN_FILENO);
                        int fd = open(filev[0], O_RDONLY);
                        if (fd == -1){
                            perror("error");
                        }        
                    }
                    if(strcmp(filev[1], "0\0") != 0){ // Si hay un fichero
                        close(STDOUT_FILENO);
                        int fd = open(filev[1], O_CREAT | O_WRONLY, 0666);
                        if (fd == -1){
                            perror("error");
                        }
                    }

                    /* Ejecutamos el comando */
                    getCompleteCommand(argvv, 0);
                    
                    if (execvp(argv_execvp[0], argv_execvp) == -1) {perror("error");}
                    /* Si llega aquí es que ha fallado y el hijo termina*/
                    exit(-1);
            
                default: // Padre
                    if (in_background == 0){ // En caso de que el proceso sea en background
                        while(wait(&status)!= pid);
                    }
            }
        }
    }
	return 0;
}
//P2-SSOO-23/24

//  MSH main file
// Write your msh source code here