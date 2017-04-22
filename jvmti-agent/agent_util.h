#ifndef AGENT_UTIL_H
#define AGENT_UTIL_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdarg.h>

#include <jni.h>
#include <jvmti.h>

#ifdef __cplusplus
extern "C" {
#endif

void fatal_error(const char * format, ...);
void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str);
void deallocate(jvmtiEnv *jvmti, void *ptr);

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif
