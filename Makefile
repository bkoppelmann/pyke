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

TOP_MODULE = PykeTop

compile: $(VERILOG_TOP)

$(VERILOG_TOP): $(wildcard $(SRC_DIR)/scala/*.scala)
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

.PHONY: emulator asm
emulator: $(EMULATOR)
asm: $(BASE_DIR)/tools/asm/be.py

$(BASE_DIR)/tools/asm/be.py: $(ISA_DIR)/$(ISA).decode $(ISA_DIR)/$(ISA).yml
	$(DECODETREE) --asm=$(ISA_DIR)/$(ISA).yml -o $@ $<


$(EMULATOR): $(VERILOG_TOP) $(TB)
	@mkdir -p $(SIM_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) -CFLAGS "-I$(SIM_DIR)" -Mdir $(SIM_DIR) -o $@ $^
	$(MAKE) -C $(SIM_DIR) -f V$(TOP_MODULE).mk

.PHONY: clean
clean:
	rm -rf $(GEN_DIR)/*
	rm -rf $(SIM_DIR)/*

include $(BASE_DIR)/sw/tests.mk
