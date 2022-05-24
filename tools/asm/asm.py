#! /usr/bin/env python3
import sys
import argparse

rrr_insn = ['add', 'sub', 'sll', 'srl', 'sra', 'xor', 'and', 'or', 'beq']
rri_insn = ['addi', 'lw', 'sw']
rr_insn  = ['jr']

instructions = rrr_insn + rri_insn + rr_insn

start_addr = 0
linenum = 0
charnum = 0

class VLIWInstruction:
    pass

class Instruction:
    pass

class Label:
    def __init__(self, ident):
        self.ident = ident

    def print(self):
        print(self.ident)

def print_error(msg):
    print("ERROR: " + msg + " at line {}".format(linenum))
    sys.exit(1)

def parse_asm_directive(toks, tid, t):
    global start_addr
    global linenum, charnum
    if t[1:] == "org":
        addr_str = toks[tid+1]
        if addr_str.startswith("0x"):
            start_addr = int(addr_str, base=16)
        else:
            start_addr = int(addr_str)
        print("Setting startaddr to {}".format(hex(start_addr)))
        return 1
    else:
        print_error("Unknown asm directive")

def parse_label(tok):
    if tok.startswith(':'):
        return tok[1:]

    print_error("Expected label, got '{}'".format(tok))


def parse_reg(tok):
    if tok.startswith('%'):
        reg = int(tok[1:])
        if reg < 15:
            return reg
        print_error("Invalid register '{}'".format(reg))

    print_error("Expected register, got '{}'".format(tok))

def parse_imm(tok):
    global linenum
    if tok.startswith('$'):
        imm = int(tok[1:])
        return imm
    print_error("Expected immediate, got '{}'".format(tok))

def parse_instruction(toks, tid, t):
    global rrr_insn, rri_insn, rr_insn

    if t in rrr_insn:
        dst = parse_reg(toks[tid+1])
        rs1 = parse_reg(toks[tid+2])
        rs2 = parse_reg(toks[tid+3])
        print("Found insn '{}' with args x{} x{} x{}".format(t, dst, rs1, rs2))
        return 4
    elif t in rri_insn:
        dst = parse_reg(toks[tid+1])
        rs1 = parse_reg(toks[tid+2])
        imm = parse_imm(toks[tid+3])
        print("Found insn '{}' with args x{} x{} #{}".format(t, dst, rs1, imm))
        return 4
    elif t in rr_insn:
        dst = parse_reg(toks[tid+1])
        label = parse_label(toks[tid+2])
        print("Found insn '{}' with args x{} :{}".format(t, dst, label))
        return 3

    print_error("Invalid instruction '{}'".format(t))

def parse_line(line, linenum):
    global instructions
    labels = []
    ident = ''

    line = line.strip()
    line = line.expandtabs()

    end = line.find('#')
    if end >= 0:
        line = line[:end]

    toks = line.split()
    skip_tokens = 0

    for tid, t in enumerate(toks):
        if skip_tokens > 0:
            skip_tokens = skip_tokens - 1
            continue

        if t in instructions:
            skip_tokens = parse_instruction(toks, tid, t)
        elif t.endswith(":"):
            print("Found jump-label '{}'".format(t[:-1]))
        elif t.startswith("."):
            skip_tokens = parse_asm_directive(toks, tid, t)
        else:
            print("ERROR: Unknown token '{}' at line {}".format(t, linenum))
            sys.exit(1)

def parse_input(file_path):
    global linenum
    with open(file_path, 'r') as f:
        lines = f.readlines()
        for line in lines:
            linenum += 1
            parse_line(line, linenum)

def main():
    parser = argparse.ArgumentParser(description='Pyke ASM')
    parser.add_argument('inputs', metavar='inputs', type=str, help='Input files to be assembled')
    parser.add_argument('-o', type=str, help='output file')
    args = parser.parse_args()

    parse_input(args.inputs)
main()
