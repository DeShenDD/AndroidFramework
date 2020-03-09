#include <iostream>
#include <thread>
#include <unistd.h>
#include "include/WatchDog.h"

using namespace std;
using namespace chrono;

static WatchDog *mWatchDog;

WatchDog *getInstance()
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

    std::thread threadMain(WatchDogMainThread);
    threadMain.detach();
    std::thread threadSecond(checkAllMonitorObserve);
    threadSecond.detach();
}

WatchDog::~WatchDog()
{
}

void WatchDog::addMonitor(Monitor* Monitor)
{
    mMonitorQueue.push_back(Monitor);
}

void WatchDog::addHandler(Handler& Handler)
{
}

int WatchDog::getMaxCostTime(auto  &startTime)
{
    int state = COMPLETE;
    for(unsigned int i=0; i<mMonitor.size(); i++) {
        state = max(state, mMonitor[i]->getState(mComplete, startTime));
    }

    return state;
}


void WatchDog::checkAllMonitorObserve()
{
    int mMoninorCount;
    while(1) {
        if (!mBeginCheck) {
            continue;
        } else {
            mMoninorCount = mMonitor.size();

            Monitor *mCurrentMonitor;
            for (int i=0; i<mMoninorCount; i++) {
                mCurrentMonitor = mMonitor[i];
                mCurrentMonitor->checkMonitor();
            }

            //mCurrentMonitor = nullptr;
            mComplete = 1;
        }
    }

}

void WatchDog::triggerAllMonitorObserve()
{
    int mMoninorCount;
    if (mComplete) {
        mMoninorCount = mMonitorQueue.size();
        for (int i=0; i<mMoninorCount; i++) {
            mMonitor.push_back(mMonitorQueue[i]);
        }
    }

    
    if ((!mComplete)||(0==mMonitor.size())) {
        return;
    }

    mComplete = 0;
    //mCurrentMonitor = nullptr;
    //mStartTime = system_clock::now();
    mBeginCheck = 1;

    return;
}

void WatchDog::WatchDogMainThread()
{
    while(1) {
        auto mStartTime = system_clock::now();
        checkAllMonitorObserve();
        sleep(30);
        int state = (*mWatchDog).getMaxCostTime(mStartTime);
        switch (state)
        {
            case COMPLETE: cout<<"all normal!"<< state << endl; break;
            case WAITING: cout<<"continue waiting!"<< state << endl; break;
            case HALF_WAIT: cout<<"over half!"<< state << endl; break;
            case OVERTIME: cout<<"over time!"<< state << endl; break;
            default: cout<<"state error!"<< state << endl; break;
        }

        mBeginCheck = 0;

        
    }
}

