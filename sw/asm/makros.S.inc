#define LOAD_RESULT_ADDR(reg) \
    addi %11 %0 $5   | addi %13 %0 $15 _NL\
    slli %11 %11 $13 | add %0 %0 %0 _NL\
    slli %13 %13 $6 | addi %14 %0 $1 _NL\
    addi %12 %0 $7  | slli %14 %14 $5 _NL\
    add %13 %13 %14 | add %11 %11 %12 _NL\
    add %11 %11 %13 | xor %1 %1 %2 _NL\
    slli reg %11 $2
