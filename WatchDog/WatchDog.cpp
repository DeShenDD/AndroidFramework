#include "WatchDog.h"
#include <iostream>

using namespace std;

static WatchDog getInstance()
{
    if (mWatchDog == nullptr) {
        mWatchDog = new WatchDog();
    }

    return mWatchDog;
}


WatchDog::WatchDog()
{
    mBeginCheck = 0;
    mComplete = 1;

    thread threadMain(WatchDogMainThread);
    threadMain.detach();
    thread threadSecond(checkAllMonitorObserve);
    threadSecond.detach();
}

WatchDog::~WatchDog();
{
}

WatchDog::void addMonitor(Monitor& mMonitor)
{
    mMonitorQueue.push(mMonitor);
}

WatchDog::void addHandler(Handler& mHandler)
{
}

WatchDog::int getMaxCostTime()
{
    int state = COMPLETED;
    for(int i=0; i<mMonitor.size; i++) {
        state = max(state, mMonitor[i].getState(mComplete, mStartTime));
    }

    return state;
}


WatchDog::void checkAllMonitorObserve()
{
    while(1) {
        if (!mBeginCheck) {
            continue;
        } else {
            mMoninorCount = mMonitor.size();

            for (int i=0; i<mMoninorCount; i++) {
                mCurrentMonitor = mMonitor[i];
                mCurrentMonitor.monitor();
            }

            mCurrentMonitor = nullptr;
            mComplete = 1;
        }
    }

}

WatchDog::void triggerAllMonitorObserve()
{
    int mMoninorCount;
    if (mComplete) {
        mMoninorCount = mMonitorQueue.size();
        for (int i=0; i<mMoninorCount; i++) {
            mMonitor.insert(mMonitorQueue[i]);
        }
    }

    
    if ((!mComplete)||(0==mMonitor.size())) {
        return;
    }

    mComplete = 0;
    mCurrentMonitor = nullptr;
    mStartTime = chrono::high_resolution_clock::now();
    mBeginCheck = 1;

    return;
}

WatchDog::void WatchDogMainThread()
{
    while(1) {
        checkAllMonitorObserve();
        sleep(30);
        int state = getMaxCostTime();
        switch (state)
        {
            case COMPLETE: cout<<"all normal!"<< state << endl; break;
            case WAITING: cout<<"continue waiting!"<< state << endl; break;
            case HALF_WAIT: cout<<"over half!"<< state << endl; break;
            case OVERTIME: cout<<"over time!"<< state << endl; break;
            default: cout<<"state error!"<< state << endl; brak;
        }

        mBeginCheck = 0;

        
    }
}

