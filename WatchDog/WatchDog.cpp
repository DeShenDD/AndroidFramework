#include <iostream>
#include <thread>
#include <unistd.h>
#include "include/WatchDog.h"

using namespace std;
using namespace chrono;

mutex m_Mutex;
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
    std::cout<<"WatchDog Init Completely." <<std::endl;
}

WatchDog::~WatchDog()
{
    std::cout<<"release WatchDog object." <<std::endl;
}


void WatchDog::addMonitor(Monitor* Monitor)
{
    cout<<"Add Monitor: "<<Monitor<<endl;
    m_Mutex.lock();
    mMonitorQueue.push_back(Monitor);
    m_Mutex.unlock();
}


int WatchDog::getMaxCostTime()
{
    int state = COMPLETE;
    for(unsigned int i=0; i<mMonitor.size(); i++) {
        cout<<"The "<<i<<" object"<< &mMonitor[i] <<endl;
        state = max(state, mMonitor[i]->getState(mComplete));
    }

    return state;
}


void WatchDog::checkAllMonitorObserve()
{
    unsigned int mMoninorCount;
    cout<<"Begin to run checkAllMonitorObserve."<<endl;
    while(1) {
        if (!mBeginCheck || mComplete) {
            continue;
        } else {
            mMoninorCount = mMonitor.size();

            m_Mutex.lock();
            Monitor *mCurrentMonitor;
            for (unsigned int i=0; i<mMoninorCount; i++) {
                mCurrentMonitor = mMonitor[i];
                mCurrentMonitor->mStartTime = time(NULL);
                mCurrentMonitor->checkMonitor();
            }
            m_Mutex.unlock();

            mCurrentMonitor = nullptr;
            mComplete = 1;
        }
    }

}

void WatchDog::triggerAllMonitorObserve()
{
    unsigned int mMoninorCount;
    if (mComplete) {
        m_Mutex.lock();
        mMoninorCount = mMonitorQueue.size();
        cout << "Monitor Queue size" << mMoninorCount<<endl;
        for (unsigned int i=0; i<mMoninorCount; i++) {
            mMonitor.push_back(mMonitorQueue[i]);
            cout<<&mMonitorQueue[i]<<endl;
        }
        for (auto it=mMonitorQueue.begin(); it!=mMonitorQueue.end(); ) {
            mMonitorQueue.erase(it);
        }
        m_Mutex.unlock();
    }

    
    if ((!mComplete)||(0==mMonitor.size())) {
        cout<<"monitor size"<<mMonitor.size()<<endl;
        return;
    }

    mComplete = 0;
    mBeginCheck = 1;

    return;
}

void WatchDog::WatchDogMainThread()
{
    cout<<"Begin to run WatchDogMainThread."<<endl;
    while(1) {
        triggerAllMonitorObserve();
        sleep(30);
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

