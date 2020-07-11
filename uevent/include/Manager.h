#ifndef _MANAGER_H_
#define _MANAGER_H_

#include "Uevent.h"
#include "ManagerInterface.h"
#include "Parse.h"

class Manager : public ManagerInterface {

private:
    int initConnection();
    Manager();
    ~Manager() override;
    struct ueventInfo* parse(char *str);
    int mSock;
    Uevent* mUevent;
    Parse* mParse;

public:
    void notify(char* str) override;
    static Manager* create();
    void unregister();
    void enregister();
};

#endif
