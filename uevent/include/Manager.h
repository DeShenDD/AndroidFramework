#ifndef _MANAGER_H_
#define _MANAGER_H_

#include "Uevent.h"
#include "ManagerInterface.h"

class Manager : public ManagerInterface {

private:
    int initConnection();
    Manager();
    ~Manager() override;
    std::string parse(std::string &str);
    int mSock;
    Uevent* mUevent;

public:
    void notify(std::string &str) override;
    static Manager* create();
    void unregister();
    void enregister();
};

#endif
