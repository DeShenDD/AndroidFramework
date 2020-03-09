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

    static WatchDog getInstance();

    void addMonitor(Monitor* mMonitor);

    void addHandler(Handler& mHandler);

private:

    static void WatchDogMainThread();

    int getMaxCostTime(auto  &startTime);

    void triggerAllMonitorObserve();

    static std::vector<Monitor*> mMonitorQueue;

    static std::vector<Handler> mHandlerQueue;

    static std::vector<Monitor*> mMonitor;

    static int mComplete;

    static int mBeginCheck;

    //auto mStartTime;

    //Monitor mCurrentMonitor;

    static void checkAllMonitorObserve();
};


#endif
