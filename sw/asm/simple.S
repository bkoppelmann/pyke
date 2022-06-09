#include "makros.S.inc"
# simple asm test
# %1 = 1
# %2 = 2
# %3 = 1 + 2

# %10 = 0xa3e7 = io_success
# %14 = 15
.org 0x8000
start:
    LOAD_RESULT_ADDR(%11) 
    addi %2 %0 $2
    jr %0 :start      | sw %2 %11 $0
