#! /usr/bin/env python3
import sys
import argparse

rrr_insn = ['add', 'sub', 'sll', 'srl', 'sra', 'xor', 'and', 'or']
rri_insn = ['addi', 'slli', 'lw']
rrs_insn = ['sw']
br_insn = ['bne', 'beq']
rr_insn  = ['jr']

instructions = rrr_insn + rri_insn + rr_insn + rrs_insn + br_insn

current_addr = 0
charnum = 0
verbose = False

all_labels = {}
vliw_insn = []

class VLIWInstruction:
    def __init__(self, addr, insns):
        self.insns = insns
        self.addr  = addr
        self.insn_len = 16

    def resolve_br_labels(self):
        for insn in self.insns:
            if type(insn) is BRInstruction:
                insn.resolve_br_label(self.addr)

    def encode(self):
        encoding = 0
        for num, insn in enumerate(self.insns):
            encoding = encoding | (insn.encode() << (num * self.insn_len))
        return str(hex(encoding))

class Instruction:
    def __init__(self):
        self.op  = 'addi'
        self.rs1 = 0
        self.rs2 = 0
        self.dst = 0
        self.imm = 0
        self.label = ''

    def print_error(self, msg):
        print("ERROR: " + msg)
        sys.exit(1)

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
            'slli' : 0b1101,
            'bne'  : 0b1110,
        }
        return mapping[self.op]

    def encode_rs1(self):
        if self.rs1 > 15 or self.rs1 < 0:
            self.print_error("Invalid register '{}'".format(self.rs1))
        return self.rs1 << 8

    def encode_rs2(self):
        if self.rs2 > 15 or self.rs2 < 0:
            self.print_error("Invalid register '{}'".format(self.rs2))
        return self.rs2 << 12

    def encode_dst(self):
        if self.dst > 15 or self.dst < 0:
            self.print_error("Invalid register '{}'".format(self.dst))
        return self.dst << 4

    def encode_imm(self):
        if self.imm > 15 or self.imm < 0:
            self.print_error("Invalid imm '{}'".format(self.imm))
        return self.imm << 12

    def encode_simm(self):
        if self.imm > 15 or self.imm < 0:
            self.print_error("Invalid imm '{}'".format(self.imm))
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

class BRInstruction(Instruction):
    def __init__(self, op, rs1, rs2, label):
        self.op = op
        self.rs1 = rs1
        self.rs2 = rs2
        self.label = label

    def resolve_br_label(self, addr):
        global all_labels
        if not self.label in all_labels:
            self.print_error("Label {} not defined".format(self.label))

        label_def = all_labels[self.label]
        self.pc_off = label_def.addr - addr
        if abs(self.pc_off) > (2**3 << 2): # 3 bit word aligned
            self.print_error("Branch to label {} cannot fit into imm".format(self.label))

    def encode_pc_off(self):
        return (self.pc_off >> 2) << 4

    def encode(self):
        return self.encode_op() | self.encode_pc_off() | self.encode_rs1() | self.encode_rs2()


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

class LabelDef:
    def __init__(self, name, addr):
        self.name = name
        self.addr = addr

    def print(self):
        print("Label {} -> {}".format(self.name, self.addr))



def print_verbose(msg):
    global verbose
    if verbose:
        print(msg)

class Parser:
    def __init__(self, file_path):
        self.linenum = 0
        with open(file_path, 'r') as f:
            self.lines = f.readlines()

    def parse(self):
        for line in self.lines:
            self.linenum += 1
            self.parse_line(line)

    def print_error(self, msg):
        print("ERROR: " + msg + " at line {}".format(self.linenum))
        sys.exit(1)


    def parse_asm_directive(self, toks, tid, t):
        global current_addr
        if t[1:] == "org":
            addr_str = toks[tid+1]
            if addr_str.startswith("0x"):
                current_addr = int(addr_str, base=16)
            else:
                current_addr = int(addr_str)
            print_verbose("Setting PC to {}".format(hex(current_addr)))
            return 1
        else:
            self.print_error("Unknown asm directive")

    def parse_label(self, tok):
        if tok.startswith(':'):
            return tok[1:]

        self.print_error("Expected label, got '{}'".format(tok))


    def parse_reg(self, tok):
        if tok.startswith('%'):
            reg = int(tok[1:])
            if reg < 15:
                return reg
            self.print_error("Invalid register '{}'".format(reg))

        self.print_error("Expected register, got '{}'".format(tok))

    def parse_imm(self, tok):
        if tok.startswith('$'):
            imm = int(tok[1:])
            return imm
        self.print_error("Expected immediate, got '{}'".format(tok))

    def parse_instruction(self, toks, tid, t, insns):
        global rrr_insn, rri_insn, rr_insn, rrs_insn

        if t in rrr_insn:
            dst = self.parse_reg(toks[tid+1])
            rs1 = self.parse_reg(toks[tid+2])
            rs2 = self.parse_reg(toks[tid+3])
            print_verbose("{}: Found insn '{}' with args x{} x{} x{}".format(hex(current_addr), t, dst, rs1, rs2))
            insns.append(RRRInstruction(t, dst, rs1, rs2))
            return 4
        elif t in rri_insn:
            dst = self.parse_reg(toks[tid+1])
            rs1 = self.parse_reg(toks[tid+2])
            imm = self.parse_imm(toks[tid+3])
            insns.append(RRIInstruction(t, dst, rs1, imm))
            print_verbose("{}: Found insn '{}' with args x{} x{} #{}".format(hex(current_addr), t, dst, rs1, imm))
            return 4
        elif t in rrs_insn:
            source = self.parse_reg(toks[tid+1])
            base =   self.parse_reg(toks[tid+2])
            imm =    self.parse_imm(toks[tid+3])
            insns.append(RRSInstruction(t, source, base, imm))
            print_verbose("{}: Found insn '{}' with args x{} x{} #{}".format(hex(current_addr), t, source, base, imm))
            return 4
        elif t in rr_insn:
            dst   = self.parse_reg(toks[tid+1])
            label = self.parse_label(toks[tid+2])
            insns.append(RRInstruction(t, dst, label))
            print_verbose("Found insn '{}' with args x{} :{}".format(t, dst, label))
            return 3
        elif t in br_insn:
            rs1   = self.parse_reg(toks[tid+1])
            rs2   = self.parse_reg(toks[tid+2])
            label = self.parse_label(toks[tid+3])
            insns.append(BRInstruction(t, rs1, rs2, label))
            print_verbose("{}: Found insn '{}' with args x{} x{} :{}".format(hex(current_addr), t, rs1, rs2, label))
            return 4

        print_error("Invalid instruction '{}'".format(t))

    def parse_label_def(self, tid, t, toks):
        global current_addr
        label = t[:-1]
        print_verbose("Found jump-label '{}'".format(label))

        if tid+1 < len(toks): # are insn in the same line?
            label_pc = current_addr
        else:
            label_pc = current_addr + 4

        all_labels[label] = LabelDef(label, label_pc)

    def parse_line(self, line):
        global instructions
        global vliw_insn, current_addr

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
                skip_tokens = self.parse_instruction(toks, tid, t, insns)
            elif t.endswith(":"):
                self.parse_label_def(tid, t, toks)
            elif t.startswith("."):
                skip_tokens = self.parse_asm_directive(toks, tid, t)
            else:
                self.print_error("Unknown token '{}'".format(t))

        if len(insns) > 0: # we have a VLIW insn
            vliw_insn.append(VLIWInstruction(current_addr, insns))
            increase_pc()


def increase_pc():
    global current_addr
    current_addr = current_addr + 4


def resolve_branches():
    global vliw_insn
    for vinsn in vliw_insn:
        vinsn.resolve_br_labels()

def encode_insn():
    global vliw_insn
    res = []
    for vinsn in vliw_insn:
        res.append(vinsn.encode())
    return res

def main():
    global verbose
    parser = argparse.ArgumentParser(description='Pyke ASM')
    parser.add_argument('inputs', metavar='inputs', type=str, help='Input files to be assembled')
    parser.add_argument('-o', type=str, help='output file')
    parser.add_argument('-v', action='store_true', help='verbose printing')
    args = parser.parse_args()

    verbose = args.v
    asm_parser = Parser(args.inputs)
    asm_parser.parse()
    resolve_branches()
    asm = encode_insn()

    if (args.o is not None):
        with open(args.o, 'w') as f:
            for line in asm:
                f.write(line + "\n")
    else: #print to stdout
        for line in asm:
            print(line)
main()
