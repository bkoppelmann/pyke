AS=python3 $(BASE_DIR)/tools/asm/asm.py

TEST_BUILD_DIR = $(SIM_DIR)/tests/$(ATOM_PER_INSN)Lane
TEST_DIR = $(BASE_DIR)/sw/asm/$(ATOM_PER_INSN)Lane

ATOM_LEN = $(shell $(GET_CONFIG_VAR) isa.atomLen)
ATOM_PER_INSN =	$(shell $(GET_CONFIG_VAR) isa.atomPerInsn)
VLIW_LEN = $$(($(ATOM_LEN) * $(ATOM_PER_INSN) / 8))

TESTS = $(shell cd $(TEST_DIR) && find . -iname "*.s")
TEST_VCD = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.vcd, $(TESTS)))
TEST_HEX = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.hex, $(TESTS)))

.PRECIOUS: $(TEST_BUILD_DIR)/%.hex $(TEST_BUILD_DIR)/%.s

.PHONY: tests clean_tests
tests: $(TEST_BUILD_DIR) $(TEST_VCD) $(ASM_BE)

$(TEST_BUILD_DIR):
	@mkdir -p $(TEST_BUILD_DIR)

$(TEST_BUILD_DIR)/%.s: $(TEST_DIR)/%.S
	@$(CC) -E -I$(TEST_DIR) -o $@ $<
	@sed -i s/_NL/\\n/g $@

$(TEST_BUILD_DIR)/%.hex: $(TEST_BUILD_DIR)/%.s
	$(AS) -o $@ $<

$(TEST_BUILD_DIR)/%.vcd:$(TEST_BUILD_DIR)/%.hex $(EMULATOR)
	cd $(SIM_DIR) && $(EMULATOR) -l $(VLIW_LEN) -i $< -v $(patsubst %.hex, %.vcd, $<)

clean_tests:
	rm -f $(TEST_BUILD_DIR)/*
