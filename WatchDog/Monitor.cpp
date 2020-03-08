#include "Monitor.h"

using namespace std;

Monitor::int getState(int flag, long startTime)
{
    if(flag) {
        return COMPLETE;
    } else {
        long latency = chrono::high_resolution_clock::now() - startTime;
        if (latency < mWaitTime/2) {
            return WAITING;
        } else if (latency < mWaitTime) {
            return HALF_WAIT;
        }
    }

    return OVERTIME;
}

