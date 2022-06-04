AS=python3 $(BASE_DIR)/tools/asm/asm.py

TEST_BUILD_DIR = $(SIM_DIR)/tests
TEST_DIR = $(BASE_DIR)/sw/asm/

TESTS = $(shell cd $(TEST_DIR) && find . -iname "*.s")
TEST_VCD = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.vcd, $(TESTS)))
TEST_HEX = $(addprefix $(TEST_BUILD_DIR)/,$(patsubst %.S, %.hex, $(TESTS)))

.PRECIOUS: $(TESTS_HEX)

.PHONY: tests clean_tests
tests: $(TEST_BUILD_DIR) $(TEST_VCD)

$(TEST_BUILD_DIR):
	@mkdir -p $(TEST_BUILD_DIR)

$(TEST_BUILD_DIR)/%.hex: $(TEST_DIR)/%.S
	$(AS) -o $@ $<

$(TEST_BUILD_DIR)/%.vcd:$(TEST_BUILD_DIR)/%.hex $(EMULATOR)
	@cd $(SIM_DIR) && $(EMULATOR) -i $< -v $(patsubst %.hex, %.vcd, $<)
