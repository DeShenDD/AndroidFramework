CC := g++
INCLUDE := -Iinclude/ -Ilib/build/include
LIB := -L/home/jason/jason/Android/AndroidFramework/WatchDog/lib/build/lib  -pthread -lboost_chrono -lboost_system #-lboost_timer -lboost_atomic -lboost_iostreams -lboost_math_c99f -lboost_container -lboost_context -lboost_coroutine -lboost_date_time -lboost_filesystem -lboost_graph -lboost_locale -lboost_log_setup -lboost_log -lboost_math_c99l -lboost_math_c99 -lboost_math_tr1f -lboost_math_tr1l -lboost_math_tr1 -lboost_prg_exec_monitor -lboost_program_options -lboost_python -lboost_random -lboost_regex -lboost_serialization -lboost_signals -lboost_type_erasure -lboost_unit_test_framework -lboost_wave -lboost_wserialization -lboost_thread
SRC := $(wildcard  *.cpp)
OBJS :=  $(patsubst %cpp, %o, $(SRC)) 
CFLAGS := -g -Wall  

TARGET:= main

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) -std=c++14 $(CFLAGS) $(INCLUDE) $(LIB) $^ -o $@

$(OBJS): %o:%cpp
	$(CC) -std=c++14 -c $(CFLAGS) $(INCLUDE) $(LIB) $< -o $@

clean:
	rm *.o main
