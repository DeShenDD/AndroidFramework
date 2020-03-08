#include <iostream>
#include "MainThread.h"
#include "WatchDog.h"

using namespace std;

int main()
{
    MainThread *objectOne = new MainThread();

    while(1)
    {
    }

    delete objectOne;

    return 0;
}
