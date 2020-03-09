#include <chrono>
#include "include/Monitor.h"

using namespace std;

int Monitor::getState(int flag, auto startTime)
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
