#ifndef _WATCH_DOG_H
#define _WATCH_DOG_H

#include <iostream>
#include <vector>
#include "Monitor.h"
//#include "Handler.h"
#include "state.h"


class WatchDog {

public:

    static WatchDog* getInstance() {
        if (mWatchDog == nullptr) {
            mWatchDog = new WatchDog();
        }

        return mWatchDog;
    }

    void addMonitor(Monitor* mMonitor);

    //void addHandler(Handler& mHandler);
private:

    WatchDog();
    ~WatchDog() {}

    static WatchDog* mWatchDog;

    void WatchDogMainThread();

    int getMaxCostTime();

    void triggerAllMonitorObserve();

    std::vector<Monitor*> mMonitorQueue;

    //std::vector<Handler> mHandlerQueue;

    std::vector<Monitor*> mMonitor;

    int mComplete;

    int mBeginCheck;

    //auto mStartTime;

    //Monitor mCurrentMonitor;

    void checkAllMonitorObserve();
};

#endif
