#include "Monitor.h"

class MainThread : Monitor {

public:

    MainThread();

    ~MainThread();

private:

    virtual  void checkMonitor();

    static void MainTest();
};
