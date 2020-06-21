COMPILE := g++


Src := $(wildcard src/*.cpp)
Obj := $(patsubst %cpp, %o, $(Src))

CFLAGS := -g -Wall

TARGET:=uevent
all: $(TARGET)

$(TARGET):$(Obj)
	$(COMPILE) $(CFLAGS) $< -o $@
