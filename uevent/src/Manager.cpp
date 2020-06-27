#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include<sys/types.h>
#include<sys/un.h>
#include "Manager.h"

#define UDP_SOCKET  "./udp_socket"

using namespace std;

/*Manager::communication()
{
    while(1)
    {
        sendto();
    }
}*/

struct sockaddr_un addr;

int Manager::initConnection()
{
    mSock = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (mSock < 0)
    {
        cout<<"create socket failed."<<endl;
        return -1;
    }

    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, UDP_SOCKET, sizeof(UDP_SOCKET));
    return mSock;
}

Manager* Manager::create()
{
    Manager* m_Manager = new Manager();
    int ret = m_Manager->initConnection();
    if (ret < 0)
    {
        cout<<"init socket failed."<<endl;
        return nullptr;
    }
    return m_Manager;
}

Manager::Manager()
{
    mUevent = new Uevent();
    enregister();
}

Manager::~Manager()
{
    unregister();
    close(mSock);
    unlink(UDP_SOCKET);
}

void Manager::enregister()
{
    mUevent->AddObserver(this);
}

void Manager::unregister()
{
    mUevent->removeObserver(this);
}

void Manager::notify(string &str)
{
    string parseString;
    parseString = parse(str);
    cout<<"info "<<parseString<<"socket: "<<mSock<<endl;
    sendto(mSock, parseString.c_str(), strlen(parseString.c_str())+1,
            0, (struct sockaddr*)&addr, sizeof(struct sockaddr_un));
}

string  Manager::parse(string &str)
{
    return str;
}
