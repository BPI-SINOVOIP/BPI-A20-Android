
# define sources root directory before everything
SRC_ROOT := ..

# include rule.mk
include $(SRC_ROOT)/rule.mk

# add you test case here
.PHONY: all
all:
	cp script_parser.sh $(BINDIR)/
	cp send_cmd_pipe.sh $(BINDIR)/

#please add test follow the character as 1,2,3,4... a,b,c,d,e,f,g...
	make -C 2gtester
	make -C bluetooth
	make -C ethtester
	make -C emmctester
	make -C gsensortester
	make -C hdmitester
	make -C host1tester
	make -C host2tester
	make -C irtester
	make -C keytester
	make -C ledtester
	make -C memtester
	make -C mictester
	make -C mmctester
	make -C nandtester
	make -C otgtester
	make -C rtctester
	make -C satatester
	make -C spdiftester
	make -C tvtester
	make -C udisktester
	make -C wifitester
