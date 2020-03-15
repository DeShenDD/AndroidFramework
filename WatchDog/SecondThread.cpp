#include <iostream>
#include <thread>
#include <mutex>
#include <unistd.h>
#include "include/SecondThread.h"
#include "include/WatchDog.h"

using namespace std;

static std::mutex mMutex;

void SecondThread::MainTest()
{
    while(1)
    {
        sleep(5);
        cout << "SecondThread" << endl;
        mMutex.lock();
        sleep(10);
        mMutex.unlock();
        sleep(30);
    }
}

SecondThread::SecondThread()
{
    thread test(&SecondThread::MainTest, this);
    test.detach();
 
    WatchDog::getInstance()->addMonitor(this);
}


void SecondThread::checkMonitor()
{
    mMutex.lock();
    cout<<"CheckMonitor2"<<endl;
    mMutex.unlock();
    cout<<"CheckMonitorUnlock2"<<endl;
}
