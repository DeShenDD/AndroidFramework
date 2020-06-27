#ifndef _UEVENT_H_
#define _UEVENT_H_
#define MAXSIZE 2048

#include <iostream>
#include <vector>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include "ManagerInterface.h"

class Uevent {
public:
    Uevent();
    ~Uevent();
    int getSocketFd();
    void setSocketFd(const int socket);
    int initSocketFd();
    void AddObserver(ManagerInterface* observer);
    void removeObserver(ManagerInterface* observer);
    void notifyChanged(char* str);
    void run();
    int mSock;

private:

    int buffsize = MAXSIZE;
    char buf[MAXSIZE];
    std::vector<ManagerInterface*> mObserver;

};

#endif
