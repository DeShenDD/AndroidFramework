#include "MainThread.h"
#include <thread>
#include <mutex>

using namespace std;

static mutex mMutex;

MainThread::void ()
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


MainThread::void monitor()
{
    mMutex.lock();
    mMutex.unlock();
}
