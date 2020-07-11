#ifndef _MANAGER_INTERFACE_H_
#define _MANAGER_INTERFACE_H_
#include <iostream>
#include <string>

class ManagerInterface {

public:
    ManagerInterface() {};
    virtual ~ManagerInterface() {};
    virtual void notify(char* str) = 0;

};

#endif
