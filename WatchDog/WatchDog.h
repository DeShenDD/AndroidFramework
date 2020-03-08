#ifndef WATCH_DOG_H
#define WATCH_DOG_H

#include <iostream>
#include <vector>
#include "Monitor.h"
#include "Handler.h"
#include "state.h"


class WatchDog {

public:

    WatchDog();
    ~WatchDog();

    static WatchDog mWatchDog;

    static WatchDog getInstance();

    void addMonitor(Monitor& mMonitor);

    void addHandler(Handler& mHandler);

private:

    void WatchDogMainThread();

    int getMaxCostTime();

    void triggerAllMonitorObserve();

    vector<Monitor> mMonitorQueue;

    vector<Handler> mHandlerQueue;

    vector<Monitor> mMonitor;

    int mComplete;

    int mBeginCheck;

    long mStartTime;

    Monitor mCurrentMonitor;

    void checkAllMonitorObserve();
};


#endif
