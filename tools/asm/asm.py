#! /usr/bin/env python3
import sys
import argparse

rrr_insn = ['add', 'sub', 'sll', 'srl', 'sra', 'xor', 'and', 'or', 'beq']
rri_insn = ['addi', 'lw']
rrs_insn = ['sw']
rr_insn  = ['jr']

instructions = rrr_insn + rri_insn + rr_insn + rrs_insn

start_addr = 0
linenum = 0
charnum = 0

labels = []
vliw_insn = []

class VLIWInstruction:
    def __init__(self, addr, insns):
        self.insns = insns
        self.addr  = addr
        self.insn_len = 16

    def encode(self):
        encoding = 0
        for num, insn in enumerate(self.insns):
            encoding = encoding | (insn.encode() << (num * self.insn_len))
        return str(hex(encoding))

class Instruction:
    def encode_op(self):
        mapping = {
            'add'  : 0b0000,
            'addi' : 0b0001,
            'sub'  : 0b0010,
            'lw'   : 0b0011,
            'sw'   : 0b0100,
            'sll'  : 0b0101,
            'srl'  : 0b0110,
            'sra'  : 0b0111,
            'xor'  : 0b1000,
            'and'  : 0b1001,
            'or'   : 0b1010,
            'beq'  : 0b1011,
            'jr'   : 0b1100,
        }
        return mapping[self.op]

    def encode_rs1(self):
        if self.rs1 > 15 or self.rs1 < 0:
            print_error("Invalid register '{}'".format(self.rs1))
        return self.rs1 << 8

    def encode_rs2(self):
        if self.rs2 > 15 or self.rs2 < 0:
            print_error("Invalid register '{}'".format(self.rs2))
        return self.rs2 << 12

    def encode_dst(self):
        if self.dst > 15 or self.dst < 0:
            print_error("Invalid register '{}'".format(self.dst))
        return self.dst << 4

    def encode_imm(self):
        if self.imm > 15 or self.imm < 0:
            print_error("Invalid imm '{}'".format(self.imm))
        return self.imm << 12

    def encode_simm(self):
        if self.imm > 15 or self.imm < 0:
            print_error("Invalid imm '{}'".format(self.imm))
        return self.imm << 4



class RRRInstruction(Instruction):
    def __init__(self, op, dst, rs1, rs2):
        self.op = op
        self.dst = dst
        self.rs1 = rs1
        self.rs2 = rs2

    def encode(self):
        return self.encode_op() | self.encode_dst() | self.encode_rs1() | self.encode_rs2()


class RRIInstruction(Instruction):
    def __init__(self, op, dst, rs1, imm):
        self.op = op
        self.dst = dst
        self.rs1 = rs1
        self.imm = imm

    def encode(self):
        return self.encode_op() | self.encode_dst() | self.encode_rs1() | self.encode_imm()

class RRSInstruction(Instruction):
    def __init__(self, op, source, base, imm):
        self.op = op
        self.rs1 = base
        self.rs2 = source
        self.imm = imm

    def encode(self):
        return self.encode_op() | self.encode_simm() | self.encode_rs1() | self.encode_rs2()



class RRInstruction(Instruction):
    def __init__(self, op, dst, rs1):
        self.op = op
        self.dst = dst
        self.rs1 = rs1

    def encode(self):
        return self.encode_op()

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

def parse_instruction(toks, tid, t, insns):
    global rrr_insn, rri_insn, rr_insn, rrs_insn

    if t in rrr_insn:
        dst = parse_reg(toks[tid+1])
        rs1 = parse_reg(toks[tid+2])
        rs2 = parse_reg(toks[tid+3])
        print("Found insn '{}' with args x{} x{} x{}".format(t, dst, rs1, rs2))
        insns.append(RRRInstruction(t, dst, rs1, rs2))
        return 4
    elif t in rri_insn:
        dst = parse_reg(toks[tid+1])
        rs1 = parse_reg(toks[tid+2])
        imm = parse_imm(toks[tid+3])
        insns.append(RRIInstruction(t, dst, rs1, imm))
        print("Found insn '{}' with args x{} x{} #{}".format(t, dst, rs1, imm))
        return 4
    elif t in rrs_insn:
        source = parse_reg(toks[tid+1])
        base = parse_reg(toks[tid+2])
        imm = parse_imm(toks[tid+3])
        insns.append(RRSInstruction(t, source, base, imm))
        print("Found insn '{}' with args x{} x{} #{}".format(t, source, base, imm))
        return 4
    elif t in rr_insn:
        dst = parse_reg(toks[tid+1])
        label = parse_label(toks[tid+2])
        insns.append(RRInstruction(t, dst, label))
        print("Found insn '{}' with args x{} :{}".format(t, dst, label))
        return 3

    print_error("Invalid instruction '{}'".format(t))

def parse_line(line, linenum):
    global instructions
    global vliw_insn
    labels = []
    ident = ''

    line = line.strip()
    line = line.expandtabs()

    end = line.find('#')
    if end >= 0:
        line = line[:end]

    toks = line.split()
    skip_tokens = 0

    insns = []

    for tid, t in enumerate(toks):
        if skip_tokens > 0:
            skip_tokens = skip_tokens - 1
            continue

        if t in instructions:
            skip_tokens = parse_instruction(toks, tid, t, insns)
        elif t.endswith(":"):
            print("Found jump-label '{}'".format(t[:-1]))
        elif t.startswith("."):
            skip_tokens = parse_asm_directive(toks, tid, t)
        else:
            print("ERROR: Unknown token '{}' at line {}".format(t, linenum))
            sys.exit(1)

    if len(insns) > 0: # we have a VLIW insn
        vliw_insn.append(VLIWInstruction(0, insns))

def parse_input(file_path):
    global linenum
    with open(file_path, 'r') as f:
        lines = f.readlines()
        for line in lines:
            linenum += 1
            parse_line(line, linenum)

def encode_insn():
    global vliw_insn
    res = []
    for vinsn in vliw_insn:
        res.append(vinsn.encode())
    return res

def main():
    parser = argparse.ArgumentParser(description='Pyke ASM')
    parser.add_argument('inputs', metavar='inputs', type=str, help='Input files to be assembled')
    parser.add_argument('-o', type=str, help='output file')
    args = parser.parse_args()

    parse_input(args.inputs)
    asm = encode_insn()
    if (args.o is not None):
        with open(args.o, 'w') as f:
            for line in asm:
                f.write(line + "\n")
    else: #print to stdout
        for line in asm:
            print(line)
main()
