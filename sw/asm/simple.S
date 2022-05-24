# simple asm test
.org 0x80000000
start:
    addi %1 %0 $1 | addi %2 %0 $2 # test foo
    jr %0 :start   | add %3 %1 %2
