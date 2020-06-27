COMPILE := g++

CURPATH := $(shell pwd)

BUILDFILE := $(CURPATH)/build
$(info $(BUILDFILE))
ifneq ($(BUILDFILE), $(wildcard $(BUILDFILE)))
$(shell mkdir $(BUILDFILE))
endif

Src := $(wildcard src/*.cpp)
ClientSrc := $(wildcard client/*.cpp)
ClientObj := $(patsubst %cpp, %o, $(ClientSrc))
Obj := $(patsubst %cpp, %o, $(Src))

$(info $(ClientSrc))

LIBPATH := -I./include/

CFLAGS := -g -Wall -std=c++14 -pthread

TARGET := $(BUILDFILE)/Uevent
Client := $(BUILDFILE)/client

all: $(TARGET) $(Client)

$(TARGET): $(Obj)
	$(COMPILE) -o $(@) $(CFLAGS) $^

$(Obj): %.o:%.cpp
	$(COMPILE) -c $(CFLAGS) $(LIBPATH) $< -o  $@

$(Client): $(ClientObj)
	$(COMPILE) -o $(@) $(CFLAGS) $^
$(ClientSrc) : %.o:%.cpp
	$(COMPILE) -c $(CFLAGS)  $< -o $@ 

clean:
	rm -r $(BUILDFILE)
