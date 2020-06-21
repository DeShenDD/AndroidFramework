#ifndef _UEVENT_H_
#define _UEVENT_H_
#define MAXSIZE 2048
#include <iostream>

class uevent {

    uevent();
    ~uevent();
    getSocketFd();
    setSocketFd(const int socket);
    initSocketFd();
    AddObserver();
    removeObserver();
    notifyChanged();
    int mSock;

private:

    int buffsize = MAXSIZE;
    char buf[MAXSIZE];
    

};

#endif
