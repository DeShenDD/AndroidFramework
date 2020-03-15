#include <iostream>
#include "include/MainThread.h"
#include "include/SecondThread.h"
using namespace std;

int main()
{
    MainThread *objectOne = new MainThread();
    SecondThread *objectTwo = new SecondThread();

    while(1)
    {
    }

    delete objectOne;
    delete objectTwo;

    return 0;
}
