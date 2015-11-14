
# define sources root directory before everything
SRC_ROOT := ../..

# change compiler and linker option before you include rule.mk
#
# link to libscript.a when you need to fetch configuration
# from test_script
#
#CFLAGS := $(CFLAGS) -g
LDFLAGS := -lscript -lpthread

# include rule.mk
include $(SRC_ROOT)/rule.mk

CFLAGS := $(CFLAGS) -I$(LIBDIR)/salsa-lib-0.0.22/src

# define objects collection variable
tvtester_objs = tvtester.o mixer.o pcm.o

# add your target(s) to all
.PHONY: all
all: tvtester

# define you target, the target will be output to dragonboard/output/bin
# directory
tvtester: $(tvtester_objs)
	$(LINK_MSG)
	$(LINKX)

# change 'example_objs' to your objects collection variable
$(tvtester_objs): %.o: %.c
	$(COMPILE_MSG)
	$(COMPILEX)

