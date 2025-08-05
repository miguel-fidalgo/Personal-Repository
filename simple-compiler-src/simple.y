/*
 *
 * simple.y: simple parser for the simple "C" language
 * 
 */

%token	<string_val> WORD

%token 	NOTOKEN LPARENT RPARENT LBRACE RBRACE LCURLY RCURLY COMA SEMICOLON EQUAL STRING_CONST LONG LONGSTAR VOID CHARSTAR CHARSTARSTAR INTEGER_CONST AMPERSAND OROR ANDAND EQUALEQUAL NOTEQUAL LESS GREAT LESSEQUAL GREATEQUAL PLUS MINUS TIMES DIVIDE PERCENT IF ELSE WHILE DO FOR CONTINUE BREAK RETURN

%union	{
		char   *string_val;
		int nargs;
		int my_nlabel;
	}

%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

int yylex();
int yyerror(const char * s);

extern int line_number;
const char * input_file;
char * asm_file;
FILE * fasm;

#define MAX_ARGS 5
int nargs;
char * args_table[MAX_ARGS];

#define MAX_GLOBALS 100
int nglobals = 0;
char * global_vars_table[MAX_GLOBALS];
int global_type[MAX_GLOBALS];

#define MAX_LOCALS 32
int nlocals = 0;
char * local_vars_table[MAX_LOCALS];
int local_type[MAX_LOCALS];

#define MAX_STRINGS 100
int nstrings = 0;
char * string_table[MAX_STRINGS];

char *regStk[]={ "rbx", "r10", "r13", "r14", "r15"};
char nregStk = sizeof(regStk)/sizeof(char*);

char *regStkB[]={ "bl", "r10b", "r13b", "r14b", "r15b"};

char *regArgs[]={ "rdi", "rsi", "rdx", "rcx", "r8", "r9"};
char nregArgs = sizeof(regArgs)/sizeof(char*);

int tag = 0;

int top = 0;

int nargs =0;

int if_label = 0;
int loop_label = 0;

// We define a number tag for each variable type, so that we can differenciate them
# define CHARSTAR_ 0
# define CHARSTARSTAR_ 1
# define LONG_ 2
# define LONGSTAR_ 3
# define VOID_ 4

int type;

%}

%%

goal:	program
	;

program :
        function_or_var_list;

function_or_var_list:
        function_or_var_list function
        | function_or_var_list global_var
        | /*empty */
	;

function:
		var_type WORD
		{
			nlocals = 0;
			fprintf(fasm, "\t.text\n");
			fprintf(fasm, ".globl %s\n", $2);
			fprintf(fasm, "%s:\n", $2);

			fprintf(fasm, "\t# Save Frame pointer\n");
			fprintf(fasm, "\tpushq %%rbp\n");
			fprintf(fasm, "\tmovq %%rsp,%%rbp\n");

			fprintf(fasm, "\t# Save registers. \n");
			fprintf(fasm, "\tpushq %%rbx\n");
			fprintf(fasm, "\tpushq %%rbx\n");
			fprintf(fasm, "\tpushq %%r10\n");
			fprintf(fasm, "\tpushq %%r13\n");
			fprintf(fasm, "\tpushq %%r14\n");
			fprintf(fasm, "\tpushq %%r15\n");
			fprintf(fasm, "\tsubq $%d, %%rsp\n\n", MAX_LOCALS * 8);
		}
		LPARENT arguments RPARENT 
		{
			if (nlocals > 0) {
				fprintf(fasm, "\t# Store function parameters\n");
				for (int i = 0; i < nlocals; i++) {
					fprintf(fasm, "\tmovq %%%s, %d(%%rsp)\n", regArgs[i], 8 * i);
				}	
			}
		}
		compound_statement
		{
			fprintf(fasm, "\t# Restore registers\n");
			fprintf(fasm, "\taddq $%d, %%rsp\n", MAX_LOCALS * 8);
			fprintf(fasm, "\tpopq %%r15\n");
			fprintf(fasm, "\tpopq %%r14\n");
			fprintf(fasm, "\tpopq %%r13\n");
			fprintf(fasm, "\tpopq %%r10\n");
			fprintf(fasm, "\tpopq %%rbx\n");
			fprintf(fasm, "\tpopq %%rbx\n");
			fprintf(fasm, "\tleave\n");
			fprintf(fasm, "\tret\n\n");
		}
	;

arg_list:
		arg
		| arg_list COMA arg
 	;

arguments:
		arg_list
		| /*empty*/
	;

arg: var_type WORD {
		char* id = $2;
		assert(nlocals < MAX_LOCALS);
		local_vars_table[nlocals] = id;
		local_type[nlocals] = type;
		nlocals++;
		}
	;

global_var: 
        var_type global_var_list SEMICOLON;

global_var_list: WORD {
			char * id = $1; // Equivalent to $<string_val>1 
			fprintf(fasm,"\t# global id=%s\n", id);
			fprintf(fasm,"\t.data\n");
			fprintf(fasm,"\t.comm %s, 8", id);
			fprintf(fasm,"\n\n");
			global_vars_table[nglobals] = id;
			global_type[nglobals] = type;
			nglobals++;
        }
		| global_var_list COMA WORD {
			char * id = $3; // Equivalent to $<string_val>3 
			fprintf(fasm,"\t# global id=%s\n", id);
			fprintf(fasm,"\t.data\n");
			fprintf(fasm,"\t.comm %s, 8", id);
			fprintf(fasm,"\n\n");
			global_vars_table[nglobals] = id;
			global_type[nglobals] = type;
			nglobals++;
		}
    ;

var_type: CHARSTAR {
			type = CHARSTAR_;
		}
		| CHARSTARSTAR {
			type = CHARSTARSTAR_;
		}
		| LONG {
			type = LONG_;
		}
		| LONGSTAR {
			type = LONGSTAR_;
		}
		| VOID {
			type = VOID_;
		}
	;

assignment:
		WORD EQUAL expression {
			char * id = $1;
			// id may be a local var o a global var. See if it is a local var
			// Iterate over the local vars to see if it is there
			int localvar = -1;
			int vartype;
			for (int i = 0; i < nlocals; i++) {
				if (strcmp(id, local_vars_table[i]) == 0) {
					localvar = i;
					vartype = local_type[i];
					break;
				}
			}
			if (localvar != -1) {
				// Local var
				fprintf(fasm, "\t# We're assigning a local var: %s.\n", id);
				fprintf(fasm, "\tmovq %%%s, %d(%%rsp)\n", regStk[top-1], 8 * localvar);
			} else {
				// Global var
				fprintf(fasm, "\t# We're assigning a global var: %s.\n", id);
				fprintf(fasm, "\tmovq %%%s, %s\n", regStk[top-1], id);
			}
			top--; // Decrement top as we've used the value
		}
		| WORD LBRACE expression RBRACE EQUAL expression {
			fprintf(fasm, "\t# Assigning an array value\n");
			char * id = $<string_val>1;
			// id may be a local var o a global var. See if it is a local var
			// Iterate over local vars to see if it is there
			int localvar = -1;
			int vartype = -1;
			for (int i = 0; i < nlocals; i++) {
				if (strcmp(id, local_vars_table[i]) == 0) {
					localvar = i;
					vartype = local_type[i];
					break;
				}
			}
			if (localvar != -1) {
				// Local var
				fprintf(fasm, "\t# Push the address of %s\n", id);
				fprintf(fasm, "\tmovq %d(%%rsp), %%%s\n", localvar * 8, regStk[top]);
			} else {
				// Global var
				for (int i = 0; i < nlocals; i++) {
					if (strcmp(id, global_vars_table[i]) == 0) {
						localvar = i;
						vartype = global_type[i];
						break;
					}
				}
				fprintf(fasm, "\t# Push the address of %s\n", id);
				fprintf(fasm, "\tmovq %s, %%%s\n", id, regStk[top]);
			}
			top++;

			// Multiply the index by the size based on vartype
			if (vartype == 0) {
				fprintf(fasm, "\t# Type = char\n");
				fprintf(fasm, "\timulq $1, %%%s\n", regStk[top-3]);
			} else {
				fprintf(fasm, "\t# Type = long\n");
				fprintf(fasm, "\timulq $8, %%%s\n", regStk[top-3]);
			}

			// Add base address and offset
			fprintf(fasm, "\taddq %%%s, %%%s\n", regStk[top-3], regStk[top-1]);

			fprintf(fasm, "\t# Assign the value %%%s to the address in %%%s\n", regStk[top-2], regStk[top-1]);
			fprintf(fasm, "\tmovq %%%s, (%%%s)\n", regStk[top-2], regStk[top-1]);

			top -= 3;
		}
	;

call:
		WORD LPARENT call_arguments RPARENT {
		char * funcName = $<string_val>1;
		int nargs = $<nargs>3;
		int i;
		fprintf(fasm, "\t# func=%s nargs=%d\n", funcName, nargs);
		fprintf(fasm, "\t# Move values from reg stack to reg args\n");
		// Move arguments from regStk to regArgs in order
		for (i = 0; i < nargs; i++) {
		fprintf(fasm, "\tmovq %%%s, %%%s\n", regStk[top - nargs + i], regArgs[i]);
		}
		top -= nargs; // Adjust top after moving arguments
		if (!strcmp(funcName, "printf")) {
			// printf has a variable number of arguments
			// and it need the following
			fprintf(fasm, "\tmovl $0, %%eax\n");
		}
		fprintf(fasm, "\tcall %s\n", funcName);
		fprintf(fasm, "\tmovq %%rax, %%%s\n\n", regStk[top]);
		top++;
		}
      ;

call_arg_list:
		expression {
			$<nargs>$=1;
		}
		| call_arg_list COMA expression {
			$<nargs>$++;
		}

	 ;

call_arguments:
		call_arg_list { $<nargs>$=$<nargs>1; }
		| /*empty*/ { $<nargs>$=0;}
	;

expression :
		logical_or_expr
	;

logical_or_expr:
		logical_and_expr
		| logical_or_expr OROR logical_and_expr {
			fprintf(fasm,"\n\t# ||\n");
			if (top<nregStk) {
				fprintf(fasm, "\torq %%%s, %%%s\n", regStk[top-1], regStk[top-2]);
				top--;
			}
		}
		
	;

logical_and_expr:
		equality_expr
		| logical_and_expr ANDAND equality_expr {
			fprintf(fasm,"\n\t# &&\n");
			if (top<nregStk) {
				fprintf(fasm, "\tandq %%%s, %%%s\n", regStk[top-1], regStk[top-2]);
				top--;
			}
		}
    ;

equality_expr:
		relational_expr
		| equality_expr EQUALEQUAL relational_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsete %%al\t\t\t# Set byte if equal (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
		| equality_expr NOTEQUAL relational_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsetne %%al\t\t\t# Set byte if not equal (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
    ;

relational_expr:
		additive_expr
		| relational_expr LESS additive_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsetl %%al\t\t\t# Set byte if less (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
		| relational_expr GREAT additive_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsetg %%al\t\t\t# Set byte if greater (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
		| relational_expr LESSEQUAL additive_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsetle %%al\t\t\t# Set byte if less or equal (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
		| relational_expr GREATEQUAL additive_expr {
			fprintf(fasm, "\tmovq $0, %%rax\t\t# Zero %%rax\n");
			fprintf(fasm, "\tcmpq %%%s, %%%s\t\t# Compare top of the stack\n",
					regStk[top-1], regStk[top-2]);
			fprintf(fasm, "\tsetge %%al\t\t\t# Set byte if greater or equal (%%al lowest byte of %%rax)\n");
			fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
			top--;
		}
    ;

additive_expr:
		multiplicative_expr
		| additive_expr PLUS multiplicative_expr {
			fprintf(fasm,"\n\t# +\n");
			if (top<nregStk) {
				fprintf(fasm, "\taddq %%%s, %%%s\n", regStk[top-1], regStk[top-2]);
				top--;
			}
		}
		| additive_expr MINUS multiplicative_expr {
			fprintf(fasm,"\n\t# -\n");
			if (top<nregStk) {
				fprintf(fasm, "\tsubq %%%s,%%%s\n", regStk[top-1], regStk[top-2]);
				top--;
			}
		}
	;

multiplicative_expr:
		primary_expr
		| multiplicative_expr TIMES primary_expr {
			fprintf(fasm,"\n\t# *\n");
			if (top<nregStk) {
				fprintf(fasm, "\timulq %%%s,%%%s\n", regStk[top-1], regStk[top-2]);
				top--;
			}
		}
		| multiplicative_expr DIVIDE primary_expr {
			fprintf(fasm, "\n\t# /\n");
			if (top < nregStk) {
				fprintf(fasm, "\t# Dividend goes into %%rax\n");
				fprintf(fasm, "\tmovq %%%s, %%rax\n", regStk[top-2]);
				fprintf(fasm, "\t# Sign-extend rax into rdx:rax before division\n");
				fprintf(fasm, "\tcqto\n");
				fprintf(fasm, "\tidivq %%%s\n", regStk[top-1]);
				fprintf(fasm, "\t# Now we have quotient in %%rax\n");
				fprintf(fasm, "\tmovq %%rax, %%%s\n", regStk[top-2]);
				top--;
			}						
		}
		| multiplicative_expr PERCENT primary_expr {
			fprintf(fasm, "\n\t# %%\n");
			if (top < nregStk) {
				fprintf(fasm, "\t# Dividend goes into %%rax\n");
				fprintf(fasm, "\tmovq %%%s, %%rax\n", regStk[top-2]);
				fprintf(fasm, "\t# Sign-extend rax into rdx:rax before division\n");
				fprintf(fasm, "\tcqto\n");
				fprintf(fasm, "\tidivq %%%s\n", regStk[top-1]);
				fprintf(fasm, "\t# Now we have reminder in %%rdx\n");
				fprintf(fasm, "\tmovq %%rdx, %%%s\n", regStk[top-2]);
				top--;
			}	
		}
	;

primary_expr:
		STRING_CONST {
			// Add string to string table.
			// String table will be produced later
			string_table[nstrings]=$<string_val>1;
			fprintf(fasm, "\t# push string %s top=%d\n", $<string_val>1, top);
			if (top<nregStk) {
				fprintf(fasm, "\tmovq $string%d, %%%s\n", nstrings, regStk[top]);
				//fprintf(fasm, "\tmovq $%s,%%%s\n", $<string_val>1, regStk[top]);
				top++;
			}
			nstrings++;
		}
		| call
		| WORD {
			char * id = $<string_val>1;
			// id may be a local var o a global var. See if it is a local var
			// Iterate over local vars to see if it is there
			int localvar = -1;
			for (int i = 0; i < nlocals; i++) {
				if (strcmp(id, local_vars_table[i]) == 0) {
					localvar = i;
					break;
				}
			}
			if (localvar != -1) {
				// Local var (including parameters)
				fprintf(fasm, "\t# Push local var %s\n", id);
				fprintf(fasm, "\tmovq %d(%%rsp), %%%s\n", localvar * 8, regStk[top]);
			} else {
				// Global var
				fprintf(fasm, "\tmovq %s, %%%s\n", id, regStk[top]);
			}
			top++;
		}
		| WORD LBRACE expression RBRACE {
			fprintf(fasm, "\t# Assigning an array value\n");
			char * id = $<string_val>1;
			// id may be a local var o a global var. See if it is a local var
			// Iterate over local vars to see if it is there
			int localvar = -1;
			int vartype = -1;
			for (int i = 0; i < nlocals; i++) {
				if (strcmp(id, local_vars_table[i]) == 0) {
					localvar = i;
					vartype = local_type[i];
					break;
				}
			}
			if (localvar != -1) {
				// Local var
				fprintf(fasm, "\t# Push the address of %s\n", id);
				fprintf(fasm, "\tmovq %d(%%rsp), %%%s\n", localvar * 8, regStk[top]);
			} else {
				// Global var
				for (int i = 0; i < nlocals; i++) {
					if (strcmp(id, global_vars_table[i]) == 0) {
						localvar = i;
						vartype = global_type[i];
						break;
					}
				}
				fprintf(fasm, "\tmovq %s, %%%s\n", id, regStk[top]);
			}
			top++;

			// Multiply the index by the size based on vartype
			if (vartype == 0) {
				fprintf(fasm, "\t# Type = char\n");
				fprintf(fasm, "\timulq $1, %%%s\n", regStk[top-2]);
			} else {
				fprintf(fasm, "\t# Type = long\n");
				fprintf(fasm, "\timulq $8, %%%s\n", regStk[top-2]);
			}

			// Add base address and offset
			fprintf(fasm, "\taddq %%%s, %%%s\n", regStk[top-2], regStk[top-1]);
		
			fprintf(fasm, "\tmovq (%%%s), %%%s\n", regStk[top-1], regStk[top-2]);

			if (vartype == 0) {
				fprintf(fasm, "\tmovb %%%s, %%r9b\n", regStkB[top-2]);
				fprintf(fasm, "\tmovq $0, %%%s\n", regStk[top-2]);
				fprintf(fasm, "\tmovb %%r9b, %%%s\n", regStkB[top-2]);
			}
			top--;
		}
		| AMPERSAND WORD {
			char * id = $<string_val>2; // Get the variable name
			// id may be a local var o a global var. See if it is a local var
			// Iterate over local vars to see if it is there
			int localvar = -1;
			for (int i = 0; i < nlocals; i++) {
				if (strcmp(id, local_vars_table[i]) == 0) {
					localvar = i;
					break;
				}
			}
			if (localvar != -1) {
				// Local var
				fprintf(fasm, "\t# Get the address of local var %s\n", id);
				fprintf(fasm, "\tleaq %d(%%rsp), %%%s\n", localvar * 8, regStk[top]);
			} else {
				// Global var
				fprintf(fasm, "\tleaq %s, %%%s\n", id, regStk[top]);
			}
			top++;
		}
		| INTEGER_CONST {
			fprintf(fasm, "\t# push %s\n", $<string_val>1);
			if (top<nregStk) {
				fprintf(fasm, "\tmovq $%s, %%%s\n", $<string_val>1, regStk[top]);
				top++;
			}
		}
		| LPARENT expression RPARENT
	;

compound_statement:
		LCURLY statement_list RCURLY
	;

statement_list:
		statement_list statement
		| /*empty*/
	;

local_var:
        var_type local_var_list SEMICOLON;

local_var_list: 
		WORD {
			// First local variable
			char * id = $1;
			assert(nlocals < MAX_LOCALS);
			local_vars_table[nlocals] = id;
			local_type[nlocals] = type;
			nlocals++;
		}
		| local_var_list COMA WORD {
			char * id = $<string_val>3;
			assert(nlocals < MAX_LOCALS);
			local_vars_table[nlocals] = id;
			local_type[nlocals] = type;
			nlocals++;
		}
	;

statement:
		assignment SEMICOLON
		| call SEMICOLON { 
			top= 0; /* Reset register stack */ 
		}
		| local_var
		| compound_statement
		| IF LPARENT expression RPARENT {
			// Act1: After parsing the expression
			$<my_nlabel>1 = if_label++;  // Generate a unique label number
			fprintf(fasm, "\t# IF statement\n");
			fprintf(fasm, "\tcmpq $0, %%%s\n", regStk[top - 1]);  // Compare expression result to zero
			fprintf(fasm, "\tje else_%d\n", $<my_nlabel>1);  // Jump to else label if zero
			top--;  // Pop the expression result from the stack
		}
		statement {
			// Act2: After parsing the 'then' statement
			fprintf(fasm, "\tjmp endif_%d\n", $<my_nlabel>1);  // Jump to end of if to skip else
			fprintf(fasm, "else_%d:\n", $<my_nlabel>1);  // Define else label
		}
		else_optional {
			// Act3: After parsing 'else_optional'
			fprintf(fasm, "endif_%d:\n", $<my_nlabel>1);  // Define end if label
		}
		| WHILE LPARENT {
			// act 1: generates a unique label for the start of the loop
			$<my_nlabel>0=loop_label;
			tag = loop_label;
			loop_label++;
			fprintf(fasm, "loop_start_%d:\n", $<my_nlabel>0);
		}
		expression RPARENT {
			// act2: compares the result of the expression to zero and jumps to 
			// the end of the label if false
			fprintf(fasm, "\tcmpq $0, %%%s\n", regStk[top - 1]);  // Compare expression result to zero
			fprintf(fasm, "\tje loop_end_%d\n", $<my_nlabel>0);
			top--;
		}
		statement {
			// act3: jumps back to the start label after executing the loop body
			// and defines the end label
			fprintf(fasm, "\tjmp loop_start_%d\n", $<my_nlabel>0);
			fprintf(fasm, "loop_end_%d:\n", $<my_nlabel>0);
		}
		| DO {
			// act 1: generates a unique label before the loop
			$<my_nlabel>0=loop_label;
			tag = loop_label;
			loop_label++;
			fprintf(fasm, "loop_start_%d:\n", $<my_nlabel>0);
		} statement WHILE LPARENT expression RPARENT SEMICOLON {
			// act 2: after evaluating the condition
			fprintf(fasm, "\tcmpq $0, %%%s\n", regStk[top - 1]);  // Compare expression result to zero
			fprintf(fasm, "\tjne loop_start_%d\n", $<my_nlabel>0);  // Jump if not zero
			fprintf(fasm, "\tjmp loop_end_%d\n", $<my_nlabel>0);
			fprintf(fasm, "loop_end_%d:\n", $<my_nlabel>0);
			top--;
		}
		| FOR LPARENT assignment SEMICOLON{
			$<my_nlabel>0 = loop_label;
			tag = loop_label;
			loop_label++;
			fprintf(fasm, "for_start_%d:\n", $<my_nlabel>0);
		} expression SEMICOLON {
			fprintf(fasm, "\tcmpq $0, %%%s\n", regStk[top - 1]);  // Compare expression result to zero
			fprintf(fasm, "\tje loop_end_%d\n", $<my_nlabel>0);  // Jump to loop end if 0
			fprintf(fasm, "\tjne for_body_%d\n", $<my_nlabel>0);  // Jump to body if not 0
			fprintf(fasm, "loop_start_%d:\n", $<my_nlabel>0);  // Define loop counter
			top--;
		}
		assignment RPARENT {
			// After the assignment, the counter i will be updated
			fprintf(fasm, "\tjmp for_start_%d\n", $<my_nlabel>0);  // Jump to for start to check the condition again
			fprintf(fasm, "for_body_%d:\n", $<my_nlabel>0);  // Define for body
		} statement {
			fprintf(fasm, "\tjmp loop_start_%d\n", $<my_nlabel>0);  // Jump to loop counter to update it
			fprintf(fasm, "loop_end_%d:\n", $<my_nlabel>0);  // Define for end
		}
		| jump_statement
	;

else_optional:
		ELSE  statement
		| /* empty */
	;

jump_statement:
		CONTINUE SEMICOLON {
			fprintf(fasm, "\t# CONTINUE\n");
			fprintf(fasm, "\tjmp loop_start_%d\n", tag);
		}
		| BREAK SEMICOLON {
			fprintf(fasm, "\t# BREAK\n");
			fprintf(fasm, "\tjmp loop_end_%d\n", tag);
		}
		| RETURN expression SEMICOLON {
			fprintf(fasm, "\tmovq %%rbx, %%rax\n");
			fprintf(fasm, "\t# Restore registers\n");
			fprintf(fasm, "\taddq $%d, %%rsp\n", MAX_LOCALS * 8);
			fprintf(fasm, "\tpopq %%r15\n");
			fprintf(fasm, "\tpopq %%r14\n");
			fprintf(fasm, "\tpopq %%r13\n");
			fprintf(fasm, "\tpopq %%r10\n");
			fprintf(fasm, "\tpopq %%rbx\n");
			fprintf(fasm, "\tpopq %%rbx\n");
			top = 0;
			fprintf(fasm, "\tleave\n");
			fprintf(fasm, "\tret\n\n");
		}
	;

%%

void yyset_in (FILE *  in_str );

int
yyerror(const char * s)
{
	fprintf(stderr,"%s:%d: %s\n", input_file, line_number, s);
}


int
main(int argc, char **argv)
{
	printf("-------------WARNING: You need to implement global and local vars ------\n");
	printf("------------- or you may get problems with top------\n");
	
	// Make sure there are enough arguments
	if (argc <2) {
		fprintf(stderr, "Usage: simple file\n");
		exit(1);
	}

	// Get file name
	input_file = strdup(argv[1]);

	// Verify that is a .c file
	int len = strlen(input_file);
	if (len < 2 || input_file[len-2]!='.' || input_file[len-1]!='c') {
		fprintf(stderr, "Error: file extension is not .c\n");
		exit(1);
	}

	// Get assembly file name
	asm_file = strdup(input_file);
	asm_file[len-1]='s';

	// Open file to compile
	FILE * f = fopen(input_file, "r");
	if (f==NULL) {
		fprintf(stderr, "Cannot open file %s\n", input_file);
		perror("fopen");
		exit(1);
	}

	// Create assembly file (fopen with write "w")
	fasm = fopen(asm_file, "w");
	if (fasm==NULL) {
		fprintf(stderr, "Cannot open file %s\n", asm_file);
		perror("fopen");
		exit(1);
	}

	// Uncomment for debugging
	//fasm = stderr;

	// Create compilation file
	// 
	yyset_in(f);
	yyparse(); // Call the parser that yac has generated

	// Generate string table
	int i;
	for (i = 0; i<nstrings; i++) {
		fprintf(fasm, "string%d:\n", i);
		fprintf(fasm, "\t.string %s\n", string_table[i]);
	}

	fclose(f);
	fclose(fasm);

	return 0;
}
