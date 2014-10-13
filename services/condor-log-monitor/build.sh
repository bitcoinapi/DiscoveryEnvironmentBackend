#!/bin/sh

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

mkdir -p build/usr/local/bin
mkdir -p build/var/log/condor-log-monitor
godep restore
go build .
cp condor-log-monitor build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/condor-log-monitor --version $VERSION --epoch 0 --prefix / --name condor-log-monitor --verbose -C build -f .