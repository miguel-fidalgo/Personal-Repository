.text
#if(__APPLE__)
	.global _entry_point

_entry_point:
#else
	.global entry_point

entry_point:
#endif
	push %rbp	# save stack frame for C convention
	mov %rsp, %rbp

	pushq %rbx
	pushq %r12
	pushq %r13
	pushq %r14
	pushq %r15

	# beginning generated code
	movq $5, %rbx
	jmp loop1_cond
loop1_body:
	movq %rbx, %rcx
	movq $1, %rdi
	subq %rdi, %rcx
	movq %rcx, %rbx
loop1_cond:
	movq %rbx, %rcx
	movq $0, %rdi
	cmpq %rdi, %rcx
	jg loop1_body
	movq %rbx, %rcx
	movq %rcx, %rbx
	movq %rbx, %rax
	# end generated code
	# %rax contains the result

	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	mov %rbp, %rsp	# reset frame
	pop %rbp
	ret



