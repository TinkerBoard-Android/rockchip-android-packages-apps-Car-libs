#!/bin/bash

if grep -rq "findViewById\|requireViewById" car-ui-lib/src/com/android/car/ui/toolbar/; then
    grep -r "findViewById\|requireViewById" car-ui-lib/src/com/android/car/ui/toolbar/;
    echo "Illegal use of findViewById or requireViewById in car-ui-lib. Please consider using CarUiUtils#findViewByRefId or CarUiUtils#requireViewByRefId" && false;
fi

if grep -rq "findViewById\|requireViewById" car-ui-lib/src/com/android/car/ui/recyclerview/; then
    grep -r "findViewById\|requireViewById" car-ui-lib/src/com/android/car/ui/recyclerview/;
    echo "Illegal use of findViewById or requireViewById in car-ui-lib. Please consider using CarUiUtils#findViewByRefId or CarUiUtils#requireViewByRefId" && false;
fi
