#ifndef _MONITOR_H_
#define _MONITOR_H_
#include <iostream>
#include "state.h"

class Monitor{

public:
    Monitor();

    virtual ~Monitor();

    int getState(int flag, auto startTime);

    virtual void checkMonitor()=0;

private:

    long mWaitTime = 60;

};


#endif
