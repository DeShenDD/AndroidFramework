CC := g++
INCLUDE := -Iinclude/
SRC := $(wildcard  *.cpp)
OBJS :=  $(patsubst %cpp, %o, $(SRC)) 
CFLAGS := -g -Wall  

TARGET:= main

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) -std=c++14 $(CFLAGS)  $^ -o $@

$(OBJS): %o:%cpp
	$(CC) -std=c++14 -c $(CFLAGS) $(INCLUDE) $< -o $@

