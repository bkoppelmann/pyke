import sys

class Backend:
    def __init__(self, parser):
        self.parser = parser

        self.rrr_insn = ['add', 'sub', 'sll', 'srl', 'sra', 'xor', 'and', 'or']
        self.rri_insn = ['addi', 'slli', 'lw']
        self.rrs_insn = ['sw']
        self.br_insn = ['bne', 'beq']
        self.rr_insn  = ['jr']
        self.instructions = self.rrr_insn + self.rri_insn + self.rr_insn + self.rrs_insn + self.br_insn


    def parse_instruction(self, t, insns):
        if t in self.rrr_insn:
            dst = self.parser.parse_reg(self.parser.consume())
            rs1 = self.parser.parse_reg(self.parser.consume())
            rs2 = self.parser.parse_reg(self.parser.consume())
            insns.append(RRRInstruction(t, dst, rs1, rs2))

        elif t in self.rri_insn:
            dst = self.parser.parse_reg(self.parser.consume())
            rs1 = self.parser.parse_reg(self.parser.consume())
            imm = self.parser.parse_imm(self.parser.consume())
            insns.append(RRIInstruction(t, dst, rs1, imm))
        elif t in self.rrs_insn:
            source = self.parser.parse_reg(self.parser.consume())
            base =   self.parser.parse_reg(self.parser.consume())
            imm =    self.parser.parse_imm(self.parser.consume())
            insns.append(RRSInstruction(t, source, base, imm))
        elif t in self.rr_insn:
            dst   = self.parser.parse_reg(self.parser.consume())
            label = self.parser.parse_label(self.parser.consume())
            insns.append(RRInstruction(t, dst, label))

        elif t in self.br_insn:
            rs1   = self.parser.parse_reg(self.parser.consume())
            rs2   = self.parser.parse_reg(self.parser.consume())
            label = self.parser.parse_label(self.parser.consume())
            insns.append(BRInstruction(t, rs1, rs2, label))
        else:
            self.parser.print_error("Invalid instruction '{}'".format(t))

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

    def is_branch(self):
        return False



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
        self.pc_off = 0

    def is_branch(self):
        return True

    def resolve_br_label(self, all_labels, addr):
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

