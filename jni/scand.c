// from AOSP, frameworks/base/cmds/installd/installd.*
/*
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <ctype.h>
#include <fcntl.h>
#include <errno.h>
#include <utime.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
*/
#include "libscan.h"

#define LOG_TAG         "installd"
#define SOCKET_PATH     "scand"
#define BUFFER_MAX      1024

int main(const int argc, const char *argv[]) {
    
}

/*
int main(const int argc, const char *argv[]) {    
    char buf[BUFFER_MAX];
    struct sockaddr addr;
    socklen_t alen;
    int lsocket, s, count;
    
    lsocket = android_get_control_socket(SOCKET_PATH);
    if (lsocket < 0) {
        LOGE("Failed to get socket from environment: %s\n", strerror(errno));
        exit(1);
    }
    if (listen(lsocket, 5)) {
        LOGE("Listen on socket failed: %s\n", strerror(errno));
        exit(1);
    }
    fcntl(lsocket, F_SETFD, FD_CLOEXEC);
    
    for (;;) {
        alen = sizeof(addr);
        s = accept(lsocket, &addr, &alen);
        if (s < 0) {
                LOGE("Accept failed: %s\n", strerror(errno));
            continue;
        }
        fcntl(s, F_SETFD, FD_CLOEXEC);
        
        LOGI("new connection\n");
        for (;;) {
            unsigned short count;
            if (readx(s, &count, sizeof(count))) {
                LOGE("failed to read size\n");
                break;
            }
            if ((count < 1) || (count >= BUFFER_MAX)) {
                LOGE("invalid size %d\n", count);
                break;
            }
            if (readx(s, buf, count)) {
                LOGE("failed to read command\n");
                break;
            }
            buf[count] = 0;
            if (execute(s, buf)) break;
        }
        LOGI("closing connection\n");
        close(s);
    }
    
    return 0;
}
*/
