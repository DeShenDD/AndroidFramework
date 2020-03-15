#include "Monitor.h"

class SecondThread : public Monitor {

public:

    SecondThread();

    ~SecondThread() {};

private:

    virtual  void checkMonitor();

    void MainTest();
};
