#include <iostream>
#include <thread>
#include <unistd.h>
#include "include/WatchDog.h"

using namespace std;
using namespace chrono;

WatchDog* WatchDog::mWatchDog = nullptr;

/*static WatchDog *mWatchDog;

static WatchDog *getInstance()
{
    if (mWatchDog == nullptr) {
        mWatchDog = new WatchDog();
    }

    return mWatchDog;
}*/


WatchDog::WatchDog()
{
    mBeginCheck = 0;
    mComplete = 1;

    std::thread threadMain(&WatchDog::WatchDogMainThread, this);
    threadMain.detach();
    std::thread threadSecond(&WatchDog::checkAllMonitorObserve, this);
    threadSecond.detach();
}


void WatchDog::addMonitor(Monitor* Monitor)
{
    mMonitorQueue.push_back(Monitor);
}


int WatchDog::getMaxCostTime()
{
    int state = COMPLETE;
    for(unsigned int i=0; i<mMonitor.size()-1; i++) {
        state = max(state, mMonitor[i]->getState(mComplete));
    }

    return state;
}


void WatchDog::checkAllMonitorObserve()
{
    int mMoninorCount;
    cout<<"Begin to run checkAllMonitorObserve."<<endl;
    while(1) {
        if (!mBeginCheck || mComplete) {
            continue;
        } else {
            mMoninorCount = mMonitor.size();

            Monitor *mCurrentMonitor;
            for (int i=0; i<mMoninorCount; i++) {
                mCurrentMonitor = mMonitor[i];
                mCurrentMonitor->mStartTime = time(NULL);
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
            cout<<&mMonitorQueue[i]<<endl;
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
    cout<<"Begin to run WatchDogMainThread."<<endl;
    while(1) {
        //long mStartTime = 0;//(long)system_clock::now();
        triggerAllMonitorObserve();
        sleep(10);
        int state = getMaxCostTime();
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

