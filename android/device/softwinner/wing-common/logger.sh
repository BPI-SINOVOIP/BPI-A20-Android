#!/system/bin/sh

FILE_PATH=/data/local/logs

FILE_PREFIX_KERNLOG=kernlog
FILE_PREFIX_MAINLOG=mainlog
FILE_PREFIX_RADIOLOG=radiolog

/system/bin/log -t logging "Starting $1 logging"
#/system/bin/log -t logging "Sleeping 25 senconds for vold ready"

#sleep 25

if [ ! -d $FILE_PATH ]
then
/system/bin/log -t logging "not found path $FILE_PATH"
mkdir -p $FILE_PATH
fi

rename_logfile()
{
	FILE_PREFIX=$1
	LAST_LAST_LAST_FILE=`busybox find $FILE_PATH/ -name "$FILE_PREFIX-*.log.last.last"`
	LAST_LAST_FILE=`busybox find $FILE_PATH/ -name "$FILE_PREFIX-*.log.last"`
	LAST_FILE=`busybox find $FILE_PATH/ -name "$FILE_PREFIX-*.log"`
	#change log files name
	/system/bin/log -t logging "$LAST_LAST_LAST_FILE $LAST_LAST_FILE $LAST_FILE"
	rm $LAST_LAST_LAST_FILE
	mv $LAST_LAST_FILE $LAST_LAST_FILE.last
	mv $LAST_FILE $LAST_FILE.last
}

logger_kmsg()
{
	/system/bin/log -t logging "logger $FILE_PREFIX_KERNLOG ..."
	#rename_logfile $FILE_PREFIX_KERNLOG
	busybox cat /proc/kmsg > $FILE_PATH/$FILE_PREFIX_KERNLOG-`date +%Y%m%d-%H%M`.log 
}

logger_logcat()
{
	/system/bin/log -t logging "logger $FILE_PREFIX_MAINLOG ..."
	#rename_logfile $FILE_PREFIX_MAINLOG
	logcat -v time -f $FILE_PATH/$FILE_PREFIX_MAINLOG-`date +%Y%m%d-%H%M`.log 
}

logger_radio()
{
	/system/bin/log -t logging "logger $FILE_PREFIX_RADIOLOG ..."
	#rename_logfile $FILE_PREFIX_RADIOLOG
	logcat -v time -b radio -f $FILE_PATH/$FILE_PREFIX_RADIOLOG-`date +%Y%m%d-%H%M`.log 
}

case "$1" in
kernel)
	logger_kmsg;;
android)
	logger_logcat;;
radio)
	logger_radio;;
*)
	print "error argument, only for kernel, android, radio\n"
	;;
esac

