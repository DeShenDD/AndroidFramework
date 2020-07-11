#ifndef _PARSE_H_
#define _PARSE_H_
//namespace uevent
//{

struct ueventInfo {
    const char *action;
    const char *path;
    const char *subsystem;
    const char *firmware;
    int major;
    int minor;
};

class Parse {

public:
    Parse();
    ~Parse();

    struct ueventInfo* parseInfo(char* msg, struct ueventInfo *luther_gliethttp);
};
//}

#endif
