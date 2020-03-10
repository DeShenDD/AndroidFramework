#include <iostream>
#include <thread>
#include <mutex>
#include <unistd.h>
#include "include/MainThread.h"
#include "include/WatchDog.h"

using namespace std;

static std::mutex mMutex;

void MainThread::MainTest()
{
    while(1)
    {
        sleep(5);
        cout << "MainThread" << endl;
        mMutex.lock();
        sleep(155);
        mMutex.unlock();
    }
}

MainThread::MainThread()
{
    thread test(&MainThread::MainTest, this);
    test.detach();
 
    WatchDog::getInstance()->addMonitor(this);
}


void MainThread::checkMonitor()
{
    mMutex.lock();
    cout<<"CheckMonitor"<<endl;
    mMutex.unlock();
    cout<<"CheckMonitorUnlock"<<endl;
}
