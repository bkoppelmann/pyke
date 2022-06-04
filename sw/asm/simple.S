# simple asm test
# %1 = 1
# %2 = 2
# %3 = 1 + 2

# %10 = 0xa3e7 = io_success
# %14 = 15
.org 0x8000
start:
    addi %1 %0 $1     | addi %2 %0 $2
    addi %14 %0 $15   | addi %13 %0 $6
    sll  %10 %14 %13  | addi %5 %0 $5
    sll  %12 %1 %5    | addi %6 %0 $13
    addi %12 %12 $7   | add %3 %1 %2
    add %10 %10 %12   | sll %11 %5 %6 # create 999 | create 0xa000
    add %10 %10 %11   | addi %0 %0 $0
    jr %0 :start      | sw %2 %10 $0
