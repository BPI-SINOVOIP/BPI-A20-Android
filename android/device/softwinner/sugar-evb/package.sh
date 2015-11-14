#!/bin/bash
DEBUG="uart0"
SIGMODE="none"

if [ "$1" = "-d" -o "$2" = "-d" ]; then
	echo "----------debug version, have card0 printf-----------"
	DEBUG="card0";
else
	echo "----------realse version, have uart0 printf-----------"
fi


cd $PACKAGE
  ./pack -c sun7i -p android -b sugar-evb  -d ${DEBUG} -s ${SIGMODE}
cd -