base_dir   = $(abspath .)
src_dir    = $(base_dir)/src/main
gen_dir    = $(base_dir)/generated-src
out_dir    = $(base_dir)/outputs

SBT       = sbt
SBT_FLAGS = -ivy $(base_dir)/.ivy2

compile: $(gen_dir)/Pyke.v

$(gen_dir)/Pyke.v: $(wildcard $(src_dir)/scala/*.scala)
	$(SBT) $(SBT_FLAGS) "run $(gen_dir)"

clean:
	rm -rf $(gen_dir)/*
