#include <chrono>
#include "include/Monitor.h"

using namespace std;
using namespace chrono;

Monitor::Monitor()
{
}

Monitor::~Monitor()
{
}

int Monitor::getState(int flag)
{
    if(flag) {
        return COMPLETE;
    } else {
        long long endTime = time(NULL);
        long long latency = endTime - mStartTime;
        cout << latency <<endl;
        if (latency < mWaitTime/2) {
            return WAITING;
        } else if (latency < mWaitTime) {
            return HALF_WAIT;
        }
    }

    return OVERTIME;
}

