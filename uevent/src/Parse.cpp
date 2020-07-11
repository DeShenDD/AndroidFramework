#include <iostream>
#include <stdlib.h>
#include <string.h>
#include "Parse.h"

using namespace std;


Parse::Parse() {}

Parse::~Parse() {}


struct ueventInfo* Parse::parseInfo(char* msg, struct ueventInfo *luther_gliethttp)
{
    if(msg) return NULL;

    if (!strncmp(msg, "ACTION=", 7)) {
        msg += 7;
        luther_gliethttp->action = msg;
    } else if (!strncmp(msg, "DEVPATH=", 8)) {
        msg += 8;
        luther_gliethttp->path = msg;
    } else if (!strncmp(msg, "SUBSYSTEM=", 10)) {
        msg += 10;
        luther_gliethttp->subsystem = msg;
    } else if (!strncmp(msg, "FIRMWARE=", 9)) {
        msg += 9;
        luther_gliethttp->firmware = msg;
    } else if (!strncmp(msg, "MAJOR=", 6)) {
        msg += 6;
        luther_gliethttp->major = atoi(msg);
    } else if (!strncmp(msg, "MINOR=", 6)) {
        msg += 6;
        luther_gliethttp->minor = atoi(msg);
    }

    return luther_gliethttp;
}
