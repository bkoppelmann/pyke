AS=python3 $(BASE_DIR)/tools/asm/asm.py

TEST_BUILD_DIR = $(SIM_DIR)/tests
TEST_DIR = $(BASE_DIR)/sw/asm/

VLIW_LEN = $(shell $(BASE_DIR)/scripts/vliw_len.py $(CONFIG))

TESTS = $(shell cd $(TEST_DIR) && find . -iname "*.s")
TEST_VCD = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.vcd, $(TESTS)))
TEST_HEX = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.hex, $(TESTS)))

.PRECIOUS: $(TEST_BUILD_DIR)/%.hex $(TEST_BUILD_DIR)/%.s

.PHONY: tests clean_tests
tests: $(TEST_BUILD_DIR) $(TEST_VCD)

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
