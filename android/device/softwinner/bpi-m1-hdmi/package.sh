#!/bin/bash
DEBUG="uart0"
SIGMODE="none"
VERSION="4.2.2"

if [ "$1" = "-d" -o "$2" = "-d" ]; then
	echo "----------debug version, have card0 printf-----------"
	DEBUG="card0";
else
	echo "----------realse version, have uart0 printf-----------"
fi


cd $PACKAGE
  ./pack -c sun7i -p android -v ${VERSION} -b bpi-m1-hdmi  -d ${DEBUG} -s ${SIGMODE}
cd -
