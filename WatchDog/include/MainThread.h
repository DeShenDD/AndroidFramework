#include "Monitor.h"

class MainThread : public Monitor {

public:

    MainThread();

    ~MainThread() {};

private:

    virtual  void checkMonitor();

    void MainTest();
};
