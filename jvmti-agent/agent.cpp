#include <cstring>

#include "agent_util.h"

#define MAX_THREAD_NAME_LEN 512

static jrawMonitorID lock;
static jboolean vm_is_dead;
static jvmtiEnv *globalJvmti;

static void
enter_critical_section(jvmtiEnv *jvmti)
{
  jvmtiError error;

  error = jvmti->RawMonitorEnter(lock);
  check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

static void
exit_critical_section(jvmtiEnv *jvmti)
{
  jvmtiError error;

  error = jvmti->RawMonitorExit(lock);
  check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}

void
get_thread_name(jvmtiEnv *jvmti, jthread thread, char *tname, int maxlen)
{
  jvmtiThreadInfo info;
  jvmtiError error;

  (void)memset(&info, 0, sizeof(info));

  (void)strcpy(tname, "Unknown");

  error = jvmti->GetThreadInfo(thread, &info);
  check_jvmti_error(jvmti, error, "Cannot get thread info");

  if (info.name != NULL) {
    int len;

    len = (int)strlen(info.name);
    if (len < maxlen) {
      (void)strcpy(tname, info.name);
    }
    deallocate(jvmti, (void *)info.name);
  }
}

void JNICALL
cbVMDeath(jvmtiEnv *jvmti, JNIEnv *env)
{
  enter_critical_section(jvmti); {
    vm_is_dead = JNI_TRUE;
  } exit_critical_section(jvmti);
}

void JNICALL
cbThreadStart(jvmtiEnv *jvmti, JNIEnv *env, jthread thread)
{
}

void JNICALL
cbThreadEnd(jvmtiEnv *jvmti, JNIEnv *env, jthread thread)
{
  jclass cls;
  jfieldID fid;
  jobject obj;
  jmethodID mid;

  enter_critical_section(jvmti); {
    if (vm_is_dead == JNI_FALSE) {
      char tname[MAX_THREAD_NAME_LEN];
      get_thread_name(jvmti, thread, tname, sizeof(tname));
      printf("ThreadEnd: %s\n", tname);

      char foo[MAX_THREAD_NAME_LEN];
      strncpy(foo, tname, 7);
      if (strcmp(foo, "Thread-") == 0) {
        printf("This thread was an application spawned thread\n");
      }

      /*
      cls = env->FindClass("org/jmock/integration/junit4/PerformanceMockery");
      fid = env->GetStaticFieldID(cls, "INSTANCE", "Lorg/jmock/integration/junit4/PerformanceMockery;");
      obj = env->GetStaticObjectField(cls, fid);

      mid = env->GetMethodID(cls, "jvmtiCallback", "()V");
      env->CallVoidMethod(obj, mid);
      */
    }
  } exit_critical_section(jvmti);
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
  jvmtiEnv *jvmti;
  jvmtiError error;
  jint res;
  jvmtiCapabilities capa;
  jvmtiEventCallbacks callbacks;

  res = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);
  if (res != JNI_OK) {
    fatal_error("ERROR: Unable to access JVMTI Version 1 (0x%x),"
            " is your JDK >= 5.0?"
            " JNIEnv's GetEnv() returned %d\n",
            JVMTI_VERSION_1_0, res);
  }

  (void)memset(&capa, 0, sizeof(capa));
  error = jvmti->AddCapabilities(&capa);
  check_jvmti_error(jvmti, error, "Unable to get necessary JVMTI capabilities");

  (void)memset(&callbacks, 0, sizeof(callbacks));
  callbacks.VMDeath = &cbVMDeath;
  callbacks.ThreadStart = &cbThreadStart;
  callbacks.ThreadEnd = &cbThreadEnd;
  error = jvmti->SetEventCallbacks(&callbacks, (jint)sizeof(callbacks));
  check_jvmti_error(jvmti, error, "Unable to set JVMTI callbacks");

  error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
      JVMTI_EVENT_THREAD_START, (jthread)NULL);
  check_jvmti_error(jvmti, error, "Cannot set event notification");
  error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
      JVMTI_EVENT_THREAD_END, (jthread)NULL);
  check_jvmti_error(jvmti, error, "Cannot set event notification");

  error = jvmti->CreateRawMonitor("agent data", &lock);
  check_jvmti_error(jvmti, error, "Cannot create raw monitor");

  vm_is_dead = JNI_FALSE;
  globalJvmti = jvmti;
  return JNI_OK;
}
