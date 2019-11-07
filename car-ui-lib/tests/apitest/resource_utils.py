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

import os
import sys
import lxml.etree as etree

class ResourceLocation:
    def __init__(self, file, line=None):
        self.file = file
        self.line = line

    def __str__(self):
        if self.line is not None:
            return self.file + ':' + str(self.line)
        else:
            return self.file

class Resource:
    def __init__(self, name, type, location=None):
        self.name = name
        self.type = type
        self.locations = []
        if location is not None:
            self.locations.append(location)

    def __eq__(self, other):
        return self.name == other.name and self.type == other.type

    def __ne__(self, other):
        return self.name != other.name or self.type != other.type

    def __hash__(self):
        return hash((self.name, self.type))

    def __str__(self):
        result = ''
        for location in self.locations:
            result += str(location) + ': '
        result += '<'+self.type+' name="'+self.name+'"'

        return result + '>'

    def __repr__(self):
        return str(self)

def get_all_resources(resDir):
    allResDirs = [f for f in os.listdir(resDir) if os.path.isdir(os.path.join(resDir, f))]
    valuesDirs = [f for f in allResDirs if f.startswith('values')]
    fileDirs = [f for f in allResDirs if not f.startswith('values')]

    resources = set()

    # Get the filenames of the all the files in all the fileDirs
    for dir in fileDirs:
        type = dir.split('-')[0]
        for file in os.listdir(os.path.join(resDir, dir)):
            if file.endswith('.xml'):
                add_resource_to_set(resources,
                                    Resource(file[:-4], type,
                                             ResourceLocation(os.path.join(resDir, dir, file))))

    for dir in valuesDirs:
        for file in os.listdir(os.path.join(resDir, dir)):
            if file.endswith('.xml'):
                for resource in get_resources_from_single_file(os.path.join(resDir, dir, file)):
                    add_resource_to_set(resources, resource)

    return resources

def get_resources_from_single_file(filename):
    doc = etree.parse(filename)
    resourceTag = doc.getroot()
    result = set()
    for resource in resourceTag:
        if resource.tag == 'declare-styleable' or resource.tag is etree.Comment:
            continue

        if resource.tag == 'item' or resource.tag == 'public':
            add_resource_to_set(result, Resource(resource.get('name'), resource.get('type'),
                                                 ResourceLocation(filename, resource.sourceline)))
        else:
            add_resource_to_set(result, Resource(resource.get('name'), resource.tag,
                                                 ResourceLocation(filename, resource.sourceline)))
    return result

def remove_layout_resources(resourceSet):
    result = set()
    for resource in resourceSet:
        if resource.type != 'layout':
            result.add(resource)
    return result

# Used to get objects out of sets
class _Grab:
    def __init__(self, value):
        self.search_value = value
    def __hash__(self):
        return hash(self.search_value)
    def __eq__(self, other):
        if self.search_value == other:
            self.actual_value = other
            return True
        return False

def add_resource_to_set(resourceset, resource):
    grabber = _Grab(resource)
    if grabber in resourceset:
        grabber.actual_value.locations.extend(resource.locations)
    else:
        resourceset.update([resource])

if __name__ == '__main__':
    print(get_all_resources(sys.argv[1]))
