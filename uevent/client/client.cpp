#include <iostream>
#include <string>
#include <sys/types.h>
#include <unistd.h>
#include <sys/socket.h>
#include <string.h>
#include <sys/un.h>

using namespace std;

#define UDP_SOCKET "./udp_socket"

int main(int argc, char** argv)
{
    int buffsize = 2048;
    char buf[2048];
    int mSock = socket(AF_UNIX, SOCK_DGRAM, 0);
    struct sockaddr_un un;
    un.sun_family = AF_UNIX;
    strncpy(un.sun_path, UDP_SOCKET, strlen(UDP_SOCKET));
    int socket_len = sizeof(un);
    bind(mSock, (struct sockaddr*)&un,  sizeof(un));
    int ret = -1;
    while(1)
    {
        memset(buf, 0, sizeof(buf));
        cout<<"socket fd "<<mSock<<"waiting server"<<endl;
        ret = recvfrom(mSock, buf, buffsize, 0, (struct sockaddr *)&un, (socklen_t *)&socket_len);
        if (ret < 0)
        {
            cout<<"printf error."<<endl;
        } else 
        {
            cout<<"buf: "<<buf<<endl;
        }
    }

    close(mSock);
    unlink(UDP_SOCKET);
    return 0;
}
