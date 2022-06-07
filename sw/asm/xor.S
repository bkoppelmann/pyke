.org 0x8000

test1:
    addi %1 %0 $4  | addi %11 %0 $5                
    addi %2 %0 $7  | slli %11 %11 $13
    addi %3 %0 $3  | addi %13 %0 $15

    # calculate rslt addr
    slli %13 %13 $6 | addi %14 %0 $1
    addi %12 %0 $7  | slli %14 %14 $5
    add %13 %13 %14 | add %11 %11 %12
    add %11 %11 %13 | xor %1 %1 %2
    slli %11 %11 $2

    # %11 = 0xa000 + 999 
    bne %1 %3 :fail | addi %2 %0 $2 # we assume success here
success: 
    add %0 %0 %0  | sw %2 %11 $0
fail:
    addi %2 %0 $1
    add %0 %0 %0  | sw %2 %11 $0
