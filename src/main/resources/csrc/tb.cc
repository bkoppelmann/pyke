#ifdef VM_TRACE
#include <memory>
#include "verilated_vcd_c.h"
#endif
#include "verilated.h"
#include "verilated_vpi.h"
#include <iostream>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <getopt.h>

#include "VPykeTop.h"
#define VTop VPykeTop

static uint64_t trace_count = 0;
static uint8_t verbose = 0;
static char *imem_path = NULL;
static int insn_len;

double sc_time_stamp()
{
  return trace_count;
}

void cycle_clock(VTop *top, VerilatedVcdC *tfp)
{
    top->clock=1;
    top->eval();
    trace_count++;
#ifdef VM_TRACE
    tfp->dump(static_cast<vluint64_t>(trace_count));
#endif
    top->clock=0;
    top->eval();
    trace_count++;
#ifdef VM_TRACE
    tfp->dump(static_cast<vluint64_t>(trace_count));
#endif

}

int verbose_printf( const char *fmt, ...)
{
    va_list args;
    int ret=0;
    if (verbose) {
        va_start(args, fmt);
        ret = vprintf(fmt, args);
        va_end(args);
    }
    return ret;
}

void preload_imem(VTop *top, VerilatedVcdC *tfp, char* path, int insn_len)
{
    FILE *f = fopen(path, "r");
    char *line = NULL;
    size_t len = 0;
    ssize_t read;
    int i = 0;

    if (f == NULL) {
        printf("Cannot read '%s' file does not exist\n", path);
        exit(1);
    }

    verbose_printf("\n------------------------\n");
    verbose_printf("Preloading Imem from %s\n", imem_path);
    verbose_printf("------------------------\n\n");

    top->io_debug_fetch_en = 0;
    top->io_debug_imem_addr = 0x8000;
    cycle_clock(top, tfp);

    assert(insn_len % 8 == 0);
    uint num_bytes = insn_len / 8;
    while ((read = getline(&line, &len, f)) != -1) {
        for (int b = 0; b < num_bytes; b++) {
            top->io_debug_imem_addr += 1;
            top->io_debug_imem_val = strtoul(line, NULL, 16);
            cycle_clock(top, tfp);
        }
    }
    top->io_debug_fetch_en = 1;

    fclose(f);
}

void write_dmem_word(VTop *top, uint32_t idx, uint32_t word)
{
//    top->PykeTop__DOT__dmem__DOT__masked_mem_0[idx] = 0xff & word;
//    top->PykeTop__DOT__dmem__DOT__masked_mem_1[idx] = 0xff & (word >> 8) ;
//    top->PykeTop__DOT__dmem__DOT__masked_mem_2[idx] = 0xff & (word >> 16);
//    top->PykeTop__DOT__dmem__DOT__masked_mem_3[idx] = 0xff & (word >> 24);
}

void preload_dmem(VTop *top, char* path)
{
    FILE *f = fopen(path, "r");
    char *line = NULL;
    size_t len = 0;
    ssize_t read;
    int i = 0;

    if (f == NULL) {
        printf("Cannot read '%s' file does not exist\n", path);
    }


    while ((read = getline(&line, &len, f)) != -1) {
        write_dmem_word(top, i, strtol(line, NULL, 16));
        i++;
    }
    fclose(f);
}


#ifdef VM_TRACE
void parse_args(VTop *top, VerilatedVcdC *tfp, int argc, char **argv)
#else
void parse_args(VTop *top, ,int argc, char **argv)
#endif
{
    int opt;
    while((opt=getopt(argc, argv, "i:d:l:v:")) != -1) {
        switch(opt) {
        case 'i':
            imem_path = (char*)malloc(256*sizeof(char));
            strcpy(imem_path, optarg);
            break;
        case 'd':
            preload_dmem(top, optarg);
            break;
        case 'l':
            insn_len = strtol(optarg, NULL, 10);
            break;
        case 'v':
#ifdef VM_TRACE
            tfp->open(optarg);
#else
            printf("Not compiled with VCD trace\n");
#endif
            break;
        case 'V':
            verbose = true;
            break;
        }

    }
    for(; optind < argc; optind++){
        //printf("extra arguments: %s\n", argv[optind]);
    }
}


void reset(VTop *top, VerilatedVcdC *tfp)
{
    top->reset = 1;
    top->eval();
#ifdef VM_TRACE
    tfp->dump(static_cast<vluint64_t>(trace_count));
#endif
    trace_count++;

    // chisel regs are synchronous, so we need to
    // toggle the clock once during reset
    cycle_clock(top, tfp);

    top->reset = 0;
    top->eval();
#ifdef VM_TRACE
    tfp->dump(static_cast<vluint64_t>(trace_count));
#endif
    trace_count++;
}


int main(int argc, char **argv)
{
    int exit_code = 0;
#ifdef VM_TRACE
    FILE *vcdfile = NULL;
#endif
    Verilated::commandArgs(argc, argv);
    VTop *top = new VTop;
#ifdef VM_TRACE
    VerilatedVcdC* tfp = NULL;
    Verilated::traceEverOn(true);  // Verilator must compute traced signals
    tfp = new VerilatedVcdC;
    top->trace(tfp, 99);  // Trace 99 levels of hierarchy
    parse_args(top, tfp, argc, argv);
#else
    parse_args(top, argc, argv);
#endif
    if (imem_path != NULL) {
        preload_imem(top, tfp, imem_path, insn_len);
    }

    reset(top, tfp);

    // run a few clock cycles
    for(int i = 0; i < 10000; i++) {
        cycle_clock(top, tfp);
        if (top->io_success == 2) {
            exit_code = 0;
            verbose_printf("Success: %s\n", imem_path);
            goto early_exit;
        } else if (top->io_success == 1) {
            printf("Failed: %s\n", imem_path);
            exit_code = 1;
            goto early_exit;
        }
    }

    if (top->io_success == 0 || top->io_success == 1) {
        exit_code = 1;
    }

early_exit:
#ifdef VM_TRACE
    if (tfp)
        tfp->close();
    if (vcdfile)
        fclose(vcdfile);
#endif

    delete top;
    free(imem_path);
    return exit_code;
}
