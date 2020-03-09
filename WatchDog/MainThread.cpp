#include <iostream>
#include <thread>
#include <mutex>
#include <unistd.h>
#include "MainThread.h"

using namespace std;

static std::mutex mMutex;

void MainThread::MainTest()
{
    while(1)
    {
        sleep(5);
        cout << "MainThread" << endl;
    }
}

MainThread::MainThread()
{
    thread test(MainTest);
    test.detach();
}

MainThread::~MainThread()
{
}


void MainThread::checkMonitor()
{
    mMutex.lock();
    mMutex.unlock();
}
