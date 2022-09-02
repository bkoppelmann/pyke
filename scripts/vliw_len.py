#!/usr/bin/env python3
import yaml
import sys

p = sys.argv[1]

with open(p, "r") as file:
    try:
        res = yaml.safe_load(file)
        isa = res['isa']
        atomLen = int(isa['atomLen'])
        atomPerInsn = int(isa['atomPerInsn'])
        print(atomLen * atomPerInsn/8)
    except yaml.YAMLError as exc:
        print(-1)
        sys.exit(1)
