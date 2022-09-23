BASE_DIR   = $(abspath .)
SRC_DIR    = $(BASE_DIR)/src/main
GEN_DIR    = $(BASE_DIR)/generated-src
OUT_DIR    = $(BASE_DIR)/outputs
ISA_DIR    = $(SRC_DIR)/resources/isa

SBT       = sbt
SBT_FLAGS = -ivy $(BASE_DIR)/.ivy2
VERILATOR ?= verilator
VERILOG_TOP = $(GEN_DIR)/PykeTop.v

ISA ?= pyke32
CONFIG = $(ISA_DIR)/$(ISA).yml
DECODETREE = python3 $(BASE_DIR)/tools/decoder/decodetree.py
DECODETREE_FILE = $(ISA_DIR)/$(ISA).decode

GET_CONFIG_VAR = $(BASE_DIR)/scripts/get_config_var.py $(CONFIG)

TOP_MODULE = PykeTop

SCALA_SRCS = $(shell find $(SRC_DIR) -iname "*.scala")
GEN_SCALA_SRCS = $(SRC_DIR)/scala/pyke/Instructions.scala
SCALA_SRCS += $(GEN_SCALA_SRCS)
compile: $(VERILOG_TOP)

ASM_BE = $(BASE_DIR)/tools/asm/be.py

gen_hw: $(GEN_SCALA_SRCS)
$(GEN_SCALA_SRCS): $(DECODETREE_FILE)
	$(DECODETREE) --hw=$(CONFIG) -o $@ $<

$(VERILOG_TOP): $(SCALA_SRCS)
	$(SBT) $(SBT_FLAGS) "run $(GEN_DIR) $(CONFIG)"

# Testbench
SIM_DIR = $(BASE_DIR)/sim
EMULATOR = $(SIM_DIR)/top-sim
VCD_DUMP = $(SIM_DIR)/vlc_dump.vcd

TB = $(BASE_DIR)/src/main/resources/csrc/tb.cc

VERILATOR_FLAGS = --cc --exe --top-module $(TOP_MODULE) \
				  --trace --trace-max-array 8192 \
				  +define+RANDOMIZE_GARBAGE_ASSIGN \
				  -O3

.PHONY: emulator asm asm_clean
emulator: $(EMULATOR)
asm: $(ASM_BE)
clean_asm:
	rm -f $(ASM_BE)

$(ASM_BE): $(DECODETREE_FILE) $(ISA_DIR)/$(ISA).yml
	$(DECODETREE) --asm=$(ISA_DIR)/$(ISA).yml -o $@ $<


$(EMULATOR): $(VERILOG_TOP) $(TB)
	@mkdir -p $(SIM_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) -CFLAGS "-I$(SIM_DIR)" -Mdir $(SIM_DIR) -o $@ $^
	$(MAKE) -C $(SIM_DIR) -f V$(TOP_MODULE).mk -j

.PHONY: clean
clean: clean_asm
	rm -rf $(GEN_DIR)/*
	rm -rf $(SIM_DIR)/*

include $(BASE_DIR)/sw/tests.mk
