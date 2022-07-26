#! /usr/bin/env python3
import sys
import argparse
import be


verbose = False
vliw_insn = []

class VLIWInstruction:
    def __init__(self, addr, insns, be):
        self.insns = insns
        self.addr  = addr
        self.insn_len = be.atomLen

    def resolve_br_labels(self, all_labels):
        for insn in self.insns:
            if insn.has_label():
                insn.resolve_br_label(all_labels, self.addr)

    def encode(self):
        encoding = 0
        for num, insn in enumerate(self.insns):
            encoding = encoding | (insn.encode() << (num * self.insn_len))
        return str(hex(encoding))


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
        self.index = 0
        self.labels = {}
        self.current_addr = 0
        self.be = be.Backend(self)
        with open(file_path, 'r') as f:
            self.lines = f.readlines()

    def parse(self):
        for line in self.lines:
            self.linenum += 1
            self.parse_line(line)

    def print_error(self, msg):
        print("ERROR: " + msg + " at line {}".format(self.linenum))
        sys.exit(1)


    def parse_asm_directive(self, t):
        if t[1:] == "org":
            addr_str = self.consume()
            if addr_str.startswith("0x"):
                self.current_addr = int(addr_str, base=16)
            else:
                self.current_addr = int(addr_str)
            print_verbose("Setting PC to {}".format(hex(self.current_addr)))
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


    def parse_label_def(self, t):
        label = t[:-1]
        print_verbose("Found jump-label '{}'".format(label))

        if self.index+1 < len(self.toks): # are insn in the same line?
            label_pc = self.current_addr
        else:
            label_pc = self.current_addr + 4

        self.labels[label] = LabelDef(label, label_pc)

    def eol(self):
        return self.index >= len(self.toks)

    def peek(self):
        return self.toks[self.index]

    def consume(self):
        res = self.peek()
        self.index += 1
        return res

    def consume_specific(self, char):
        if self.peek() != char:
            self.print_error("Expected token '{}'".format(char))
        return self.consume()

    def parse_line(self, line):
        global vliw_insn

        line = line.strip()
        line = line.expandtabs()

        end = line.find('#')
        if end >= 0:
            line = line[:end]

        self.toks = line.split()
        self.index = 0

        insns = []

        while not self.eol():
            t = self.consume()
            if t in self.be.instructions:
                self.be.parse_instruction(t, insns)
                if not self.eol():
                    self.consume_specific('|')
            elif t.endswith(":"):
                self.parse_label_def(t)
            elif t.startswith("."):
                skip_tokens = self.parse_asm_directive(t)
            else:
                self.print_error("Unknown token '{}'".format(t))

        if len(insns) > 0: # we have a VLIW insn
            vliw_insn.append(VLIWInstruction(self.current_addr, insns, self.be))
            self.current_addr = self.current_addr + 4


def resolve_branches(all_labels):
    global vliw_insn
    for vinsn in vliw_insn:
        vinsn.resolve_br_labels(all_labels)

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
    resolve_branches(asm_parser.labels)
    asm = encode_insn()

    if (args.o is not None):
        with open(args.o, 'w') as f:
            for line in asm:
                f.write(line + "\n")
    else: #print to stdout
        for line in asm:
            print(line)
main()
