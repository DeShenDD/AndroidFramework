#include <iostream>
#include <string>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <thread>
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
    while(1)
    {
        memset(buf, 0, sizeof(buf));
        charCount = recv(mSock, buf, buffSize, 0);
        if(charCount>0) cout<<buf<<endl;
        else cout<<"buf is null "<<errno<<endl;
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

Uevent::AddObserver()
{
}

Uevent::notifyChanged()
{
}

Uevent::removeObserver()
{
}


Uevent::Uevent():mSock(-1)
{
    
}

Uevent::~Uevent()
{
}
