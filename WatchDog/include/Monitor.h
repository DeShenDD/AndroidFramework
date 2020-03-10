#ifndef _MONITOR_H_
#define _MONITOR_H_
#include <iostream>
#include "state.h"
#include <time.h>
#include <sys/timeb.h>

class Monitor {

public:
    Monitor();

    virtual ~Monitor();

    int getState(int flag);

    virtual void checkMonitor() {}

    long long  mStartTime;

private:

    long long mWaitTime = 60;

};


#endif
