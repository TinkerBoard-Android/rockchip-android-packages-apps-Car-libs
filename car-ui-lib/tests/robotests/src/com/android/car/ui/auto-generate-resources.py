#!/usr/bin/env python

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

import operator
import os
import sys
from os import listdir
from os.path import isfile, join
from xml.dom import minidom
import xml.etree.ElementTree as ET

ROOT_FOLDER = 'packages/apps/Car/libs/car-ui-lib'

"""
Script used to update the 'current.xml' file. This is being used as part of pre-submits to
verify whether resources previously exposed to OEMs are being changed by a CL, potentially
breaking existing customizations.

Example usage: python auto-generate-resources.py current.xml
"""
def main():
    # Return the absolute path to the root dir.
    android_build_top = os.environ.get('ANDROID_BUILD_TOP')

    if not android_build_top:
        print("ANDROID_BUILD_TOP not defined: run envsetup.sh / lunch")
        sys.exit(1);

    path_to_color = join(android_build_top, ROOT_FOLDER + '/res/color/')
    file_color = [f for f in listdir(path_to_color) if isfile(join(path_to_color, f))]
    path_to_drawable = join(android_build_top, ROOT_FOLDER + '/res/drawable/')
    file_drawable = [f for f in listdir(path_to_drawable) if isfile(join(path_to_drawable, f))]
    path_to_values = join(android_build_top, ROOT_FOLDER + '/res/values/')
    file_values = [f for f in listdir(path_to_values) if isfile(join(path_to_values, f))]

    # Outermost tag for the generated xml file.
    data = ET.Element('resources')
    resource_mapping = {}

    for file in file_values:
        # Complete file path.
        file_path = join(path_to_values, file)
        read_xml(file_path, resource_mapping)

    for file in file_color:
        resource_mapping.update({file[:-4]: 'color'})

    for file in file_drawable:
        resource_mapping.update({file[:-4]: 'drawable'})

    create_resource(data, resource_mapping)
    write_xml(data)


def read_xml(file_path, resource_mapping):
    doc = minidom.parse(file_path)

    items = doc.getElementsByTagName('resources')

    for res in items[0].childNodes:
        # Exclude an node other than element node. such as text, comment etc.
        if res.nodeType != res.ELEMENT_NODE or res.tagName == 'declare-styleable':
            continue

        if res.tagName == 'item':
            resource_mapping.update({res.attributes['name'].value: res.attributes['type'].value})
        else:
            resource_mapping.update({res.attributes['name'].value: res.tagName})


def write_xml(data):
    xml_tag = "<?xml version='1.0' encoding='utf-8'?>"
    header = "<!-- This file is AUTO GENERATED, DO NOT EDIT MANUALLY. -->"
    data_string = ET.tostring(data)
    output_file_name =  sys.argv[1] if len(sys.argv) > 1 else "current.xml"
    output_file = open(output_file_name, "w")
    data_string = xml_tag + header + data_string
    output_file.write(data_string)


def create_resource(data, resource_mapping):
    sorted_resources = sorted(resource_mapping.items(),  key=lambda x: x[1]+x[0])

    for resource in sorted_resources:
        item = ET.SubElement(data, 'public')
        item.set('type', resource[1])
        item.set('name', resource[0])


if __name__ == '__main__':
    main()
