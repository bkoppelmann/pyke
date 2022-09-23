#include "makros.S.inc"
.org 0x8000

LOAD_RESULT_ADDR(%11)

test1:
    addi %1 %0 $4  | addi %2 %0 $7
    addi %3 %0 $3  | xor %1 %1 %2
    # %11 = 0xa000 + 999
    bne %1 %3 :fail | addi %2 %0 $2 # we assume success here
success: 
    add %0 %0 %0  | sw %2 %11 $0
fail:
    addi %2 %0 $1
    add %0 %0 %0  | sw %2 %11 $0
