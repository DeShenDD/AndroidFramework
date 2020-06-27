#include <iostream>
#include <string.h>
#include <thread>
#include <mutex>
#include <algorithm>
#include "Uevent.h"

using namespace std;

mutex mMutex;

int Uevent::getSocketFd()
{
    return mSock;
}

void Uevent::setSocketFd(const int socket)
{
    mMutex.lock();
    mSock = socket;
    mMutex.unlock();
}

void Uevent::run()
{
    int charCount = -1;
    while(1)
    {
        memset(buf, 0, sizeof(buf));
        charCount = recv(mSock, buf, buffsize, 0);
        if(charCount>0) cout<<buf<<endl;
        else cout<<"buf is null "<<errno<<endl;
        notifyChanged(buf);
    }
}

int Uevent::initSocketFd()
{
    struct sockaddr_nl mAddr;
    mAddr.nl_family = AF_NETLINK;
    mAddr.nl_pid = getpid();
    mAddr.nl_groups = 0xffffffff;
    mSock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    setsockopt(mSock, SOL_SOCKET, SO_RCVBUF, &buffsize, sizeof(buffsize));
    bind(mSock, (struct sockaddr*)&mAddr, sizeof(mAddr));
    
    return mSock;
}

void Uevent::AddObserver(ManagerInterface* observer)
{
    auto it = find(mObserver.begin(), mObserver.end(), observer);
    if(it!=mObserver.end())
    {
        cout<<"had existed this observe."<<endl;
        return;
    }
    mMutex.lock();
    mObserver.push_back(observer);
    mMutex.unlock();
}

void Uevent::notifyChanged(char* str)
{
    if (!str) return;
    string notifyStr;
    int i = 0;
    while((*str)!='\0')
    {
        notifyStr[i] = *str;
        str++;
    }
    for(auto observer : mObserver)
    {
        cout<<"observer: "<<observer<<endl;
        observer->notify(notifyStr);
    }
}

void Uevent::removeObserver(ManagerInterface* observer)
{
    mMutex.lock();
    mObserver.erase(find(mObserver.begin(), mObserver.end(), observer));
    mMutex.unlock();
}


Uevent::Uevent():mSock(-1)
{
    initSocketFd();
    thread t1(&Uevent::run, this);
    t1.detach();
}

Uevent::~Uevent()
{
}
