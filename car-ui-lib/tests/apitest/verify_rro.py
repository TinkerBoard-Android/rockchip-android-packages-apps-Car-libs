#!/usr/bin/python

# Copyright 2019, The Android Open Source Project
#
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use, copy,
# modify, merge, publish, distribute, sublicense, and/or sell copies
# of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

import argparse
import sys
from resource_utils import get_all_resources, remove_layout_resources, merge_resources
from git_utils import has_chassis_changes

def main():
    parser = argparse.ArgumentParser(description="Check that an rro does not attempt to overlay any resources that don't exist")
    parser.add_argument('--sha', help='Git hash of current changes. This script will not run if this is provided and there are no chassis changes.')
    parser.add_argument('-r', '--rro', action='append', nargs=1, help='res folder of an RRO')
    parser.add_argument('-b', '--base', action='append', nargs=1, help='res folder of what is being RROd')
    args = parser.parse_args()

    if not has_chassis_changes(args.sha):
        # Don't run because there were no chassis changes
        return

    if args.rro is None or args.base is None:
        parser.print_help()
        sys.exit(1)

    rro_resources = set()
    for resDir in args.rro:
        merge_resources(rro_resources, remove_layout_resources(get_all_resources(resDir[0])))

    base_resources = set()
    for resDir in args.base:
        merge_resources(base_resources, remove_layout_resources(get_all_resources(resDir[0])))

    extras = rro_resources.difference(base_resources)
    if len(extras) > 0:
        print("RRO attempting to override resources that don't exist:\n"
              + '\n'.join(map(lambda x: str(x), extras)))
        sys.exit(1)

if __name__ == "__main__":
    main()
