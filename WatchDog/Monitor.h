#ifndef _MONITOR_H_
#define _MONITOR_H_


class Monitor{

public:
    Monitor();

    virtual ~Monitor();

private:
    int getState(int flag, long startTime);

    virtual void checkMonitor();

};


#endif
