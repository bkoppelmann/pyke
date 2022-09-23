#!/usr/bin/env python3
import yaml
import sys

if len(sys.argv) != 3:
    print("Usage get_config_var config.yml category.key")
    sys.exit(1)

p = sys.argv[1]
key = sys.argv[2]

key_path = key.split(".")

with open(p, "r") as file:
    try:
        res = yaml.safe_load(file)

        current = res
        for k in key_path:
           current = current[k]

        print(current)
    except yaml.YAMLError as exc:
        print(exc)
        sys.exit(1)
