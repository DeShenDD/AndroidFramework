CC := g++
INCLUDE := -Iinclude/
SRC := $(wildcard  *.cpp)
OBJS :=  $(patsubst %cpp, %o, $(SRC)) 
CFLAGS := -std=c++14

TARGET:= main

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS)  $^ -o $@

%o:%cpp
	$(CC) -c $(CFLAGS) $(INCLUDE) $^ -o $@

