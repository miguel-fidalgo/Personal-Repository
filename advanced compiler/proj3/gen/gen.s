.text
.global putchar, getchar, entry_point

################# FUNCTIONS #####################
#################################################


###################### MAIN #####################
entry_point:
	pushq %rbp	# save stack frame for calling convention
	movq %rsp, %rbp
	movq %rdi, heap(%rip)
	pushq %rbx
	pushq %r12
	pushq %r13
	pushq %r14
	pushq %r15

	movq $4, %rdi
	movq %rdi, %rax
	shlq $3, %rax
	movq heap(%rip), %rbx
	movq %rbx, %rdi
	addq %rax, %rbx
	movq %rbx, heap(%rip)
	movq %rdi, %rsi
	movq $0, %rdx
	movq $3, %rcx
	movq %rsi, %rax
	movq %rdx, %rbx
	movq %rcx, %rcx
	movq %rcx, (%rax, %rbx, 8)
	movq $0, %rsi
	movq %rsi, %rdi
	movq %rdi, %rax
	
	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	movq %rbp, %rsp	# reset frame
	popq %rbp
	ret
#################################################


#################### DATA #######################

.data
heap:	.quad 0
#################################################
